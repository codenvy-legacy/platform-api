/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
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

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ZipProjectImporter implements ProjectImporter {
    @Override
    public String getId() {
        return "zip";
    }

    @Override
    public String getDescription() {
        return "Add possibility to import project from zip archive located by public URL";
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location) throws IOException {
        URL url;
        if (location.startsWith("http://") || location.startsWith("https://")) {
            url = new URL(location);
        } else {
            url = Thread.currentThread().getContextClassLoader().getResource(location);
            if (url == null) {
                final java.io.File file = new java.io.File(location);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
        }
        if (url == null) {
            throw new IOException(String.format("Can't find %s", location));
        }
        try (InputStream zip = url.openStream()) {
            baseFolder.getVirtualFile().unzip(zip, true);
        } catch (VirtualFileSystemException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
