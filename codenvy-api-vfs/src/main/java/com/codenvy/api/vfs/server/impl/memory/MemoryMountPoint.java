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

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.VirtualFileVisitor;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.PermissionDeniedException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of MountPoint.
 * <p/>
 * NOTE: This implementation is not thread safe.
 *
 * @author andrew00x
 */
public class MemoryMountPoint implements MountPoint {
    private final String                       workspaceId;
    private final EventService                 eventService;
    private final SearcherProvider             searcherProvider;
    private final VirtualFileSystemUserContext userContext;
    private final Map<String, VirtualFile>     entries;
    private final VirtualFile                  root;

    public MemoryMountPoint(String workspaceId, EventService eventService, SearcherProvider searcherProvider,
                            VirtualFileSystemUserContext userContext) {
        this.workspaceId = workspaceId;
        this.eventService = eventService;
        this.searcherProvider = searcherProvider;
        this.userContext = userContext;
        entries = new HashMap<>();
        root = new MemoryVirtualFile(this);
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
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
        final Path internalPath = Path.fromString(path);
        final String[] elements = internalPath.elements();
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

    @Override
    public SearcherProvider getSearcherProvider() {
        return searcherProvider;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }

    VirtualFileSystemUserContext getUserContext() {
        return userContext;
    }
}
