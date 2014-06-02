/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.vfs.server;

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.SearcherProvider;

/**
 * Attaches any point on backend filesystem some VirtualFile (root folder). Only children of root folder may be accessible through this
 * API.
 *
 * @author andrew00x
 */
public interface MountPoint {

    /** Get id of workspace to which this mount point associated to.*/
    String getWorkspaceId();

    /**
     * Get root folder of virtual file system. Any files in higher level than root are not accessible through virtual file system API.
     *
     * @return root folder of virtual file system
     */
    VirtualFile getRoot();

    /**
     * Get VirtualFile by <code>path</code>.
     *
     * @param path
     *         path of virtual file
     * @return VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.ItemNotFoundException
     *         if <code>path</code> does not exist
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user which perform operation has no permissions to do it
     * @throws VirtualFileSystemException
     *         if any other errors occur
     */
    VirtualFile getVirtualFile(String path) throws VirtualFileSystemException;

    /**
     * Get VirtualFile by <code>id</code>.
     *
     * @param id
     *         id of virtual file
     * @return VirtualFile
     * @throws com.codenvy.api.vfs.server.exceptions.ItemNotFoundException
     *         if <code>id</code> does not exist
     * @throws com.codenvy.api.vfs.server.exceptions.PermissionDeniedException
     *         if user which perform operation has no permissions to do it
     * @throws VirtualFileSystemException
     *         if any other errors occur
     */
    VirtualFile getVirtualFileById(String id) throws VirtualFileSystemException;

    /** Get searcher provider associated with this MountPoint. Method may return {@code null} if implementation doesn't support searching. */
    SearcherProvider getSearcherProvider();

    /** Get EventService. EventService may be used for propagation events about updates of any items associated with this MountPoint. */
    EventService getEventService();

    /** Call after unmount this MountPoint, e.g. clear caches */
    void reset();
}
