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
package com.codenvy.api.project.server;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;

/**
 * Wrapper for VirtualFile.
 *
 * @author andrew00x
 */
public abstract class VirtualFileEntry {
    private final String      workspace;
    private       VirtualFile virtualFile;

    public VirtualFileEntry(String workspace, VirtualFile virtualFile) {
        this.workspace = workspace;
        this.virtualFile = virtualFile;
    }

    /** Gets id of workspace which file belongs to. */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Tests whether this item is a regular file.
     *
     * @see com.codenvy.api.vfs.server.VirtualFile#isFile()
     */
    public boolean isFile() {
        return virtualFile.isFile();
    }

    /**
     * Tests whether this item is a folder.
     *
     * @see com.codenvy.api.vfs.server.VirtualFile#isFolder()
     */
    public boolean isFolder() {
        return virtualFile.isFolder();
    }

    /**
     * Gets name.
     *
     * @see com.codenvy.api.vfs.server.VirtualFile#getName()
     */
    public String getName() {
        return virtualFile.getName();
    }

    /**
     * Gets path.
     *
     * @see com.codenvy.api.vfs.server.VirtualFile#getPath()
     */
    public String getPath() {
        return virtualFile.getPath();
    }

    /**
     * Gets parent folder. If this item is root folder this method always returns {@code null}.
     *
     * @see com.codenvy.api.vfs.server.VirtualFile#getParent()
     * @see com.codenvy.api.vfs.server.VirtualFile#isRoot()
     */
    public FolderEntry getParent() {
        if (virtualFile.isRoot()) {
            return null;
        }
        return new FolderEntry(workspace, virtualFile.getParent());
    }

    /**
     * Deletes this item.
     *
     * @throws ForbiddenException
     *         if delete operation is forbidden
     * @throws ServerException
     *         if other error occurs
     */
    public void remove() throws ServerException, ForbiddenException {
        virtualFile.delete(null);
    }

    /**
     * Creates copy of this item in new parent.
     *
     * @param newParent
     *         path of new parent
     * @throws NotFoundException
     *         if {@code newParent} doesn't exist
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if copy operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     */
    public abstract VirtualFileEntry copyTo(String newParent)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException;

    /**
     * Moves this item to the new parent.
     *
     * @param newParent
     *         path of new parent
     * @throws NotFoundException
     *         if {@code newParent} doesn't exist
     * @throws ForbiddenException
     *         if move operation is forbidden
     * @throws ConflictException
     *         if move operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     */
    public void moveTo(String newParent) throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final MountPoint mp = virtualFile.getMountPoint();
        virtualFile = virtualFile.moveTo(mp.getVirtualFile(newParent), null);
    }

    /**
     * Renames this item.
     *
     * @param newName
     *         new name
     * @throws ForbiddenException
     *         if rename operation is forbidden
     * @throws ConflictException
     *         if rename operation causes name conflict
     * @throws ServerException
     *         if other error occurs
     */
    public void rename(String newName) throws ConflictException, ForbiddenException, ServerException {
        virtualFile = virtualFile.rename(newName, null, null);
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    void setVirtualFile(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }
}
