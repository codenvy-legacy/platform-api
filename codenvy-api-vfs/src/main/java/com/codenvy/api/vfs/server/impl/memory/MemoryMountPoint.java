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
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.VirtualFileVisitor;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.PermissionDeniedException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.server.util.PathUtil;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of MountPoint.
 * <p/>
 * NOTE: This implementation is not thread safe.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class MemoryMountPoint implements MountPoint {
    private final Map<String, VirtualFile>     entries;
    private final VirtualFile                  root;
    private final SearcherProvider             searcherProvider;
    private final VirtualFileSystemUserContext userContext;

    MemoryMountPoint(SearcherProvider searcherProvider, VirtualFileSystemUserContext userContext) {
        this.searcherProvider = searcherProvider;
        this.userContext = userContext;
        entries = new HashMap<>();
        root = new MemoryVirtualFile(this);
    }

    public VirtualFile getRoot() {
        return root;
    }

    @Override
    public VirtualFile getVirtualFileById(String id) throws VirtualFileSystemException {
        if (id.equals(root.getId())) {
            return getRoot();
        }
        final VirtualFile virtualFile = entries.get(id);
        if (virtualFile == null) {
            throw new ItemNotFoundException(String.format("Object '%s' does not exists. ", id));
        }
        if (!((MemoryVirtualFile)virtualFile).hasPermission(VirtualFileSystemInfo.BasicPermissions.READ, true)) {
            throw new PermissionDeniedException(String.format("Unable get item '%s'. Operation not permitted. ", id));
        }
        return virtualFile;
    }

    @Override
    public VirtualFile getVirtualFile(String path) throws VirtualFileSystemException {
        if (path == null) {
            throw new IllegalArgumentException("Item path may not be null. ");
        }
        if ("/".equals(path) || path.isEmpty()) {
            return getRoot();
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        VirtualFile virtualFile = getRoot();
        String[] elements = PathUtil.parse(path);
        for (int i = 0, length = elements.length; virtualFile != null && i < length; i++) {
            String name = elements[i];
            if (virtualFile.isFolder()) {
                virtualFile = virtualFile.getChild(name);
            }
        }
        if (virtualFile == null) {
            throw new ItemNotFoundException(String.format("Object '%s' does not exists. ", path));
        }

        return virtualFile;
    }

    @Override
    public void reset() {
        entries.clear();
    }

    void putItem(MemoryVirtualFile item) throws VirtualFileSystemException {
        if (item.isFolder()) {
            final Map<String, VirtualFile> flatten = new HashMap<>();
            item.accept(new VirtualFileVisitor() {
                @Override
                public void visit(VirtualFile virtualFile) throws VirtualFileSystemException {
                    if (virtualFile.isFolder()) {
                        final LazyIterator<VirtualFile> children = virtualFile.getChildren(VirtualFileFilter.ALL);
                        while (children.hasNext()) {
                            children.next().accept(this);
                        }
                    }
                    flatten.put(virtualFile.getId(), virtualFile);
                }
            });
            entries.putAll(flatten);
        } else {
            entries.put(item.getId(), item);
        }
    }

    void deleteItem(String id) {
        entries.remove(id);
    }

    SearcherProvider getSearcherProvider() {
        return searcherProvider;
    }

    VirtualFileSystemUserContext getUserContext() {
        return userContext;
    }
}
