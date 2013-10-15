/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.HtmlErrorFormatter;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
import com.codenvy.api.vfs.server.exceptions.ItemAlreadyExistException;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.NotSupportedException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.ChangeEvent;
import com.codenvy.api.vfs.server.observation.ChangeEvent.ChangeType;
import com.codenvy.api.vfs.server.observation.EventListenerList;
import com.codenvy.api.vfs.server.observation.ProjectUpdateListener;
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
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.ACLCapability;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Base implementation of VirtualFileSystem.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public abstract class VirtualFileSystemImpl implements VirtualFileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualFileSystemImpl.class);

    protected final String                       vfsId;
    protected final URI                          baseUri;
    protected final EventListenerList            listeners;
    protected final VirtualFileSystemUserContext userContext;
    protected final MountPoint                   mountPoint;
    protected final SearcherProvider             searcherProvider;

    public VirtualFileSystemImpl(String vfsId,
                                 URI baseUri,
                                 EventListenerList listeners,
                                 VirtualFileSystemUserContext userContext,
                                 MountPoint mountPoint,
                                 SearcherProvider searcherProvider) {
        this.vfsId = vfsId;
        this.baseUri = baseUri;
        this.listeners = listeners;
        this.userContext = userContext;
        this.mountPoint = mountPoint;
        this.searcherProvider = searcherProvider;
    }

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
        final Item copy = fromVirtualFile(virtualFileCopy, false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, copy.getId(), copy.getPath(), copy.getMimeType(), ChangeType.CREATED,
                                    userContext.getVirtualFileSystemUser()));
        }
        return copy;
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
        final File file = (File)fromVirtualFile(newVirtualFile, false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, file.getId(), file.getPath(), file.getMimeType(), ChangeType.CREATED,
                                    userContext.getVirtualFileSystemUser()));
        }
        return file;
    }

    @Path("folder/{parentId}")
    @Override
    public Folder createFolder(@PathParam("parentId") String parentId, @QueryParam("name") String name) throws VirtualFileSystemException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create new folder. Item specified as parent is not a folder. ");
        }
        final VirtualFile newVirtualFile = parent.createFolder(name);
        final Folder folder = (Folder)fromVirtualFile(newVirtualFile, false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, folder.getId(), folder.getPath(), folder.getMimeType(), ChangeType.CREATED,
                                    userContext.getVirtualFileSystemUser()));
        }
        return folder;
    }

    @Path("project/{parentId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public Project createProject(@PathParam("parentId") String parentId,
                                 @QueryParam("name") String name,
                                 @QueryParam("type") String type,
                                 List<Property> properties) throws VirtualFileSystemException {
        if (properties == null) {
            properties = new ArrayList<>(2);
        }
        if (type != null) {
            final Property projectTypeProperty = DtoFactory.getInstance().createDto(Property.class);
            projectTypeProperty.setName("vfs:projectType");
            projectTypeProperty.setValue(Collections.singletonList(type));
            properties.add(projectTypeProperty);
        }
        final Property mimeTypeProperty = DtoFactory.getInstance().createDto(Property.class);
        mimeTypeProperty.setName("vfs:mimeType");
        mimeTypeProperty.setValue(Collections.singletonList(Project.PROJECT_MIME_TYPE));
        properties.add(mimeTypeProperty);

        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create new project. Item specified as parent is not a folder. ");
        }
        final VirtualFile newVirtualFile = parent.createProject(name, properties);
        final Project project = (Project)fromVirtualFile(newVirtualFile, false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, project.getId(), project.getPath(), project.getMimeType(), ChangeType.CREATED,
                                    userContext.getVirtualFileSystemUser()));
        }
        if (newVirtualFile.getParent().isRoot()) {
            //For child project project no need to fire event for create project
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}#", name, project.getProjectType());
        }
        return project;
    }

    @Path("delete/{id}")
    @Override
    public void delete(@PathParam("id") String id, @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        if (virtualFile.isRoot()) {
            throw new InvalidArgumentException("Unable delete root folder. ");
        }
        final String path = virtualFile.getPath();
        final String mediaType = virtualFile.getMediaType();
        String name = null;
        String projectType = null;
        final boolean isProject = virtualFile.isProject();
        final boolean isInRootFolder = virtualFile.getParent().isRoot();
        if (isProject) {
            name = virtualFile.getName();
            projectType = virtualFile.getPropertyValue("vfs:projectType");
        }
        virtualFile.delete(lockToken);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, id, path, mediaType, ChangeType.DELETED, userContext.getVirtualFileSystemUser()));
        }
        if (isProject) {
            //For child project project no need to fire event for delete project
            if (isInRootFolder) {
                LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}#", name, projectType);
            }
        }
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
                           || (itemTypeType == ItemType.PROJECT && file.isProject())
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
        final ItemList itemList = DtoFactory.getInstance().createDto(ItemList.class);
        itemList.setItems(items);
        itemList.setNumItems(children.size());
        itemList.setHasMoreItems(children.hasNext());
        return itemList;
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
        final ItemNode tree = DtoFactory.getInstance().createDto(ItemNode.class);
        tree.setItem(fromVirtualFile(virtualFile, includePermissions, propertyFilter));
        tree.setChildren(getTreeLevel(virtualFile, depth, includePermissions, propertyFilter));
        return tree;
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
            final ItemNode itemNode = DtoFactory.getInstance().createDto(ItemNode.class);
            itemNode.setItem(fromVirtualFile(next, includePermissions, propertyFilter));
            itemNode.setChildren(getTreeLevel(next, depth - 1, includePermissions, propertyFilter));
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
        final ItemList itemList = DtoFactory.getInstance().createDto(ItemList.class);
        itemList.setItems(items);
        itemList.setNumItems(versions.size());
        itemList.setHasMoreItems(versions.hasNext());
        return itemList;
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
        final Lock lock = DtoFactory.getInstance().createDto(Lock.class);
        lock.setLockToken(lockToken);
        lock.setOwner(user.getUserId());
        lock.setTimeout(timeout);
        return lock;
    }

    @Path("move/{id}")
    @Override
    public Item move(@PathParam("id") String id,
                     @QueryParam("parentId") String parentId,
                     @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile origin = mountPoint.getVirtualFileById(id);
        final String oldPath = origin.getPath();
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        if (!parent.isFolder()) {
            throw new InvalidArgumentException("Unable create move item. Item specified as parent is not a folder. ");
        }
        final Item moved = fromVirtualFile(origin.moveTo(parent, lockToken), false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, moved.getId(), moved.getPath(), oldPath, moved.getMimeType(), ChangeType.MOVED,
                                    userContext.getVirtualFileSystemUser()));
        }
        return moved;
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
        final boolean isProjectBefore = origin.isProject();
        final String oldPath = origin.getPath();
        final VirtualFile renamedVirtualFile = origin.rename(newName, newMediaType == null ? null : newMediaType.toString(), lockToken);
        final Item renamed = fromVirtualFile(renamedVirtualFile, false, PropertyFilter.ALL_FILTER);
        final boolean isProjectAfter = renamedVirtualFile.isProject();
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, renamed.getId(), renamed.getPath(), oldPath, renamed.getMimeType(), ChangeType.RENAMED,
                                    userContext.getVirtualFileSystemUser()));
        }
        if (isProjectAfter && !isProjectBefore) {
            //For child project project no need to fire event for create project
            if (renamedVirtualFile.getParent().isRoot()) {
                LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}#", renamed.getName(), ((Project)renamed).getProjectType());
            }
        }
        return renamed;
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

            final ItemList itemList = DtoFactory.getInstance().createDto(ItemList.class);
            itemList.setItems(items);
            itemList.setNumItems(result.length);
            itemList.setHasMoreItems(length < result.length);
            return itemList;
        }
        throw new NotSupportedException("Not supported. ");
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
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id).updateACL(acl, override, lockToken);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, virtualFile.getId(), virtualFile.getPath(), virtualFile.getMediaType(),
                                    ChangeType.ACL_UPDATED,
                                    userContext.getVirtualFileSystemUser()));
        }
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
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, virtualFile.getId(), virtualFile.getPath(), virtualFile.getMediaType(),
                                    ChangeType.CONTENT_UPDATED,
                                    userContext.getVirtualFileSystemUser()));
        }
    }

    @Path("item/{id}")
    @Override
    public Item updateItem(@PathParam("id") String id,
                           List<Property> properties,
                           @QueryParam("lockToken") String lockToken) throws VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(id);
        final boolean isProjectBefore = virtualFile.isProject();
        virtualFile.updateProperties(properties, lockToken);
        final boolean isProjectAfter = virtualFile.isProject();
        final Item updated = fromVirtualFile(virtualFile, false, PropertyFilter.ALL_FILTER);
        if (listeners != null) {
            listeners.notifyListeners(
                    new ChangeEvent(this, updated.getId(), updated.getPath(), updated.getMimeType(), ChangeType.PROPERTIES_UPDATED,
                                    userContext.getVirtualFileSystemUser()));
        }
        if (isProjectAfter && !isProjectBefore) {
            //For child project project no need to fire event for create project
            if (virtualFile.getParent().isRoot()) {
                LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}#", updated.getName(), ((Project)updated).getProjectType());
            }
        }

        /*boolean jRebelPropertyUpdated = false;
        for (Property p : properties) {
            if (p.getName().equals("jrebel")) {
                jRebelPropertyUpdated = true;
                break;
            }
        }
        if (jRebelPropertyUpdated) {
            final String projectType = updated.getPropertyValue("vfs:projectType");
            //TODO : move somewhere VFS must not be care about any project specific properties
            boolean jRebelUsage = Boolean.parseBoolean(updated.getPropertyValue("jrebel"));
            VirtualFileSystemUser user = userContext.getVirtualFileSystemUser();
            LOG.info("EVENT#jrebel-usage# WS#"
                     + EnvironmentContext.getCurrent().getVariable(EnvironmentContext.WORKSPACE_NAME).toString() + "# USER#"
                     + user.getUserId() + "# PROJECT#" + updated.getName() + "# TYPE#" + projectType + "# JREBEL#" + jRebelUsage + "#");
        }*/
        return updated;
    }

    @Path("export/{folderId}")
    @Override
    public ContentStream
    exportZip(@PathParam("folderId") String folderId) throws IOException, VirtualFileSystemException {
        final VirtualFile virtualFile = mountPoint.getVirtualFileById(folderId);
        if (!virtualFile.isFolder()) {
            throw new InvalidArgumentException(String.format("Unable export to zip. Item '%s' is not a folder. ", virtualFile.getPath()));
        }
        return virtualFile.zip();
    }

    @Path("import/{parentId}")
    @Override
    public void importZip(@PathParam("parentId") String parentId,
                          InputStream in,
                          @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
            throws VirtualFileSystemException, IOException {
        final VirtualFile parent = mountPoint.getVirtualFileById(parentId);
        final boolean isProjectBefore = parent.isProject();
        parent.unzip(in, overwrite);
        final boolean isProjectAfter = parent.isProject();
        if (!isProjectBefore && isProjectAfter) {
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}#", parent.getName(), parent.getPropertyValue("vfs:projectType"));
        }
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

                final VirtualFile file = mountPoint.getVirtualFileById(parentId).getChild(name).updateContent(mediaType,
                                                                                                              contentItem.getInputStream(),
                                                                                                              null);
                if (listeners != null) {
                    listeners.notifyListeners(
                            new ChangeEvent(this, file.getId(), file.getPath(), file.getMediaType(), ChangeType.CONTENT_UPDATED,
                                            userContext.getVirtualFileSystemUser()));
                }
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

    @Path("watch/start/{projectId}")
    @Override
    public void startWatchUpdates(@PathParam("projectId") String projectId) throws VirtualFileSystemException {
        if (listeners == null) {
            throw new VirtualFileSystemException("EventListenerList is not configured properly. ");
        }
        final VirtualFile project = mountPoint.getVirtualFileById(projectId);
        if (!project.isProject()) {
            throw new InvalidArgumentException(String.format("Item '%s' is not a project. ", project.getPath()));
        }
        if (listeners.addEventListener(ProjectUpdateEventFilter.newFilter(this, project), new ProjectUpdateListener(projectId))) {
            List<Property> properties = new ArrayList<>(1);
            Property myProperty = DtoFactory.getInstance().createDto(Property.class);
            myProperty.setName("vfs:lastUpdateTime");
            myProperty.setValue(Collections.singletonList("0"));
            properties.add(myProperty);
            project.updateProperties(properties, null);
        }
    }

    @Path("watch/stop/{projectId}")
    @Override
    public void stopWatchUpdates(@PathParam("projectId") String projectId) throws VirtualFileSystemException {
        if (listeners != null) {
            final VirtualFile project = mountPoint.getVirtualFileById(projectId);
            if (!project.isProject()) {
                return;
            }
            if (!listeners.removeEventListener(ProjectUpdateEventFilter.newFilter(this, project),
                                               new ProjectUpdateListener(projectId))) {
                throw new InvalidArgumentException(String.format("Project '%s' is not under watching. ", project.getPath()));
            }
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
            final File dtoFile = DtoFactory.getInstance().createDto(File.class);
            dtoFile.setItemType(ItemType.FILE);
            dtoFile.setParentId(parentId);
            dtoFile.setId(id);
            dtoFile.setName(name);
            dtoFile.setPath(path);
            dtoFile.setLength(virtualFile.getLength());
            dtoFile.setMimeType(mediaType);
            dtoFile.setCreationDate(created);
            dtoFile.setLastModificationDate(virtualFile.getLastModificationDate());
            dtoFile.setLocked(virtualFile.isLocked());
            dtoFile.setVersionId(virtualFile.getVersionId());
            dtoFile.setProperties(virtualFile.getProperties(propertyFilter));
            if (addLinks) {
                dtoFile.setLinks(LinksHelper.createFileLinks(baseUri, (String)EnvironmentContext.getCurrent().getVariable(
                        EnvironmentContext.WORKSPACE_NAME), id, id, path, mediaType, locked, parentId));
            }
            item = dtoFile;
        } else {
            if (virtualFile.isProject()) {
                final String projectType = virtualFile.getPropertyValue("vfs:projectType");
                Project dtoProject = DtoFactory.getInstance().createDto(Project.class);
                dtoProject.setItemType(ItemType.PROJECT);
                dtoProject.setParentId(parentId);
                dtoProject.setId(id);
                dtoProject.setName(name);
                dtoProject.setPath(path);
                dtoProject.setMimeType(mediaType);
                dtoProject.setCreationDate(created);
                dtoProject.setProperties(virtualFile.getProperties(propertyFilter));
                dtoProject.setProjectType(projectType == null ? "default" : projectType);
                if (addLinks) {
                    dtoProject.setLinks(LinksHelper.createProjectLinks(baseUri, (String)EnvironmentContext.getCurrent().getVariable(
                            EnvironmentContext.WORKSPACE_NAME), id, parentId));
                }
                item = dtoProject;
            } else {
                Folder dtoFolder = DtoFactory.getInstance().createDto(Folder.class);
                dtoFolder.setItemType(ItemType.FOLDER);
                dtoFolder.setParentId(parentId);
                dtoFolder.setId(id);
                dtoFolder.setName(name);
                dtoFolder.setPath(path);
                dtoFolder.setMimeType(mediaType);
                dtoFolder.setCreationDate(created);
                dtoFolder.setProperties(virtualFile.getProperties(propertyFilter));
                if (addLinks) {
                    dtoFolder.setLinks(LinksHelper.createFolderLinks(baseUri, (String)EnvironmentContext.getCurrent().getVariable(
                            EnvironmentContext.WORKSPACE_NAME), id, isRoot, parentId));
                }
                item = dtoFolder;
            }
        }

        if (includePermissions) {
            VirtualFileSystemUser user = userContext.getVirtualFileSystemUser();
            VirtualFile current = virtualFile;
            while (current != null) {
                final Map<Principal, Set<BasicPermissions>> objectPermissions = current.getPermissions();
                if (!objectPermissions.isEmpty()) {
                    Set<String> userPermissions = new HashSet<>(4);
                    final Principal userPrincipal = DtoFactory.getInstance().createDto(Principal.class);
                    userPrincipal.setName(user.getUserId());
                    userPrincipal.setType(Principal.Type.USER);
                    Set<BasicPermissions> permissionsSet = objectPermissions.get(userPrincipal);
                    if (permissionsSet != null) {
                        for (BasicPermissions basicPermission : permissionsSet) {
                            userPermissions.add(basicPermission.value());
                        }
                    }
                    final Principal anyPrincipal = DtoFactory.getInstance().createDto(Principal.class);
                    anyPrincipal.setName(VirtualFileSystemInfo.ANY_PRINCIPAL);
                    anyPrincipal.setType(Principal.Type.USER);
                    permissionsSet = objectPermissions.get(anyPrincipal);
                    if (permissionsSet != null) {
                        for (BasicPermissions basicPermission : permissionsSet) {
                            userPermissions.add(basicPermission.value());
                        }
                    }
                    for (String group : user.getGroups()) {
                        final Principal groupPrincipal = DtoFactory.getInstance().createDto(Principal.class);
                        groupPrincipal.setName(group);
                        groupPrincipal.setType(Principal.Type.GROUP);
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
