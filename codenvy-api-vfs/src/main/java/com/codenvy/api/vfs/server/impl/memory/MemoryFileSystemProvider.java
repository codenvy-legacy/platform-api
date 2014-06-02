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

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemProvider;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.LuceneSearcherProvider;
import com.codenvy.api.vfs.server.search.Searcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/** @author andrew00x */
public class MemoryFileSystemProvider extends VirtualFileSystemProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryFileSystemProvider.class);

    public static class SimpleLuceneSearcherProvider extends LuceneSearcherProvider {
        MemoryLuceneSearcher searcher;

        @Override
        public Searcher getSearcher(MountPoint mountPoint, boolean create) throws VirtualFileSystemException {
            if (searcher == null) {
                searcher = new MemoryLuceneSearcher(getIndexedMediaTypes());
                searcher.init(mountPoint);
            }
            return searcher;
        }
    }

    private final String                       workspaceId;
    private final EventService                 eventService;
    private final VirtualFileSystemUserContext userContext;
    private final SimpleLuceneSearcherProvider searcherProvider;

    private MemoryMountPoint memoryMountPoint;

    public MemoryFileSystemProvider(String workspaceId, EventService eventService, VirtualFileSystemUserContext userContext) {
        super(workspaceId);
        this.workspaceId = workspaceId;
        this.eventService = eventService;
        this.userContext = userContext;
        searcherProvider = new SimpleLuceneSearcherProvider();
    }

    public MemoryFileSystemProvider(String workspaceId, EventService eventService) {
        this(workspaceId, eventService, VirtualFileSystemUserContext.newInstance());
    }

    @Override
    public VirtualFileSystem newInstance(URI baseUri) throws VirtualFileSystemException {
        final MemoryMountPoint memoryMountPoint = (MemoryMountPoint)getMountPoint(true);
        return new MemoryFileSystem(
                baseUri == null ? URI.create("") : baseUri,
                workspaceId,
                userContext,
                memoryMountPoint,
                searcherProvider);
    }

    @Override
    public MountPoint getMountPoint(boolean create) throws VirtualFileSystemException {
        if (memoryMountPoint == null && create) {
            memoryMountPoint = new MemoryMountPoint(workspaceId, eventService, searcherProvider, userContext);
        }
        return memoryMountPoint;
    }

    @Override
    public void close() {
        try {
            final MemoryMountPoint memoryMountPoint = (MemoryMountPoint)getMountPoint(false);
            if (memoryMountPoint != null) {
                final Searcher searcher = memoryMountPoint.getSearcherProvider().getSearcher(memoryMountPoint, false);
                if (searcher != null) {
                    searcher.close();
                }
            }
        } catch (VirtualFileSystemException e) {
            LOG.error(e.getMessage(), e);
        }
        super.close();
    }
}
