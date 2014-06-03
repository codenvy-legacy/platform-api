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

import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * @author andrew00x
 */
public abstract class AbstractVirtualFileEntry {
    private VirtualFile virtualFile;

    public AbstractVirtualFileEntry(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    public boolean isFile() {
        try {
            return virtualFile.isFile();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public boolean isFolder() {
        try {
            return virtualFile.isFolder();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public String getName() {
        try {
            return virtualFile.getName();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public String getPath() {
        try {
            return virtualFile.getPath();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public FolderEntry getParent() {
        try {
            if (virtualFile.isRoot()) {
                return null;
            }
            return new FolderEntry(virtualFile.getParent());
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void remove() {
        try {
            virtualFile.delete(null);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public abstract AbstractVirtualFileEntry copyTo(String newParent);

    public void moveTo(String newParent) {
        try {
            final MountPoint mp = virtualFile.getMountPoint();
            virtualFile = virtualFile.moveTo(mp.getVirtualFile(newParent), null);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void rename(String newName) {
        try {
            virtualFile = virtualFile.rename(newName, null, null);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    void setVirtualFile(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }
}
