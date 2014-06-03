/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server;

import com.codenvy.api.core.util.Pair;
import com.codenvy.api.vfs.server.exceptions.HtmlErrorFormatter;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
import com.codenvy.api.vfs.server.exceptions.ItemAlreadyExistException;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.NotSupportedException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.QueryExpression;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.server.util.LinksHelper;
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.File;
import com.codenvy.api.vfs.shared.dto.Folder;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.ItemList;
import com.codenvy.api.vfs.shared.dto.ItemNode;
import com.codenvy.api.vfs.shared.dto.Lock;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.ACLCapability;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.dto.server.DtoFactory;

import org.apache.commons.fileupload.FileItem;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Base implementation of VirtualFileSystem.
 *
 * @author andrew00x
 */
public abstract class VirtualFileSystemImpl implements VirtualFileSystem {
    protected final String                       vfsId;
    protected final URI                          baseUri;
    protected final VirtualFileSystemUserContext userContext;
    protected final MountPoint                   mountPoint;
    protected final SearcherProvider             searcherProvider;

    public VirtualFileSystemImpl(String vfsId,
                                 URI baseUri,
                                 VirtualFileSystemUserContext userContext,
                                 MountPoint mountPoint,
                                 SearcherProvider searcherProvider) {
        this.vfsId = vfsId;
        this.baseUri = baseUri;
        this.userContext = userContext;
        this.mountPoint = mountPoint;
        this.searcherProvider = searcherProvider;
    }

    @Override
    public MountPoint getMountPoint() {
        return mountPoint;
    }

