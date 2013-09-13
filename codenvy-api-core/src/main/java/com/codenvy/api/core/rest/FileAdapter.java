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
package com.codenvy.api.core.rest;

import com.codenvy.api.core.util.ContentTypeGuesser;

/**
 * Wrapper for {@link java.io.File} to help represent local file to the remote user over HTTP.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class FileAdapter {
    private final java.io.File file;
    private final String       href;
    private final String       contentType;

    public FileAdapter(java.io.File file, String href, String contentType) {
        this.file = file;
        this.href = href;
        this.contentType = contentType;
    }

    public FileAdapter(java.io.File file, String href) {
        this(file, href, file.isFile() ? ContentTypeGuesser.guessContentType(file) : null);
    }

    /** @see java.io.File#exists() */
    public boolean exists() {
        return file.exists();
    }

    /** @see java.io.File#isFile() */
    public boolean isFile() {
        return file.isFile();
    }

    /** @see java.io.File#isDirectory() */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /** @see java.io.File#getName() */
    public String getName() {
        return file.getName();
    }

    /**
     * Get relative file location. Path is relative to some known base directory.
     *
     * @return relative file location
     */
    public String getHref() {
        return href;
    }

    /**
     * Get content type of file.
     *
     * @return content type of file
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get child {@code FileAdapter}.
     *
     * @param relativePath
     *         path relative to this {@code FileAdapter}
     * @return child {@code FileAdapter}
     * @throws IllegalArgumentException
     *         if this {@code FileAdapter} is not directory or if {@code relativePath} points to the higher level in filesystem hierarchy,
     *         e.g. path {@code /a/../..} is invalid
     */
    public FileAdapter getChild(String relativePath) {
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("Cannot create child of file.");
        }
        final java.io.File childFile = new java.io.File(file, relativePath);
        if (!(childFile.toPath().normalize().startsWith(file.toPath().normalize()))) {
            throw new IllegalArgumentException(String.format("Invalid relative path %s", relativePath));
        }
        return new FileAdapter(childFile, href + '/' + relativePath);
    }

    /**
     * Get wrapped {@code File}.
     *
     * @return wrapped {@code File}
     */
    public java.io.File getIoFile() {
        return file;
    }

    @Override
    public String toString() {
        return "FileAdapter{" +
               "file=" + file +
               ", href='" + href + '\'' +
               ", contentType='" + contentType + '\'' +
               '}';
    }
}
