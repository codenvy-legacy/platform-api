/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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

import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectFolder extends ProjectEntry {

    public ProjectFolder(VirtualFile virtualFile) {
        super(virtualFile);
    }

    public ProjectFolder copy(String destPath) {
        try {
            final Path internalVfsPath = Path.fromString(destPath);
            if (internalVfsPath.length() <= 1) {
                throw new IllegalArgumentException(String.format("Invalid path %s. Can't create folder outside of project.", destPath));
            }
            final VirtualFile vf = getVirtualFile();
            final String parentPath = internalVfsPath.getParent().toString();
            return new ProjectFolder(vf.copyTo(vf.getMountPoint().getVirtualFile(parentPath)));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public List<ProjectEntry> getChildren() {
        try {
            final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(VirtualFileFilter.ALL);
            final List<ProjectEntry> children = new ArrayList<>();
            while (vfChildren.hasNext()) {
                final VirtualFile vf = vfChildren.next();
                if (vf.isFile()) {
                    children.add(new ProjectFile(vf));
                } else {
                    children.add(new ProjectFolder(vf));
                }
            }
            return children;
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }
}
