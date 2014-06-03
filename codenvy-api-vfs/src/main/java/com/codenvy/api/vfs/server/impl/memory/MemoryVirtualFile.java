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
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.core.util.ContentTypeGuesser;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileVisitor;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
import com.codenvy.api.vfs.server.exceptions.ItemAlreadyExistException;
import com.codenvy.api.vfs.server.exceptions.LockException;
import com.codenvy.api.vfs.server.exceptions.NotSupportedException;
import com.codenvy.api.vfs.server.exceptions.PermissionDeniedException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemRuntimeException;
import com.codenvy.api.vfs.server.observation.CreateEvent;
import com.codenvy.api.vfs.server.observation.DeleteEvent;
import com.codenvy.api.vfs.server.observation.MoveEvent;
import com.codenvy.api.vfs.server.observation.RenameEvent;
import com.codenvy.api.vfs.server.observation.UpdateACLEvent;
import com.codenvy.api.vfs.server.observation.UpdateContentEvent;
import com.codenvy.api.vfs.server.observation.UpdatePropertiesEvent;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.server.util.NotClosableInputStream;
import com.codenvy.api.vfs.server.util.ZipContent;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Folder;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * In-memory implementation of VirtualFile.
 * <p/>
 * NOTE: This implementation is not thread safe.
 *
 * @author andrew00x
 */
public class MemoryVirtualFile implements VirtualFile {
    private static final Logger  LOG    = LoggerFactory.getLogger(MemoryVirtualFile.class);
    private static final boolean FILE   = false;
    private static final boolean FOLDER = true;

    private static MemoryVirtualFile newFile(MemoryVirtualFile parent, String name, InputStream content, String mediaType)
            throws IOException {
        return new MemoryVirtualFile(parent, ObjectIdGenerator.generateId(), name, content, mediaType);
    }

    private static MemoryVirtualFile newFile(MemoryVirtualFile parent, String name, byte[] content, String mediaType) {
        return new MemoryVirtualFile(parent, ObjectIdGenerator.generateId(), name, content, mediaType);
    }

    private static MemoryVirtualFile newFolder(MemoryVirtualFile parent, String name) {
        return new MemoryVirtualFile(parent, ObjectIdGenerator.generateId(), name);
    }

    //

    private final boolean                               type;
    private final String                                id;
    private final Map<Principal, Set<BasicPermissions>> permissionsMap;
    private final Map<String, List<String>>             properties;
    private final long                                  creationDate;
    private final Map<String, VirtualFile>              children;
    private final MemoryMountPoint                      mountPoint;

    private String            name;
    private MemoryVirtualFile parent;
    private byte[]            content;
    private long              lastModificationDate;
    private LockHolder        lock;
    private boolean exists = true;

    // --- File ---
    private MemoryVirtualFile(MemoryVirtualFile parent, String id, String name, InputStream content, String mediaType)
            throws IOException {
        this(parent, id, name, content == null ? null : ByteStreams.toByteArray(content), mediaType);
    }

    private MemoryVirtualFile(MemoryVirtualFile parent, String id, String name, byte[] content, String mediaType) {
        this.mountPoint = (MemoryMountPoint)parent.getMountPoint();
        this.parent = parent;
        this.type = FILE;
        this.id = id;
        this.name = name;
        this.permissionsMap = new HashMap<>();
        this.properties = new HashMap<>();
        this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        this.content = content == null ? new byte[0] : content;
        if (mediaType != null) {
            properties.put("vfs:mimeType", Arrays.asList(mediaType));
        }
        children = Collections.emptyMap();
    }

    // -- Folder ---
    private MemoryVirtualFile(MemoryVirtualFile parent, String id, String name) {
        this.mountPoint = (MemoryMountPoint)parent.getMountPoint();
        this.parent = parent;
        this.type = FOLDER;
        this.id = id;
        this.name = name;
        this.permissionsMap = new HashMap<>();
        this.properties = new HashMap<>();
        this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        children = new HashMap<>();
    }

    /* root folder */ MemoryVirtualFile(MountPoint mountPoint) {
        this.mountPoint = (MemoryMountPoint)mountPoint;
        this.type = FOLDER;
        this.id = ObjectIdGenerator.generateId();
        this.name = "";
        this.permissionsMap = new HashMap<>();
        final Principal groupPrincipal = DtoFactory.getInstance().createDto(Principal.class);
        groupPrincipal.setName("workspace/developer");
        groupPrincipal.setType(Principal.Type.GROUP);
        final Principal anyPrincipal = DtoFactory.getInstance().createDto(Principal.class)
                                                 .withName(VirtualFileSystemInfo.ANY_PRINCIPAL).withType(Principal.Type.USER);
        this.permissionsMap.put(groupPrincipal, EnumSet.of(BasicPermissions.ALL));
        this.permissionsMap.put(anyPrincipal, EnumSet.of(BasicPermissions.READ));
        this.properties = new HashMap<>();
        this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        children = new HashMap<>();
    }