    @Path("copy/{id}")
    @Override
    public Item copy(@PathParam("id") String id, @QueryParam("parentId") String parentId) throws VirtualFileSystemException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create copy item. Item specified as parent is not a folder. ");
        }
        final VirtualFile virtualFileCopy = mountPoint.getVirtualFileById(id).copyTo(parent);
        return fromVirtualFile(virtualFileCopy, false, PropertyFilter.ALL_FILTER);
    }

    @Path("file/{parentId}")
    @Override
    public File createFile(@PathParam("parentId") String parentId,
                           @QueryParam("name") String name,
                           @DefaultValue(MediaType.APPLICATION_OCTET_STREAM) @HeaderParam("Content-Type") MediaType mediaType,
                           InputStream content) throws VirtualFileSystemException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create new file. Item specified as parent is not a folder. ");
        }
        final VirtualFile newVirtualFile = parent.createFile(name, mediaType != null ? mediaType.toString() : null, content);
        return (File)fromVirtualFile(newVirtualFile, false, PropertyFilter.ALL_FILTER);
    }

    @Path("folder/{parentId}")
    @Override
    public Folder createFolder(@PathParam("parentId") String parentId, @QueryParam("name") String name) throws VirtualFileSystemException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create new folder. Item specified as parent is not a folder. ");
        }
        final VirtualFile newVirtualFile = parent.createFolder(name);
        return (Folder)fromVirtualFile(newVirtualFile, false, PropertyFilter.ALL_FILTER);
    }

    @Path("delete/{id}")
    @Override
    public void delete(@PathParam("id") String id, @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (virtualFile.isRoot()) {
            throw new InvalidArgumentException("Unable delete root folder. ");
        }
        virtualFile.delete(lockToken);
    }

    @Path("acl/{id}")
    @Override
    public List<AccessControlEntry> getACL(@PathParam("id") String id) throws VirtualFileSystemException {
        if (getInfo().getAclCapability() == ACLCapability.NONE) {
            throw new NotSupportedException("ACL feature is not supported. ");
        }
        return mountPoint.getVirtualFileById(id).getACL();
    }

    @Path("children/{id}")
    @Override
    public ItemList getChildren(@PathParam("id") String folderId,
                                @DefaultValue("-1") @QueryParam("maxItems") int maxItems,
                                @QueryParam("skipCount") int skipCount,
                                @QueryParam("itemType") String itemType,
                                @DefaultValue("false") @QueryParam("includePermissions") Boolean includePermissions,
                                @DefaultValue(PropertyFilter.NONE) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        if (skipCount < 0) {
            throw new InvalidArgumentException("'skipCount' parameter is negative. ");
        }

        final ItemType itemTypeType;
        if (itemType != null) {
            try {
                itemTypeType = ItemType.fromValue(itemType);
            } catch (IllegalArgumentException e) {
                throw new InvalidArgumentException(String.format("Unknown type: %s", itemType));
            }
        } else {
            itemTypeType = null;
        }

        final VirtualFile virtualFile = mountPoint.getVirtualFileById(folderId);

        if (!virtualFile.isFolder()) {
            throw new InvalidArgumentException(String.format("Unable get children. Item '%s' is not a folder. ", virtualFile.getPath()));
        }

        final VirtualFileFilter filter;
        if (itemTypeType == null) {
            filter = VirtualFileFilter.ALL;
        } else {
            filter = new VirtualFileFilter() {
                @Override
                public boolean accept(VirtualFile file) throws VirtualFileSystemException {
                    return (itemTypeType == ItemType.FILE && file.isFile())
                           || (itemTypeType == ItemType.FOLDER && file.isFolder());
                }
            };
        }
        final LazyIterator<VirtualFile> children = virtualFile.getChildren(filter);
        try {
            if (skipCount > 0) {
                children.skip(skipCount);
            }
        } catch (NoSuchElementException nse) {
            throw new InvalidArgumentException("'skipCount' parameter is greater then total number of items. ");
        }

        final List<Item> items = new ArrayList<>();
        for (int count = 0; children.hasNext() && (maxItems < 0 || count < maxItems); count++) {
            items.add(fromVirtualFile(children.next(), includePermissions, propertyFilter));
        }
        return DtoFactory.getInstance().createDto(ItemList.class).withItems(items).withNumItems(children.size())
                         .withHasMoreItems(children.hasNext());
    }

    @Override
    public ItemList getChildren(String folderId, int maxItems, int skipCount, String itemType, boolean includePermissions)
            throws VirtualFileSystemException {
        return getChildren(folderId, maxItems, skipCount, itemType, includePermissions, PropertyFilter.ALL_FILTER);
    }

    @Path("tree/{id}")
    @Override
    public ItemNode getTree(@PathParam("id") String folderId,
                            @DefaultValue("-1") @QueryParam("depth") int depth,
                            @DefaultValue("false") @QueryParam("includePermissions") Boolean includePermissions,
                            @DefaultValue(PropertyFilter.NONE) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(folderId);
        if (!virtualFile.isFolder()) {
            throw new InvalidArgumentException(String.format("Unable get tree. Item '%s' is not a folder. ", virtualFile.getPath()));
        }
        return DtoFactory.getInstance().createDto(ItemNode.class)
                         .withItem(fromVirtualFile(virtualFile, includePermissions, propertyFilter))
                         .withChildren(getTreeLevel(virtualFile, depth, includePermissions, propertyFilter));
    }

    @Override
    public ItemNode getTree(String folderId, int depth, boolean includePermissions) throws VirtualFileSystemException {
        return getTree(folderId, depth, includePermissions, PropertyFilter.ALL_FILTER);
    }

    private List<ItemNode> getTreeLevel(VirtualFile virtualFile, int depth, boolean includePermissions, PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        if (depth == 0 || !virtualFile.isFolder()) {
            return null;
        }
        final LazyIterator<VirtualFile> children = virtualFile.getChildren(VirtualFileFilter.ALL);
        final List<ItemNode> level = new ArrayList<>(children.size());
        while (children.hasNext()) {
            final VirtualFile next = children.next();
            level.add(DtoFactory.getInstance().createDto(ItemNode.class)
                                .withItem(fromVirtualFile(next, includePermissions, propertyFilter))
                                .withChildren(getTreeLevel(next, depth - 1, includePermissions, propertyFilter)));
        }
        return level;
    }

    @Path("content/{id}")
    @Override
    public ContentStream getContent(@PathParam("id") String id) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (!virtualFile.isFile()) {
            throw new InvalidArgumentException(String.format("Unable get content. Item '%s' is not a file. ", virtualFile.getPath()));
        }
        return virtualFile.getContent();
    }

    @Path("contentbypath/{path:.*}")
    @Override
    public ContentStream getContent(@PathParam("path") String path, @QueryParam("versionId") String versionId)
            throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFile(path);
        if (!virtualFile.isFile()) {
            throw new InvalidArgumentException(String.format("Unable get content. Item '%s' is not a file. ", path));
        }
        return virtualFile.getContent();
    }

    @Override
    public abstract VirtualFileSystemInfo getInfo() throws VirtualFileSystemException;

    @Path("item/{id}")
    @Override
    public Item getItem(@PathParam("id") String id,
                        @DefaultValue("false") @QueryParam("includePermissions") Boolean includePermissions,
                        @DefaultValue(PropertyFilter.ALL) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        return fromVirtualFile(mountPoint.getVirtualFileById(id), includePermissions, propertyFilter);
    }

    @Override
    public Item getItem(String id, boolean includePermissions) throws VirtualFileSystemException {
        return getItem(id, includePermissions, PropertyFilter.ALL_FILTER);
    }

    @Path("itembypath/{path:.*}")
    @Override
    public Item getItemByPath(@PathParam("path") String path,
                              @QueryParam("versionId") String versionId,
                              @DefaultValue("false") @QueryParam("includePermissions") Boolean includePermissions,
                              @DefaultValue(PropertyFilter.ALL) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        VirtualFile virtualFile = getVirtualFileByPath(path);
        if (virtualFile.isFile()) {
            if (versionId != null) {
                virtualFile = virtualFile.getVersion(versionId);
            }
        } else if (versionId != null) {
            throw new InvalidArgumentException(String.format("Object '%s' is not a file. Version ID must not be set. ", path));
        }

        return fromVirtualFile(virtualFile, includePermissions, propertyFilter);
    }

    @Override
    public Item getItemByPath(String path, String versionId, boolean includePermissions) throws VirtualFileSystemException {
        return getItemByPath(path, versionId, includePermissions, PropertyFilter.ALL_FILTER);
    }

    @Path("version/{id}/{versionId}")
    @Override
    public ContentStream getVersion(@PathParam("id") String id, @PathParam("versionId") String versionId)
            throws VirtualFileSystemException {
        return mountPoint.getVirtualFileById(id).getVersion(versionId).getContent();
    }

    @Path("version-history/{id}")
    @Override
    public ItemList getVersions(@PathParam("id") String id,
                                @DefaultValue("-1") @QueryParam("maxItems") int maxItems,
                                @QueryParam("skipCount") int skipCount,
                                @DefaultValue(PropertyFilter.ALL) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        if (skipCount < 0) {
            throw new InvalidArgumentException("'skipCount' parameter is negative. ");
        }
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (!virtualFile.isFile()) {
            throw new InvalidArgumentException(
                    String.format("Unable get versions of '%s'. Versioning allowed for files only. ", virtualFile.getPath()));
        }
        final LazyIterator<VirtualFile> versions = virtualFile.getVersions(VirtualFileFilter.ALL);
        try {
            if (skipCount > 0) {
                versions.skip(skipCount);
            }
        } catch (NoSuchElementException nse) {
            throw new InvalidArgumentException("'skipCount' parameter is greater then total number of items. ");
        }

        final List<Item> items = new ArrayList<>();
        for (int count = 0; versions.hasNext() && (maxItems < 0 || count < maxItems); count++) {
            items.add(fromVirtualFile(versions.next(), false, propertyFilter));
        }
        return DtoFactory.getInstance().createDto(ItemList.class).withItems(items).withNumItems(versions.size())
                         .withHasMoreItems(versions.hasNext());
    }

    @Override
    public ItemList getVersions(String id, int maxItems, int skipCount) throws VirtualFileSystemException {
        return getVersions(id, maxItems, skipCount, PropertyFilter.ALL_FILTER);
    }

    @Path("lock/{id}")
    @Override
    public Lock lock(@PathParam("id") String id,
                     @DefaultValue("0") @QueryParam("timeout") long timeout) throws NotSupportedException, VirtualFileSystemException {
        if (!getInfo().isLockSupported()) {
            throw new NotSupportedException("Locking is not supported. ");
        }
        final VirtualFileSystemUser user = userContext.getVirtualFileSystemUser();
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (!virtualFile.isFile()) {
            throw new InvalidArgumentException(
                    String.format("Unable lock '%s'. Locking allowed for files only. ", virtualFile.getPath()));
        }
        final String lockToken = mountPoint.getVirtualFileById(id).lock(timeout);
        return DtoFactory.getInstance().createDto(Lock.class).withLockToken(lockToken).withOwner(user.getUserId()).withTimeout(timeout);
    }

    @Path("move/{id}")
    @Override
    public Item move(@PathParam("id") String id,
                     @QueryParam("parentId") String parentId,
                     @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile origin = mountPoint.getVirtualFileById(id);
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create move item. Item specified as parent is not a folder. ");
        }
        return fromVirtualFile(origin.moveTo(parent, lockToken), false, PropertyFilter.ALL_FILTER);
    }

    @Path("rename/{id}")
    @Override
    public Item rename(@PathParam("id") String id,
                       @QueryParam("mediaType") MediaType newMediaType,
                       @QueryParam("newname") String newName,
                       @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        if ((newName == null || newName.isEmpty()) && newMediaType == null) {
            // Nothing to do. Return unchanged object.
            return getItem(id, false, PropertyFilter.ALL_FILTER);
        }
        final VirtualFile origin = mountPoint.getVirtualFileById(id);
        final VirtualFile renamedVirtualFile = origin.rename(newName, newMediaType == null ? null : newMediaType.toString(), lockToken);
        return fromVirtualFile(renamedVirtualFile, false, PropertyFilter.ALL_FILTER);
    }

    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Override
    public ItemList search(MultivaluedMap<String, String> query,
                           @DefaultValue("-1") @QueryParam("maxItems") int maxItems,
                           @QueryParam("skipCount") int skipCount,
                           @DefaultValue(PropertyFilter.ALL) @QueryParam("propertyFilter") PropertyFilter propertyFilter)
            throws NotSupportedException, VirtualFileSystemException {
        if (searcherProvider != null) {
            if (skipCount < 0) {
                throw new InvalidArgumentException("'skipCount' parameter is negative. ");
            }
            final QueryExpression expr = new QueryExpression()
                    .setPath(query.getFirst("path"))
                    .setName(query.getFirst("name"))
                    .setMediaType(query.getFirst("mediaType"))
                    .setText(query.getFirst("text"));

            final String[] result = searcherProvider.getSearcher(mountPoint, true).search(expr);
            if (skipCount > 0) {
                if (skipCount > result.length) {
                    throw new InvalidArgumentException("'skipCount' parameter is greater then total number of items. ");
                }
            }
            final int length = maxItems > 0 ? Math.min(result.length, maxItems) : result.length;
            final List<Item> items = new ArrayList<>(length);
            for (int i = skipCount; i < length; i++) {
                String path = result[i];
                try {
                    items.add(fromVirtualFile(getVirtualFileByPath(path), false, propertyFilter));
                } catch (ItemNotFoundException ignored) {
                }
            }

            return DtoFactory.getInstance().createDto(ItemList.class).withItems(items).withNumItems(result.length)
                             .withHasMoreItems(length < result.length);
        }
        throw new NotSupportedException("Not supported. ");
    }

    @Override
    public ItemList search(MultivaluedMap<String, String> query, int maxItems, int skipCount)
            throws NotSupportedException, VirtualFileSystemException {
        return search(query, maxItems, skipCount, PropertyFilter.ALL_FILTER);
    }

    @Override
    public ItemList search(@QueryParam("statement") String statement,
                           @DefaultValue("-1") @QueryParam("maxItems") int maxItems,
                           @QueryParam("skipCount") int skipCount) throws NotSupportedException, VirtualFileSystemException {
        // No plan to support SQL at the moment.
        throw new NotSupportedException("Not supported. ");
    }

    @Path("unlock/{id}")
    @Override
    public void unlock(@PathParam("id") String id, @QueryParam("lockToken") String lockToken)
            throws NotSupportedException, VirtualFileSystemException {
        if (!getInfo().isLockSupported()) {
            throw new NotSupportedException("Locking is not supported. ");
        }
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (!virtualFile.isFile()) {
            throw new VirtualFileSystemException(
                    String.format("Unable unlock '%s'. Locking allowed for files only. ", virtualFile.getPath()));
        }
        virtualFile.unlock(lockToken);
    }

    @Path("acl/{id}")
    @Override
    public void updateACL(@PathParam("id") String id,
                          List<AccessControlEntry> acl,
                          @DefaultValue("false") @QueryParam("override") Boolean override,
                          @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        if (getInfo().getAclCapability() != ACLCapability.MANAGE) {
            throw new NotSupportedException("Managing of ACL is not supported. ");
        }
        mountPoint.getVirtualFileById(id).updateACL(acl, override, lockToken);
    }

    @Path("content/{id}")
    @Override
    public void updateContent(
            @PathParam("id") String id,
            @DefaultValue(MediaType.APPLICATION_OCTET_STREAM) @HeaderParam("Content-Type") MediaType mediaType,
            InputStream newContent,
            @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (!virtualFile.isFile()) {
            throw new InvalidArgumentException(String.format("Unable update content. Item '%s' is not a file. ", id));
        }
        virtualFile.updateContent(mediaType != null ? mediaType.toString() : null, newContent, lockToken);
    }

    @Path("item/{id}")
    @Override
    public Item updateItem(@PathParam("id") String id,
                           List<Property> properties,
                           @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        virtualFile.updateProperties(properties, lockToken);
        return fromVirtualFile(virtualFile, false, PropertyFilter.ALL_FILTER);
    }

    @Path("export/{folderId}")
    @Override
    public ContentStream exportZip(@PathParam("folderId") String folderId) throws IOException, VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(folderId);
        if (!virtualFile.isFolder()) {
            throw new InvalidArgumentException(String.format("Unable export to zip. Item '%s' is not a folder. ", virtualFile.getPath()));
        }
        return exportZip(virtualFile);
    }

    // For usage from Project API.
    public static ContentStream exportZip(VirtualFile folder) throws IOException, VirtualFileSystemException {
        return folder.zip(VirtualFileFilter.ALL);
    }

    @Path("export/{folderId}")
    @Override
    public Response exportZip(@PathParam("folderId") String folderId, InputStream in) throws IOException, VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(folderId);
        if (!virtualFile.isFolder()) {
            throw new InvalidArgumentException(String.format("Unable export to zip. Item '%s' is not a folder. ", virtualFile.getPath()));
        }
        return exportZip(virtualFile, in);
    }

    // For usage from Project API.
    public static Response exportZip(VirtualFile folder, InputStream in) throws IOException, VirtualFileSystemException {
        final List<Pair<String, String>> remote = new LinkedList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            String hash = line.substring(0, 32); // 32 is length of MD-5 hash sum
            int startPath = 33;
            int l = line.length();
            while (startPath < l && Character.isWhitespace(line.charAt(startPath))) {
                startPath++;
            }
            String relPath = line.substring(startPath);
            remote.add(Pair.of(hash, relPath));
        }
        if (remote.isEmpty()) {
            final ContentStream zip = folder.zip(VirtualFileFilter.ALL);
            return Response
                    .ok(zip.getStream(), zip.getMimeType())
                    .lastModified(zip.getLastModificationDate())
                    .header(HttpHeaders.CONTENT_LENGTH, Long.toString(zip.getLength()))
                    .header("Content-Disposition", "attachment; filename=\"" + zip.getFileName() + '"')
                    .build();
        }
        final LazyIterator<Pair<String, String>> md5Sums = folder.countMd5Sums();
        final int size = md5Sums.size();
        final List<Pair<String, String>> local =
                size > 0 ? new ArrayList<Pair<String, String>>(size) : new ArrayList<Pair<String, String>>();
        while (md5Sums.hasNext()) {
            local.add(md5Sums.next());
        }
        final Comparator<Pair<String, String>> comp = new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                return o1.second.compareTo(o2.second);
            }
        };
        Collections.sort(remote, comp);
        Collections.sort(local, comp);
        int remoteIndex = 0;
        int localIndex = 0;
        final List<Pair<String, com.codenvy.api.vfs.server.Path>> diff = new LinkedList<>();
        while (remoteIndex < remote.size() && localIndex < local.size()) {
            final Pair<String, String> remoteItem = remote.get(remoteIndex);
            final Pair<String, String> localItem = local.get(localIndex);
            // compare path
            int r = remoteItem.second.compareTo(localItem.second);
            if (r == 0) {
                // remote and local file exist, compare md5sum
                if (!remoteItem.first.equals(localItem.first)) {
                    diff.add(Pair.of(remoteItem.second, folder.getVirtualFilePath().newPath(localItem.second)));
                }
                remoteIndex++;
                localIndex++;
            } else if (r > 0) {
                // new file
                diff.add(Pair.of((String)null, folder.getVirtualFilePath().newPath(localItem.second)));
                localIndex++;
            } else {
                // deleted file
                diff.add(Pair.of(remoteItem.second, (com.codenvy.api.vfs.server.Path)null));
                remoteIndex++;
            }
        }
        while (remoteIndex < remote.size()) {
            diff.add(Pair.of(remote.get(remoteIndex++).second, (com.codenvy.api.vfs.server.Path)null));
        }
        while (localIndex < local.size()) {
            diff.add(Pair.of((String)null, folder.getVirtualFilePath().newPath(local.get(localIndex++).second)));
        }

        if (diff.isEmpty()) {
            return Response.status(204).build();
        }

        final ContentStream zip = folder.zip(new VirtualFileFilter() {
            @Override
            public boolean accept(VirtualFile file) throws VirtualFileSystemException {
                for (Pair<String, com.codenvy.api.vfs.server.Path> pair : diff) {
                    if (pair.second != null
                        && (pair.second.equals(file.getVirtualFilePath()) || pair.second.isChild(file.getVirtualFilePath()))) {
                        return true;
                    }
                }
                return false;
            }
        });
        final StringBuilder deleted = new StringBuilder();
        for (Pair<String, com.codenvy.api.vfs.server.Path> pair : diff) {
            if (pair.first != null && pair.second == null) {
                if (deleted.length() > 0) {
                    deleted.append(',');
                }
                deleted.append(pair.first);
            }
        }
        final Response.ResponseBuilder responseBuilder = Response
                .ok(zip.getStream(), zip.getMimeType())
                .lastModified(zip.getLastModificationDate())
                .header(HttpHeaders.CONTENT_LENGTH, Long.toString(zip.getLength()))
                .header("Content-Disposition", "attachment; filename=\"" + zip.getFileName() + '"');
        if (deleted.length() > 0) {
            responseBuilder.header("x-removed-paths", deleted.toString());
        }
        return responseBuilder.build();
    }


    @Path("import/{parentId}")
    @Override
    public void importZip(@PathParam("parentId") String parentId,
                          InputStream in,
                          @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
            throws VirtualFileSystemException, IOException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        importZip(parent, in, overwrite);
    }

    // For usage from Project API.
    public static void importZip(VirtualFile parent, InputStream in, boolean overwrite) throws VirtualFileSystemException, IOException {
        parent.unzip(in, overwrite);
    }

    @Path("downloadfile/{id}")
    @Override
    public Response downloadFile(@PathParam("id") String id) throws VirtualFileSystemException {
        final ContentStream content = getContent(id);
        return Response
                .ok(content.getStream(), content.getMimeType())
                .lastModified(content.getLastModificationDate())
                .header(HttpHeaders.CONTENT_LENGTH, Long.toString(content.getLength()))
                .header("Content-Disposition", "attachment; filename=\"" + content.getFileName() + '"')
                .build();
    }

    @Path("uploadfile/{parentId}")
    @Override
    public Response uploadFile(@PathParam("parentId") String parentId, Iterator<FileItem> formData)
            throws IOException, VirtualFileSystemException {
        try {
            FileItem contentItem = null;
            String mediaType = null;
            String name = null;
            boolean overwrite = false;

            while (formData.hasNext()) {
                FileItem item = formData.next();
                if (!item.isFormField()) {
                    if (contentItem == null) {
                        contentItem = item;
                    } else {
                        throw new InvalidArgumentException("More then one upload file is found but only one should be. ");
                    }
                } else if ("mimeType".equals(item.getFieldName())) {
                    mediaType = item.getString().trim();
                } else if ("name".equals(item.getFieldName())) {
                    name = item.getString().trim();
                } else if ("overwrite".equals(item.getFieldName())) {
                    overwrite = Boolean.parseBoolean(item.getString().trim());
                }
            }

            if (contentItem == null) {
                throw new InvalidArgumentException("Cannot find file for upload. ");
            }
            if (name == null || name.isEmpty()) {
                name = contentItem.getName();
            }
            if (mediaType == null || mediaType.isEmpty()) {
                mediaType = contentItem.getContentType();
            }

            try {
                createFile(parentId,
                           name,
                           mediaType == null ? MediaType.APPLICATION_OCTET_STREAM_TYPE : MediaType.valueOf(mediaType),
                           contentItem.getInputStream());
            } catch (ItemAlreadyExistException e) {
                if (!overwrite) {
                    throw new ItemAlreadyExistException("Unable upload file. Item with the same name exists. ");
                }
                mountPoint.getVirtualFileById(parentId).getChild(name).updateContent(mediaType, contentItem.getInputStream(), null);
            }

            return Response.ok("", MediaType.TEXT_HTML).build();
        } catch (VirtualFileSystemException | IOException e) {
            HtmlErrorFormatter.sendErrorAsHTML(e);
            // never thrown
            throw e;
        }
    }

    @Path("downloadzip/{folderId}")
    @Override
    public Response downloadZip(@PathParam("folderId") String folderId) throws IOException, VirtualFileSystemException {
        final ContentStream zip = exportZip(folderId);
        return Response //
                .ok(zip.getStream(), zip.getMimeType()) //
                .lastModified(zip.getLastModificationDate()) //
                .header(HttpHeaders.CONTENT_LENGTH, Long.toString(zip.getLength())) //
                .header("Content-Disposition", "attachment; filename=\"" + zip.getFileName() + '"') //
                .build();
    }

    @Path("uploadzip/{parentId}")
    @Override
    public Response uploadZip(@PathParam("parentId") String parentId, Iterator<FileItem> formData)
            throws IOException, VirtualFileSystemException {
        try {
            FileItem contentItem = null;
            boolean overwrite = false;
            while (formData.hasNext()) {
                FileItem item = formData.next();
                if (!item.isFormField()) {
                    if (contentItem == null) {
                        contentItem = item;
                    } else {
                        throw new InvalidArgumentException("More then one upload file is found but only one should be. ");
                    }
                } else if ("overwrite".equals(item.getFieldName())) {
                    overwrite = Boolean.parseBoolean(item.getString().trim());
                }
            }
            if (contentItem == null) {
                throw new InvalidArgumentException("Cannot find file for upload. ");
            }
            importZip(parentId, contentItem.getInputStream(), overwrite);
            return Response.ok("", MediaType.TEXT_HTML).build();
        } catch (VirtualFileSystemException | IOException e) {
            HtmlErrorFormatter.sendErrorAsHTML(e);
            // never thrown
            throw e;
        }
    }

   /* ==================================================================== */

    protected VirtualFile getVirtualFileByPath(String path) throws VirtualFileSystemException {
        return mountPoint.getVirtualFile(path);
    }

    protected Item fromVirtualFile(VirtualFile virtualFile, boolean includePermissions, PropertyFilter propertyFilter)
            throws VirtualFileSystemException {
        return fromVirtualFile(virtualFile, includePermissions, propertyFilter, true);
    }

    protected Item fromVirtualFile(VirtualFile virtualFile, boolean includePermissions, PropertyFilter propertyFilter, boolean addLinks)
            throws VirtualFileSystemException {
        final String id = virtualFile.getId();
        final String name = virtualFile.getName();
        final String path = virtualFile.getPath();
        final boolean isRoot = virtualFile.isFolder() && virtualFile.isRoot();
        final String parentId = isRoot ? null : virtualFile.getParent().getId();
        final String mediaType = virtualFile.getMediaType();
        final long created = virtualFile.getCreationDate();

        Item item;
        if (virtualFile.isFile()) {
            final boolean locked = virtualFile.isLocked();
            File dtoFile = (File)DtoFactory.getInstance().createDto(File.class)
                                           .withVersionId(virtualFile.getVersionId())
                                           .withLength(virtualFile.getLength())
                                           .withLastModificationDate(virtualFile.getLastModificationDate())
                                           .withLocked(locked)
                                           .withItemType(ItemType.FILE)
                                           .withParentId(parentId)
                                           .withId(id)
                                           .withName(name)
                                           .withPath(path)
                                           .withMimeType(mediaType)
                                           .withCreationDate(created)
                                           .withVfsId(vfsId)
                                           .withProperties(virtualFile.getProperties(propertyFilter));
            if (addLinks) {
                dtoFile.setLinks(LinksHelper.createFileLinks(baseUri, vfsId, id, id, path, mediaType, locked, parentId));
            }
            item = dtoFile;
        } else {
            Folder dtoFolder = (Folder)DtoFactory.getInstance().createDto(Folder.class)
                                                 .withItemType(ItemType.FOLDER)
                                                 .withParentId(parentId)
                                                 .withId(id)
                                                 .withName(name)
                                                 .withPath(path)
                                                 .withMimeType(mediaType)
                                                 .withCreationDate(created)
                                                 .withVfsId(vfsId)
                                                 .withProperties(virtualFile.getProperties(propertyFilter));
            if (addLinks) {
                dtoFolder.setLinks(LinksHelper.createFolderLinks(baseUri, vfsId, id, isRoot, parentId));
            }
            item = dtoFolder;
        }

        if (includePermissions) {
            VirtualFileSystemUser user = userContext.getVirtualFileSystemUser();
            VirtualFile current = virtualFile;
            while (current != null) {
                final Map<Principal, Set<BasicPermissions>> objectPermissions = current.getPermissions();
                if (!objectPermissions.isEmpty()) {
                    Set<String> userPermissions = new HashSet<>(4);
                    final Principal userPrincipal =
                            DtoFactory.getInstance().createDto(Principal.class).withName(user.getUserId()).withType(Principal.Type.USER);
                    Set<BasicPermissions> permissionsSet = objectPermissions.get(userPrincipal);
                    if (permissionsSet != null) {
                        for (BasicPermissions basicPermission : permissionsSet) {
                            userPermissions.add(basicPermission.value());
                        }
                    }
                    final Principal anyPrincipal = DtoFactory.getInstance().createDto(Principal.class)
                                                             .withName(VirtualFileSystemInfo.ANY_PRINCIPAL).withType(Principal.Type.USER);
                    permissionsSet = objectPermissions.get(anyPrincipal);
                    if (permissionsSet != null) {
                        for (BasicPermissions basicPermission : permissionsSet) {
                            userPermissions.add(basicPermission.value());
                        }
                    }
                    for (String group : user.getGroups()) {
                        final Principal groupPrincipal =
                                DtoFactory.getInstance().createDto(Principal.class).withName(group).withType(Principal.Type.GROUP);
                        permissionsSet = objectPermissions.get(groupPrincipal);
                        if (permissionsSet != null) {
                            for (BasicPermissions basicPermission : permissionsSet) {
                                userPermissions.add(basicPermission.value());
                            }
                        }
                    }
                    item.setPermissions(new ArrayList<>(userPermissions));
                    break;
                } else {
                    current = current.getParent();
                }
            }
            if (item.getPermissions() == null) {
                item.setPermissions(Arrays.asList(BasicPermissions.ALL.value()));
            }
        }

        return item;
    }
}
