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

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Produce instance of VirtualFileSystem.
 *
 * @author andrew00x
 */
public abstract class VirtualFileSystemProvider {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualFileSystemProvider.class);
    private final String workspaceId;

    public VirtualFileSystemProvider(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    /**
     * Create instance of VirtualFileSystem.
     *
     * @param baseUri
     *         base URI. Virtual filesystem uses it to provide correct links for set of operation with its items
     * @return instance of VirtualFileSystem
     * @throws VirtualFileSystemException
     */
    public abstract VirtualFileSystem newInstance(URI baseUri) throws VirtualFileSystemException;

    /**
     * Get mount point of virtual filesystem.
     *
     * @param create
     *         <code>true</code> to create MountPoint if necessary; <code>false</code> to return <code>null</code> if MountPoint is not
     *         initialized yet
     * @return <code>MountPoint</code> or <code>null</code> if <code>create</code> is <code>false</code> and the MountPoint is not
     *         initialized yet
     * @throws VirtualFileSystemException
     *         if an error occurs
     */
    public abstract MountPoint getMountPoint(boolean create) throws VirtualFileSystemException;

    /**
     * Close this provider. Call this method after unregister provider from VirtualFileSystemRegistry. Typically this
     * method called from {@link VirtualFileSystemRegistry#unregisterProvider(String)}. Usually should not call it
     * directly.
     * <p/>
     * Sub-classes should invoke {@code super.close} at the end of this method.
     */
    public void close() {
        try {
            final MountPoint mountPoint = getMountPoint(false);
            if (mountPoint != null) {
                mountPoint.reset();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
