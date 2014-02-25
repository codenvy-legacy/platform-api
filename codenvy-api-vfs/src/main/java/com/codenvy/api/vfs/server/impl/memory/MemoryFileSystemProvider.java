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

import com.codenvy.api.vfs.server.search.LuceneSearcherProvider;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.search.Searcher;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemProvider;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.EventListenerList;

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

    private MemoryMountPoint memoryMountPoint;

    public MemoryFileSystemProvider(String workspaceId) {
        super(workspaceId);
    }

    public MemoryFileSystemProvider(String workspaceId, MemoryMountPoint memoryMountPoint) {
        super(workspaceId);
        this.memoryMountPoint = memoryMountPoint;
    }

    @Override
    public VirtualFileSystem newInstance(URI baseUri, EventListenerList listeners) throws VirtualFileSystemException {
        final MemoryMountPoint memoryMountPoint = (MemoryMountPoint)getMountPoint(true);
        return new MemoryFileSystem(
                baseUri == null ? URI.create("") : baseUri,
                listeners,
                getWorkspaceId(),
                memoryMountPoint.getUserContext(),
                memoryMountPoint,
                memoryMountPoint.getSearcherProvider());
    }

    @Override
    public MountPoint getMountPoint(boolean create) throws VirtualFileSystemException {
        if (memoryMountPoint == null && create) {
            memoryMountPoint = new MemoryMountPoint(new SimpleLuceneSearcherProvider(), VirtualFileSystemUserContext.newInstance());
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
