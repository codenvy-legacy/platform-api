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

import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.Path;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author andrew00x
 */
public class ProjectFile extends ProjectEntry {

    public ProjectFile(VirtualFile virtualFile) {
        super(virtualFile);
    }

    public ProjectFile copy(String destPath) {
        try {
            final Path internalVfsPath = Path.fromString(destPath);
            if (internalVfsPath.length() <= 1) {
                throw new IllegalArgumentException(String.format("Invalid path %s. Can't create file outside of project.", destPath));
            }
            final VirtualFile vf = getVirtualFile();
            final String parentPath = internalVfsPath.getParent().toString();
            return new ProjectFile(vf.copyTo(vf.getMountPoint().getVirtualFile(parentPath)));
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
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

    public InputStream getInputStream() {
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
                final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                final byte[] buf = new byte[1024];
                int off;
                while ((off = stream.read(buf)) != -1) {
                    bout.write(buf, 0, off);
                }
                return bout.toByteArray();
            }
            final byte[] b = new byte[contentLength];
            int point, off = 0;
            while ((point = stream.read(b, off, contentLength - off)) > 0) {
                off += point;
            }
            return b;
        }
    }

    public void updateContent(byte[] content, String mediaType) throws IOException {
        try (OutputStream outputStream = openOutputStream()) {
            outputStream.write(content);
            outputStream.flush();
        }
        if (mediaType != null) {
            setMediaType(mediaType);
        }
    }

    public void updateContent(byte[] content) throws IOException {
        updateContent(content, null);
    }

    public OutputStream openOutputStream() throws IOException {
        try {
            return getVirtualFile().openOutputStream();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }
}
