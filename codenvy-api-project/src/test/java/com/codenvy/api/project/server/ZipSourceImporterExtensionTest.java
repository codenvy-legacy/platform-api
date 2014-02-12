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
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.user.User;
import com.codenvy.commons.user.UserImpl;
import com.codenvy.dto.server.DtoFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Vitaly Parfonov
 */
public class ZipSourceImporterExtensionTest {

    private static final String MY_WORKSPACE_ID = "ws";
    private SourceImporterExtension   importer;
    private MemoryFileSystemProvider  fileSystemProvider;
    protected static VirtualFileSystemRegistry virtualFileSystemRegistry = new VirtualFileSystemRegistry();

    @Before
    public void setUp() throws VirtualFileSystemException {
        fileSystemProvider = new MemoryFileSystemProvider(MY_WORKSPACE_ID);
        virtualFileSystemRegistry.registerProvider(MY_WORKSPACE_ID, fileSystemProvider);
        importer = new ZipSourceImporterExtension(virtualFileSystemRegistry);
        User user = new UserImpl("john", null, Arrays.asList("developer"));
        EnvironmentContext.getCurrent().setUser(user);
    }


    @Test
    public void testImportSource() throws Exception {
        ImportSourceDescriptor zip = DtoFactory.getInstance().createDto(ImportSourceDescriptor.class).withType("zip").withLocation(
                "Simple_spring.zip");
        String name = NameGenerator.generate("", 10);
        importer.importSource(MY_WORKSPACE_ID, name, zip);
        VirtualFile project = fileSystemProvider.getMountPoint(true).getVirtualFile(name);
        Assert.assertTrue(project.isProject());
        Assert.assertNotNull(project.getChild("pom.xml"));
    }
}
