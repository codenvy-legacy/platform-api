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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public void importSources(FolderEntry baseFolder, String location) throws IOException {
        InputStream zip = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        if (zip == null) {
            final Path path = Paths.get(location);
            if (Files.isReadable(path)) {
                zip = Files.newInputStream(path);
            }
        }
        if (zip == null) {
            throw new IOException(String.format("Can't find %s", location));
        }
        try {
            baseFolder.getVirtualFile().unzip(zip, true);
        } catch (VirtualFileSystemException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            zip.close();
        }
    }
}