    @Override
    public boolean isFile() throws VirtualFileSystemException {
        checkExist();
        return type == FILE;
    }

    @Override
    public boolean isFolder() throws VirtualFileSystemException {
        checkExist();
        return type == FOLDER;
    }

    public VirtualFile getParent() throws VirtualFileSystemException {
        checkExist();
        return parent;
    }

    public String getId() throws VirtualFileSystemException {
        checkExist();
        return id;
    }

    public String getName() throws VirtualFileSystemException {
        checkExist();
        return name;
    }

    public String getMediaType() throws VirtualFileSystemException {
        checkExist();
        String mediaType = getPropertyValue("vfs:mimeType");
        if (mediaType == null) {
            mediaType = isFile() ? ContentTypeGuesser.guessContentType(new File(getName())) : Folder.FOLDER_MIME_TYPE;
        }
        return mediaType;
    }

    @Override
    public VirtualFile setMediaType(String mediaType) throws VirtualFileSystemException {
        checkExist();
        if (mediaType == null) {
            properties.remove("vfs:mimeType");
        } else {
            properties.put("vfs:mimeType", Arrays.asList(mediaType));
        }
        return this;
    }

    public String getPath() throws VirtualFileSystemException {
        checkExist();
        VirtualFile parent = this.parent;
        if (parent == null) {
            return "/"; // for root folder
        }

        String name = this.name;
        LinkedList<String> pathSegments = new LinkedList<>();
        pathSegments.add(name);

        while (parent != null) {
            pathSegments.addFirst(parent.getName());
            parent = parent.getParent();
        }

        StringBuilder path = new StringBuilder();
        path.append('/');
        for (String seg : pathSegments) {
            if (path.length() > 1) {
                path.append('/');
            }
            path.append(seg);
        }
        return path.toString();
    }

    @Override
    public Path getVirtualFilePath() throws VirtualFileSystemException {
        return Path.fromString(getPath());
    }

    public VirtualFile updateACL(List<AccessControlEntry> acl, boolean override, String lockToken) throws VirtualFileSystemException {
        checkExist();
        if (!hasPermission(BasicPermissions.UPDATE_ACL, true)) {
            throw new PermissionDeniedException(String.format("Unable update ACL for '%s'. Operation not permitted. ", getPath()));
        }
        if (isFile() && !validateLockTokenIfLocked(lockToken)) {
            throw new LockException(String.format("Unable update ACL of item '%s'. Item is locked. ", getPath()));
        }
        final Map<Principal, Set<BasicPermissions>> update = new HashMap<>(acl.size());
        for (AccessControlEntry ace : acl) {
            final Principal principal = ace.getPrincipal();
            // Do not use 'transport' object directly, copy it instead.
            final Principal copyPrincipal = DtoFactory.getInstance().clone(principal);
            Set<BasicPermissions> permissions = update.get(copyPrincipal);
            if (permissions == null) {
                permissions = EnumSet.noneOf(BasicPermissions.class);
                update.put(copyPrincipal, permissions);
            }
            if (!(ace.getPermissions() == null || ace.getPermissions().isEmpty())) {
                for (String strPermission : ace.getPermissions()) {
                    permissions.add(BasicPermissions.fromValue(strPermission));
                }
            }
        }

        if (override) {
            permissionsMap.clear();
        }
        permissionsMap.putAll(update);
        lastModificationDate = System.currentTimeMillis();
        mountPoint.getEventService().publish(new UpdateACLEvent(mountPoint.getWorkspaceId(), getPath()));
        return this;
    }

    @Override
    public String getVersionId() throws VirtualFileSystemException {
        checkExist();
        return isFile() ? "0" : null;
    }

