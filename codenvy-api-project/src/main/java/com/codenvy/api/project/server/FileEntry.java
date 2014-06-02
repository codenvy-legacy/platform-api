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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author andrew00x
 */
public class FileEntry extends AbstractVirtualFileEntry {

    public FileEntry(VirtualFile virtualFile) {
        super(virtualFile);
    }

    public FileEntry copyTo(String newParent) {
        if (Path.fromString(newParent).isRoot()) {
            throw new ProjectStructureConstraintException(
                    String.format("Invalid path %s. Can't create file outside of project.", newParent));
        }
        try {
            final VirtualFile vf = getVirtualFile();
            final MountPoint mp = vf.getMountPoint();
            return new FileEntry(vf.copyTo(mp.getVirtualFile(newParent)));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void moveTo(String newParent) {
        if (Path.fromString(newParent).isRoot()) {
            throw new ProjectStructureConstraintException(
                    String.format("Invalid path %s. Can't move this item outside of project.", newParent));
        }
        super.moveTo(newParent);
    }

    public String getMediaType() {
        try {
            return getVirtualFile().getMediaType();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void setMediaType(String mediaType) {
        try {
            getVirtualFile().setMediaType(mediaType);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public InputStream getInputStream() throws IOException {
        try {
            return getVirtualFile().getContent().getStream();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public byte[] contentAsBytes() throws IOException {
        final ContentStream contentStream;
        try {
            contentStream = getVirtualFile().getContent();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
        final int contentLength = (int)contentStream.getLength();
        if (contentLength == 0) {
            return new byte[0];
        }
        try (InputStream stream = contentStream.getStream()) {
            if (contentLength < 0) {
                return ByteStreams.toByteArray(stream);
            }
            final byte[] b = new byte[contentLength];
            ByteStreams.readFully(stream, b);
            return b;
        }
    }

    public void updateContent(byte[] content, String mediaType) throws IOException {
        updateContent(new ByteArrayInputStream(content), mediaType);
    }

    public void updateContent(byte[] content) throws IOException {
        updateContent(content, getMediaType());
    }

    public void updateContent(InputStream content) throws IOException {
        updateContent(content, getMediaType());
    }

    public void updateContent(InputStream content, String mediaType) throws IOException {
        try {
            getVirtualFile().updateContent(mediaType, content, null);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void rename(String newName, String newMediaType) {
        try {
            final VirtualFile rVf = getVirtualFile().rename(newName, newMediaType, null);
            setVirtualFile(rVf);
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }
}
