package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * @author andrew00x
 */
public abstract class ProjectEntry {
    private VirtualFile virtualFile;

    public ProjectEntry(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
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

    public ProjectFolder getParentFolder() {
        try {
            final VirtualFile vfParent = virtualFile.getParent();
            final VirtualFile projectFile = vfParent.getChild(".codenvy/project");
            if (projectFile != null && projectFile.isFile()) {
                return new ProjectFolder(vfParent);
            }
            // File has not parent folder. It locates directly in the root of project.
            // NOTE: it doesn't mean file is locating in root folder of virtual filesystem.
            return null;
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public Project getProject() {
        try {
            final String projectName = virtualFile.getVirtualFilePath().element(0);
            return new Project(virtualFile.getMountPoint().getRoot().getChild(projectName));
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

    public void move(String path) {
        try {
            final Path internalVfsPath = Path.fromString(path);
            if (internalVfsPath.length() <= 1) {
                throw new IllegalArgumentException(String.format("Invalid path %s. Can't move this item outside of project.", path));
            }
            final MountPoint mountPoint = virtualFile.getMountPoint();
            final VirtualFile newParent = mountPoint.getVirtualFile(internalVfsPath.getParent().toString());
            virtualFile = virtualFile.moveTo(newParent, null);
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
}