    @Override
    public LazyIterator<VirtualFile> getVersions(VirtualFileFilter filter) throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException("Versioning allowed for files only. ");
        }
        if (filter.accept(this)) {
            return LazyIterator.<VirtualFile>singletonIterator(this);
        }
        return LazyIterator.emptyIterator();
    }

    @Override
    public VirtualFile getVersion(String versionId) throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException("Versioning allowed for files only. ");
        }
        if ("0".equals(versionId)) {
            return this;
        }
        throw new NotSupportedException("Versioning is not supported. ");
    }

    @Override
    public Map<Principal, Set<BasicPermissions>> getPermissions() throws VirtualFileSystemException {
        checkExist();
        final Map<Principal, Set<BasicPermissions>> copy = new HashMap<>(permissionsMap.size());
        for (Map.Entry<Principal, Set<BasicPermissions>> e : permissionsMap.entrySet()) {
            final Principal copyPrincipal = DtoFactory.getInstance().clone(e.getKey());
            copy.put(copyPrincipal, EnumSet.copyOf(e.getValue()));
        }
        return copy;
    }

    @Override
    public List<AccessControlEntry> getACL() throws VirtualFileSystemException {
        checkExist();
        final Map<Principal, Set<BasicPermissions>> permissions = getPermissions();
        final List<AccessControlEntry> acl = new ArrayList<>(permissions.size());
        for (Map.Entry<Principal, Set<BasicPermissions>> e : permissions.entrySet()) {
            final Set<BasicPermissions> basicPermissions = e.getValue();
            final Principal principal = e.getKey();
            final List<String> plainPermissions = new ArrayList<>(basicPermissions.size());
            for (BasicPermissions permission : e.getValue()) {
                plainPermissions.add(permission.value());
            }
            // principal is already copied in method getPermissions
            acl.add(DtoFactory.getInstance().createDto(AccessControlEntry.class)
                              .withPrincipal(principal).withPermissions(plainPermissions));
        }
        return acl;
    }

    public List<Property> getProperties(PropertyFilter filter) throws VirtualFileSystemException {
        checkExist();
        final List<Property> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : properties.entrySet()) {
            final String name = e.getKey();
            if (filter.accept(name)) {
                final List<String> value = e.getValue();
                final Property property = DtoFactory.getInstance().createDto(Property.class).withName(name);
                if (value != null) {
                    property.setValue(new ArrayList<>(value));
                }
                result.add(property);
            }
        }
        return result;
    }

    public VirtualFile updateProperties(List<Property> update, String lockToken) throws VirtualFileSystemException {
        checkExist();
        if (!hasPermission(BasicPermissions.UPDATE_ACL, true)) {
            throw new PermissionDeniedException(String.format("Unable update properties for '%s'. Operation not permitted. ", getPath()));
        }
        if (isFile() && !validateLockTokenIfLocked(lockToken)) {
            throw new LockException(String.format("Unable update properties of item '%s'. Item is locked. ", getPath()));
        }
        for (Property p : update) {
            String name = p.getName();
            List<String> value = p.getValue();
            if (value != null) {
                List<String> copy = new ArrayList<>(value);
                properties.put(name, copy);
            } else {
                properties.remove(name);
            }
        }
        lastModificationDate = System.currentTimeMillis();
        mountPoint.getEventService().publish(new UpdatePropertiesEvent(mountPoint.getWorkspaceId(), getPath()));
        return this;
    }

    public long getCreationDate() throws VirtualFileSystemException {
        checkExist();
        return creationDate;
    }

    public long getLastModificationDate() throws VirtualFileSystemException {
        checkExist();
        return lastModificationDate;
    }

    public void accept(VirtualFileVisitor visitor) throws VirtualFileSystemException {
        checkExist();
        visitor.visit(this);
    }

    @Override
    public LazyIterator<Pair<String, String>> countMd5Sums() throws VirtualFileSystemException {
        checkExist();
        if (isFile()) {
            return LazyIterator.emptyIterator();
        }

        final List<Pair<String, String>> hashes = new ArrayList<>();
        final int trimPathLength = getPath().length() + 1;
        final HashFunction hashFunction = Hashing.md5();
        accept(new VirtualFileVisitor() {
            @Override
            public void visit(final VirtualFile virtualFile) throws VirtualFileSystemException {
                if (virtualFile.isFile()) {
                    final InputStream stream = virtualFile.getContent().getStream();
                    try {
                        final String hexHash = ByteStreams.hash(new InputSupplier<InputStream>() {
                            @Override
                            public InputStream getInput() throws IOException {
                                return stream;
                            }
                        }, hashFunction).toString();

                        hashes.add(Pair.of(hexHash, virtualFile.getPath().substring(trimPathLength)));
                    } catch (IOException e) {
                        throw new VirtualFileSystemException(e);
                    }
                } else {
                    final LazyIterator<VirtualFile> children = virtualFile.getChildren(VirtualFileFilter.ALL);
                    while (children.hasNext()) {
                        children.next().accept(this);
                    }
                }
            }
        });
        return LazyIterator.fromList(hashes);
    }

    @Override
    public boolean exists() throws VirtualFileSystemException {
        return exists;
    }

    @Override
    public boolean isRoot() throws VirtualFileSystemException {
        checkExist();
        return parent == null;
    }


    @Override
    public LazyIterator<VirtualFile> getChildren(VirtualFileFilter filter) throws VirtualFileSystemException {
        checkExist();
        if (isFile()) {
            return LazyIterator.emptyIterator();
        }

        if (isRoot()) {
            // NOTE: We do not check read permissions when access to ROOT folder.
            if (!hasPermission(BasicPermissions.READ, false)) {
                // User has not access to ROOT folder.
                return LazyIterator.emptyIterator();
            }
        }

        List<VirtualFile> children = doGetChildren(this);
        for (Iterator<VirtualFile> i = children.iterator(); i.hasNext(); ) {
            VirtualFile virtualFile = i.next();
            if (!((MemoryVirtualFile)virtualFile).hasPermission(BasicPermissions.READ, false) || !filter.accept(virtualFile)) {
                i.remove();
            }
        }
        Collections.sort(children);
        return LazyIterator.fromList(children);
    }

    private List<VirtualFile> doGetChildren(VirtualFile folder) throws VirtualFileSystemException {
        return new ArrayList<>(((MemoryVirtualFile)folder).children.values());
    }

    @Override
    public VirtualFile getChild(String path) throws VirtualFileSystemException {
        checkExist();
        String[] elements = Path.fromString(path).elements();
        VirtualFile child = children.get(elements[0]);
        if (child != null && elements.length > 1) {
            for (int i = 1, l = elements.length; i < l && child != null; i++) {
                if (child.isFolder()) {
                    child = child.getChild(elements[i]);
                }
            }
        }
        if (child != null) {
            if (((MemoryVirtualFile)child).hasPermission(BasicPermissions.READ, false)) {
                return child;
            }
            throw new PermissionDeniedException(String.format("Unable get item '%s'. Operation not permitted. ", getPath()));
        }
        return null;
    }

    private boolean addChild(VirtualFile child) throws VirtualFileSystemException {
        checkExist();
        final String childName = child.getName();
        if (children.get(childName) == null) {
            children.put(childName, child);
            return true;
        }
        return false;
    }

    @Override
    public ContentStream getContent() throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException(String.format("Unable get content. Item '%s' is not a file. ", getPath()));
        }
        if (content == null) {
            content = new byte[0];
        }
        return new ContentStream(getName(), new ByteArrayInputStream(content), getMediaType(), content.length,
                                 new Date(lastModificationDate));
    }

    @Override
    public VirtualFile updateContent(String mediaType, InputStream content, String lockToken) throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException(String.format("Unable update content. Item '%s' is not a file. ", getPath()));
        }
        if (!hasPermission(BasicPermissions.UPDATE_ACL, true)) {
            throw new PermissionDeniedException(String.format("Unable update content of file '%s'. Operation not permitted. ", getPath()));
        }
        if (isFile() && !validateLockTokenIfLocked(lockToken)) {
            throw new LockException(String.format("Unable update content of file '%s'. File is locked. ", getPath()));
        }
        try {
            this.content = ByteStreams.toByteArray(content);
        } catch (IOException e) {
            throw new VirtualFileSystemException(String.format("Unable set content of '%s'. %s", getPath(), e.getMessage()));
        }
        properties.put("vfs:mimeType", Arrays.asList(mediaType));
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).update(this);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        lastModificationDate = System.currentTimeMillis();
        mountPoint.getEventService().publish(new UpdateContentEvent(mountPoint.getWorkspaceId(), getPath()));
        return this;
    }

    @Override
    public long getLength() throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            return 0;
        }
        return content.length;
    }

    @Override
    public String getPropertyValue(String name) throws VirtualFileSystemException {
        checkExist();
        List<Property> properties = getProperties(PropertyFilter.valueOf(name));
        if (properties.size() > 0) {
            List<String> values = properties.get(0).getValue();
            if (!(values == null || values.isEmpty())) {
                return values.get(0);
            }
        }
        return null;
    }

    @Override
    public String[] getPropertyValues(String name) throws VirtualFileSystemException {
        checkExist();
        List<Property> properties = getProperties(PropertyFilter.valueOf(name));
        if (properties.size() > 0) {
            List<String> values = properties.get(0).getValue();
            if (!(values == null || values.isEmpty())) {
                return values.toArray(new String[values.size()]);
            }
        }
        return new String[0];
    }

    @Override
    public VirtualFile copyTo(VirtualFile parent) throws VirtualFileSystemException {
        checkExist();
        ((MemoryVirtualFile)parent).checkExist();
        if (isRoot()) {
            throw new VirtualFileSystemException("Unable copy root folder. ");
        }
        if (!parent.isFolder()) {
            throw new VirtualFileSystemException("Unable create new file. Item specified as parent is not a folder. ");
        }
        if (!((MemoryVirtualFile)parent).hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(String.format("Unable copy item '%s' to %s. Operation not permitted. ",
                                                              getPath(), parent.getPath()));
        }
        VirtualFile copy = doCopy(parent);
        mountPoint.putItem((MemoryVirtualFile)copy);
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).add(parent);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), copy.getPath()));
        return copy;
    }

    private VirtualFile doCopy(VirtualFile parent) throws VirtualFileSystemException {
        VirtualFile virtualFile;
        if (isFile()) {
            virtualFile = newFile((MemoryVirtualFile)parent, name, Arrays.copyOf(content, content.length), getMediaType());
        } else {
            virtualFile = newFolder((MemoryVirtualFile)parent, name);
            LazyIterator<VirtualFile> children = getChildren(VirtualFileFilter.ALL);
            while (children.hasNext()) {
                ((MemoryVirtualFile)children.next()).doCopy(virtualFile);
            }
        }
        for (Map.Entry<String, List<String>> e : properties.entrySet()) {
            String name = e.getKey();
            List<String> value = e.getValue();
            if (value != null) {
                List<String> copy = new ArrayList<>(value);
                ((MemoryVirtualFile)virtualFile).properties.put(name, copy);
            }
        }
        if (!((MemoryVirtualFile)parent).addChild(virtualFile)) {
            throw new ItemAlreadyExistException(String.format("Item '%s' already exists. ", (parent.getPath() + '/' + name)));
        }
        return virtualFile;
    }

    @Override
    public VirtualFile moveTo(VirtualFile parent, final String lockToken) throws VirtualFileSystemException {
        checkExist();
        ((MemoryVirtualFile)parent).checkExist();
        if (isRoot()) {
            throw new VirtualFileSystemException("Unable move root folder. ");
        }
        final String myPath = getPath();
        final String newParentPath = parent.getPath();
        if (!parent.isFolder()) {
            throw new VirtualFileSystemException("Unable move item. Item specified as parent is not a folder. ");
        }
        if (!(((MemoryVirtualFile)parent).hasPermission(BasicPermissions.WRITE, true) && hasPermission(BasicPermissions.WRITE, true))) {
            throw new PermissionDeniedException(
                    String.format("Unable move item '%s' to %s. Operation not permitted. ", myPath, newParentPath));
        }

        if (isFolder()) {
            // Be sure destination folder is not child (direct or not) of moved item.
            if (newParentPath.startsWith(myPath)) {
                throw new InvalidArgumentException(
                        String.format("Unable move item %s to %s. Item may not have itself as parent. ", myPath, newParentPath));
            }
            accept(new VirtualFileVisitor() {
                @Override
                public void visit(VirtualFile virtualFile) throws VirtualFileSystemException {
                    if (virtualFile.isFolder()) {
                        for (VirtualFile childVirtualFile : doGetChildren(virtualFile)) {
                            childVirtualFile.accept(this);
                        }
                    }
                    if (!((MemoryVirtualFile)virtualFile).hasPermission(BasicPermissions.WRITE, false)) {
                        throw new PermissionDeniedException(
                                String.format("Unable move item '%s'. Operation not permitted. ", virtualFile.getPath()));
                    }
                    if (virtualFile.isFile() && virtualFile.isLocked()) {
                        throw new LockException(
                                String.format("Unable move item '%s'. Child item '%s' is locked. ", name, virtualFile.getPath()));
                    }
                }
            });
        } else {
            if (!validateLockTokenIfLocked(lockToken)) {
                throw new LockException(String.format("Unable move item %s. Item is locked. ", myPath));
            }
        }
        if (!((MemoryVirtualFile)parent).addChild(this)) {
            throw new ItemAlreadyExistException(String.format("Item '%s' already exists. ", (parent.getPath() + '/' + name)));
        }
        this.parent.children.remove(getName());
        this.parent = (MemoryVirtualFile)parent;
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(myPath);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                searcherProvider.getSearcher(mountPoint, true).add(parent);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        mountPoint.getEventService().publish(new MoveEvent(mountPoint.getWorkspaceId(), getPath(), myPath));
        return this;
    }

    @Override
    public VirtualFile rename(String newName, String newMediaType, String lockToken) throws VirtualFileSystemException {
        checkExist();
        checkName(newName);
        if (isRoot()) {
            throw new VirtualFileSystemException("Unable rename root folder. ");
        }
        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(String.format("Unable delete item '%s'. Operation not permitted. ", getPath()));
        }
        final String myPath = getPath();
        if (isFolder()) {
            accept(new VirtualFileVisitor() {
                @Override
                public void visit(VirtualFile virtualFile) throws VirtualFileSystemException {
                    if (virtualFile.isFolder()) {
                        for (VirtualFile childVirtualFile : doGetChildren(virtualFile)) {
                            childVirtualFile.accept(this);
                        }
                    }
                    if (!((MemoryVirtualFile)virtualFile).hasPermission(BasicPermissions.WRITE, false)) {
                        throw new PermissionDeniedException(
                                String.format("Unable rename item '%s'. Operation not permitted. ", virtualFile.getPath()));
                    }
                    if (virtualFile.isFile() && virtualFile.isLocked()) {
                        throw new LockException(
                                String.format("Unable rename item '%s'. Child item '%s' is locked. ", getPath(), virtualFile.getPath()));
                    }
                }
            });
        } else {
            if (!validateLockTokenIfLocked(lockToken)) {
                throw new LockException(String.format("Unable rename item '%s'. Item is locked. ", getPath()));
            }
        }

        if (parent.getChild(newName) != null) {
            throw new ItemAlreadyExistException(String.format("Item '%s' already exists. ", newName));
        }
        parent.children.remove(name);
        parent.children.put(newName, this);
        name = newName;

        if (newMediaType != null) {
            properties.put("vfs:mimeType", Arrays.asList(newMediaType));
        }
        lastModificationDate = System.currentTimeMillis();
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(myPath);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                searcherProvider.getSearcher(mountPoint, true).add(parent);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        mountPoint.getEventService().publish(new RenameEvent(mountPoint.getWorkspaceId(), getPath(), myPath));
        return this;
    }

    @Override
    public void delete(final String lockToken) throws VirtualFileSystemException {
        checkExist();
        if (isRoot()) {
            throw new VirtualFileSystemException("Unable delete root folder. ");
        }
        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(String.format("Unable delete item '%s'. Operation not permitted. ", getPath()));
        }
        final String myPath = getPath();
        final boolean isFolder = isFolder();
        if (isFolder()) {
            final List<VirtualFile> toDelete = new ArrayList<>();
            accept(new VirtualFileVisitor() {
                @Override
                public void visit(VirtualFile virtualFile) throws VirtualFileSystemException {
                    if (virtualFile.isFolder()) {
                        for (VirtualFile childVirtualFile : doGetChildren(virtualFile)) {
                            childVirtualFile.accept(this);
                        }
                    }
                    if (!((MemoryVirtualFile)virtualFile).hasPermission(BasicPermissions.WRITE, false)) {
                        throw new PermissionDeniedException(
                                String.format("Unable delete item '%s'. Operation not permitted. ", virtualFile.getPath()));
                    }

                    if (virtualFile.isFile() && virtualFile.isLocked()) {
                        throw new LockException(String.format("Unable delete item '%s'. Child item '%s' is locked. ",
                                                              getPath(), virtualFile.getPath()));
                    }
                    toDelete.add(virtualFile);
                }
            });

            for (VirtualFile virtualFile : toDelete) {
                mountPoint.deleteItem(virtualFile.getId());
                ((MemoryVirtualFile)virtualFile).exists = false;
            }

        } else {
            if (!validateLockTokenIfLocked(lockToken)) {
                throw new LockException(String.format("Unable delete item '%s'. Item is locked. ", getPath()));
            }
            mountPoint.deleteItem(getId());
        }
        parent.children.remove(name);
        exists = false;
        parent = null;
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(myPath);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        mountPoint.getEventService().publish(new DeleteEvent(mountPoint.getWorkspaceId(), myPath));
    }

    @Override
    public ContentStream zip(VirtualFileFilter filter) throws IOException, VirtualFileSystemException {
        checkExist();
        if (!isFolder()) {
            throw new VirtualFileSystemException(String.format("Unable export to zip. Item '%s' is not a folder. ", getPath()));
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ZipOutputStream zipOut = new ZipOutputStream(out);
        final LinkedList<VirtualFile> q = new LinkedList<>();
        q.add(this);
        final int rootZipPathLength = isRoot() ? 1 : (getPath().length() + 1);
        while (!q.isEmpty()) {
            final LazyIterator<VirtualFile> children = q.pop().getChildren(filter);
            while (children.hasNext()) {
                VirtualFile current = children.next();
                final String zipEntryName = current.getPath().substring(rootZipPathLength);
                if (current.isFile()) {
                    final ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zipEntry.setTime(current.getLastModificationDate());
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(((MemoryVirtualFile)current).content);
                    zipOut.closeEntry();
                } else if (current.isFolder()) {
                    final ZipEntry zipEntry = new ZipEntry(zipEntryName + '/');
                    zipEntry.setTime(0);
                    zipOut.putNextEntry(zipEntry);
                    q.add(current);
                    zipOut.closeEntry();
                }
            }
        }
        zipOut.close();
        final byte[] zipContent = out.toByteArray();
        return new ContentStream(getName() + ".zip", new ByteArrayInputStream(zipContent), "application/zip", zipContent.length,
                                 new Date());
    }

    @Override
    public void unzip(InputStream zipped, boolean overwrite) throws IOException, VirtualFileSystemException {
        checkExist();
        final ZipContent zipContent = ZipContent.newInstance(zipped);
        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(String.format("Unable import from zip to '%s'. Operation not permitted. ", getPath()));
        }

        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(zipContent.zippedData);
            // Wrap zip stream to prevent close it. We can pass stream to other method and it can read content of current
            // ZipEntry but not able to close original stream of ZIPed data.
            InputStream noCloseZip = new NotClosableInputStream(zip);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                VirtualFile current = this;
                final Path relPath = Path.fromString(zipEntry.getName());
                final String name = relPath.getName();
                if (relPath.length() > 1) {
                    // create all required parent directories
                    for (int i = 0, stop = relPath.length() - 1; i < stop; i++) {
                        MemoryVirtualFile folder = newFolder((MemoryVirtualFile)current, relPath.element(i));
                        if (((MemoryVirtualFile)current).addChild(folder)) {
                            current = folder;
                            mountPoint.putItem(folder);
                        } else {
                            current = current.getChild(relPath.element(i));
                        }
                    }
                }
                if (zipEntry.isDirectory()) {
                    if (current.getChild(name) == null) {
                        MemoryVirtualFile folder = newFolder((MemoryVirtualFile)current, name);
                        ((MemoryVirtualFile)current).addChild(folder);
                        mountPoint.putItem(folder);
                    }
                } else {
                    current.getChild(name);
                    String mediaType;
                    VirtualFile file = current.getChild(name);
                    if (file != null) {
                        if (file.isLocked()) {
                            throw new LockException(String.format("File '%s' already exists and locked. ", file.getPath()));
                        }
                        if (!((MemoryVirtualFile)file).hasPermission(BasicPermissions.WRITE, true)) {
                            throw new PermissionDeniedException(
                                    String.format("Unable update file '%s'. Operation not permitted. ", file.getPath()));
                        }
                        if (!overwrite) {
                            throw new ItemAlreadyExistException(String.format("File '%s' already exists. ", file.getPath()));
                        }
                        mediaType = file.getPropertyValue("vfs:mimeType");
                        file.updateContent(mediaType, noCloseZip, null);
                    } else {
                        mediaType = ContentTypeGuesser.guessContentType(new File(name));
                        file = newFile((MemoryVirtualFile)current, name, noCloseZip, mediaType);
                        ((MemoryVirtualFile)current).addChild(file);
                        mountPoint.putItem((MemoryVirtualFile)file);
                    }
                }
                zip.closeEntry();
            }
            SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(mountPoint, true).add(this);
                } catch (VirtualFileSystemException e) {
                    LOG.error(e.getMessage(), e);
                }
            }

        } catch (RuntimeException e) {
            throw new VirtualFileSystemException(e.getMessage(), e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public String lock(long timeout) throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException(String.format("Unable lock '%s'. Locking allowed for files only. ", getPath()));
        }

        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(String.format("Unable lock '%s'. Operation not permitted. ", getPath()));
        }
        final String lockToken = NameGenerator.generate(null, 32);
        final LockHolder lock = new LockHolder(lockToken, timeout);
        if (this.lock != null) {
            throw new LockException("File already locked. ");
        }
        this.lock = lock;
        lastModificationDate = System.currentTimeMillis();
        return lockToken;
    }

    @Override
    public VirtualFile unlock(String lockToken) throws VirtualFileSystemException {
        checkExist();
        if (!isFile()) {
            throw new VirtualFileSystemException(String.format("Unable unlock '%s'. Locking allowed for files only. ", getPath()));
        }
        if (lockToken == null) {
            throw new LockException("Null lock token. ");
        }
        final LockHolder myLock = lock;
        if (myLock == null) {
            throw new LockException("File is not locked. ");
        } else if (myLock.expired < System.currentTimeMillis()) {
            lock = null;
            throw new LockException("File is not locked. ");
        }
        if (myLock.lockToken.equals(lockToken)) {
            lock = null;
            lastModificationDate = System.currentTimeMillis();
        } else {
            throw new LockException("Unable remove lock from file. Lock token does not match. ");
        }
        lastModificationDate = System.currentTimeMillis();
        return this;
    }

    @Override
    public boolean isLocked() throws VirtualFileSystemException {
        checkExist();
        final LockHolder myLock = lock;
        if (lock != null) {
            if (myLock.expired < System.currentTimeMillis()) {
                // replace lock
                lock = null;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public VirtualFile createFile(String name, String mediaType, InputStream content) throws VirtualFileSystemException {
        checkExist();
        checkName(name);
        if (!isFolder()) {
            throw new VirtualFileSystemException("Unable create new file. Item specified as parent is not a folder. ");
        }
        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(
                    String.format("Unable create new file in '%s'. Operation not permitted. ", getPath()));
        }
        final MemoryVirtualFile newFile;
        try {
            newFile = newFile(this, name, content, mediaType);
        } catch (IOException e) {
            throw new VirtualFileSystemException(String.format("Unable set content of '%s'. ", getPath() + e.getMessage()));
        }
        if (!addChild(newFile)) {
            throw new ItemAlreadyExistException(String.format("Item with the name '%s' already exists. ", name));
        }
        mountPoint.putItem(newFile);
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).add(newFile);
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), newFile.getPath()));
        return newFile;
    }

    @Override
    public VirtualFile createFolder(String name) throws VirtualFileSystemException {
        checkExist();
        checkName(name);
        if (!isFolder()) {
            throw new VirtualFileSystemException("Unable create new folder. Item specified as parent is not a folder. ");
        }
        if (!hasPermission(BasicPermissions.WRITE, true)) {
            throw new PermissionDeniedException(
                    String.format("Unable create new folder in '%s'. Operation not permitted. ", getPath()));
        }
        MemoryVirtualFile newFolder = null;
        MemoryVirtualFile current = this;
        if (name.indexOf('/') > 0) {
            final Path internPath = Path.fromString(name);
            for (String element : internPath.elements()) {
                MemoryVirtualFile folder = newFolder(current, element);
                if (current.addChild(folder)) {
                    newFolder = folder;
                    current = folder;
                } else {
                    current = (MemoryVirtualFile)current.getChild(element);
                }
            }
            if (newFolder == null) {
                // Folder or folder hierarchy already exists.
                throw new ItemAlreadyExistException("Item already exists. ");
            }
        } else {
            newFolder = newFolder(this, name);
            if (!addChild(newFolder)) {
                throw new ItemAlreadyExistException(String.format("Item with the name '%s' already exists. ", name));
            }
        }
        mountPoint.putItem(newFolder);
        mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), newFolder.getPath()));
        return newFolder;
    }

    @Override
    public MountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public int compareTo(VirtualFile o) {
        // To get nice order of items:
        // 1. Regular folders
        // 2. Files
        if (o == null) {
            throw new NullPointerException();
        }
        try {
            if (isFolder()) {
                return o.isFolder() ? getName().compareTo(o.getName()) : -1;
            } else if (o.isFolder()) {
                return 1;
            }
            return getName().compareTo(o.getName());
        } catch (VirtualFileSystemException e) {
            // cannot continue if failed to determine item type.
            throw new VirtualFileSystemRuntimeException(e.getMessage(), e);
        }
    }

    boolean hasPermission(BasicPermissions permission, boolean checkParent) throws VirtualFileSystemException {
        checkExist();
        final VirtualFileSystemUser user = mountPoint.getUserContext().getVirtualFileSystemUser();
        VirtualFile current = this;
        while (current != null) {
            final Map<Principal, Set<BasicPermissions>> objectPermissions = current.getPermissions();
            if (!objectPermissions.isEmpty()) {
                final Principal userPrincipal =
                        DtoFactory.getInstance().createDto(Principal.class).withName(user.getUserId()).withType(Principal.Type.USER);
                Set<BasicPermissions> userPermissions = objectPermissions.get(userPrincipal);
                if (userPermissions != null) {
                    return userPermissions.contains(permission) || userPermissions.contains(BasicPermissions.ALL);
                }
                Collection<String> groups = user.getGroups();
                if (!groups.isEmpty()) {
                    for (String group : groups) {
                        final Principal groupPrincipal =
                                DtoFactory.getInstance().createDto(Principal.class).withName(group).withType(Principal.Type.GROUP);
                        userPermissions = objectPermissions.get(groupPrincipal);
                        if (userPermissions != null) {
                            return userPermissions.contains(permission) || userPermissions.contains(BasicPermissions.ALL);
                        }
                    }
                }
                final Principal anyPrincipal = DtoFactory.getInstance().createDto(Principal.class)
                                                         .withName(VirtualFileSystemInfo.ANY_PRINCIPAL).withType(Principal.Type.USER);
                userPermissions = objectPermissions.get(anyPrincipal);
                return userPermissions != null && (userPermissions.contains(permission) || userPermissions.contains(BasicPermissions.ALL));
            }
            if (checkParent) {
                current = current.getParent();
            } else {
                break;
            }
        }
        return true;
    }

    private void checkExist() throws VirtualFileSystemException {
        if (!exists) {
            throw new VirtualFileSystemException("Item already removed. ");
        }
    }

    private void checkName(String name) throws InvalidArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidArgumentException("Item's name is not set. ");
        }
    }

    private boolean validateLockTokenIfLocked(String lockToken) throws VirtualFileSystemException {
        if (!isLocked()) {
            return true;
        }
        final LockHolder myLock = lock;
        return myLock == null || myLock.lockToken.equals(lockToken);
    }

    private static class LockHolder {
        final String lockToken;
        final long   expired;

        LockHolder(String lockToken, long timeout) {
            this.lockToken = lockToken;
            this.expired = timeout > 0 ? (System.currentTimeMillis() + timeout) : Long.MAX_VALUE;
        }
    }
}
