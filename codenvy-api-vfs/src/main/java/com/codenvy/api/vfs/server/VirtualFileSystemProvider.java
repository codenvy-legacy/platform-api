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
