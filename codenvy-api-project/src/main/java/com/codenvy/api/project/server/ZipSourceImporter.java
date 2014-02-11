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

import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ZipSourceImporter implements SourceImporter {

    private final VirtualFileSystemRegistry vfsRegistry;

    @Inject
    public ZipSourceImporter(VirtualFileSystemRegistry vfsRegistry) {
        this.vfsRegistry = vfsRegistry;
    }

    @Override
    public String getType() {
        return "zip";
    }

    @Override
    public void importSource(String workspace, String projectName, String location) throws IOException, VirtualFileSystemException {
        MountPoint mountPoint = vfsRegistry.getProvider(workspace).getMountPoint(true);
        VirtualFile projectFolder = mountPoint.getRoot().getChild(projectName);
        if (projectFolder == null) {
            projectFolder = mountPoint.getRoot().createFolder(projectName);
        }
        InputStream zip = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        if (zip == null) {
            zip = Files.newInputStream(Paths.get(location), StandardOpenOption.READ);
        }
        if (zip == null) {
            throw new IOException(String.format("Can't find %s", location));
        }
        try {
            projectFolder.unzip(zip, true);
        } finally {
            zip.close();
        }
    }
}
