/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
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
