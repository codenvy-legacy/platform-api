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

import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.sun.nio.zipfs.ZipPath;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ZipSourceImporterExtension implements SourceImporterExtension {


    private VirtualFileSystemRegistry vfsRegistry;

    @Inject
    public ZipSourceImporterExtension(VirtualFileSystemRegistry vfsRegistry) {
        this.vfsRegistry = vfsRegistry;
    }

    @Override
    public String getType() {
        return "zip";
    }

    @Override
    public void importSource(String workspace, String projectName, ImportSourceDescriptor importSourceDescriptor)
            throws IOException, VirtualFileSystemException {
        MountPoint mountPoint = vfsRegistry.getProvider(workspace).getMountPoint(true);
        VirtualFile projectFolder = mountPoint.getRoot().getChild(projectName);
        String location = importSourceDescriptor.getLocation();
        if (projectFolder == null)
            projectFolder = mountPoint.getRoot().createFolder(projectName);
        InputStream templateStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        if (templateStream == null) {
            templateStream = Files.newInputStream(Paths.get(location), StandardOpenOption.READ);
        }
        if (templateStream == null)
            throw new InvalidArgumentException("Can't find " + location);
        projectFolder.unzip(templateStream, true);
    }
}
