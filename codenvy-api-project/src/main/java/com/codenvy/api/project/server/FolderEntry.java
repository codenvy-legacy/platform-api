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

import com.codenvy.api.vfs.server.LazyIterator;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew00x
 */
public class FolderEntry extends AbstractVirtualFileEntry {
    private static final VirtualFileFilter FOLDER_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) throws VirtualFileSystemException {
            return file.isFolder();
        }
    };

    private static final VirtualFileFilter FILES_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) throws VirtualFileSystemException {
            return file.isFile();
        }
    };

    public FolderEntry(VirtualFile virtualFile) {
        super(virtualFile);
    }

    public FolderEntry copyTo(String newParent) {
        try {
            final VirtualFile vf = getVirtualFile();
            final MountPoint mp = vf.getMountPoint();
            return new FolderEntry(vf.copyTo(mp.getVirtualFile(newParent)));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public AbstractVirtualFileEntry getChild(String path) {
        try {
            final VirtualFile child = getVirtualFile().getChild(path);
            if (child == null) {
                return null;
            }
            if (child.isFile()) {
                return new FileEntry(child);
            }
            return new FolderEntry(child);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public List<AbstractVirtualFileEntry> getChildren() {
        return getChildren(VirtualFileFilter.ALL);
    }

    public List<FileEntry> getChildFiles() {
        try {
            final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(FILES_FILTER);
            final List<FileEntry> children = new ArrayList<>();
            while (vfChildren.hasNext()) {
                children.add(new FileEntry(vfChildren.next()));
            }
            return children;
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public List<FolderEntry> getChildFolders() {
        try {
            final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(FOLDER_FILTER);
            final List<FolderEntry> children = new ArrayList<>();
            while (vfChildren.hasNext()) {
                children.add(new FolderEntry(vfChildren.next()));
            }
            return children;
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    List<AbstractVirtualFileEntry> getChildren(VirtualFileFilter filter) {
        try {
            final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(filter);
            final List<AbstractVirtualFileEntry> children = new ArrayList<>();
            while (vfChildren.hasNext()) {
                final VirtualFile vf = vfChildren.next();
                if (vf.isFile()) {
                    children.add(new FileEntry(vf));
                } else {
                    children.add(new FolderEntry(vf));
                }
            }
            return children;
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public FileEntry createFile(String name, byte[] content, String mediaType) {
        if (isRoot(getVirtualFile())) {
            throw new ProjectStructureConstraintException("Can't create file in root folder.");
        }
        return createFile(name, content == null ? null : new ByteArrayInputStream(content), mediaType);
    }

    public FileEntry createFile(String name, InputStream content, String mediaType) {
        if (isRoot(getVirtualFile())) {
            throw new ProjectStructureConstraintException("Can't create file in root folder.");
        }
        try {
            return new FileEntry(getVirtualFile().createFile(name, mediaType, content));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public FolderEntry createFolder(String name) {
        try {
            return new FolderEntry(getVirtualFile().createFolder(name));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public boolean isProjectFolder() {
        final AbstractVirtualFileEntry projectFile = getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        return projectFile != null && projectFile.isFile();
    }

    private boolean isRoot(VirtualFile virtualFile) {
        try {
            return virtualFile.isRoot();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }
}
