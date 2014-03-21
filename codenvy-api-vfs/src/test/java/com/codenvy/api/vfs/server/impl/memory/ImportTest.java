/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.vfs.server.VirtualFile;

import org.everrest.core.impl.ContainerResponse;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** @author andrew00x */
public class ImportTest extends MemoryFileSystemTest {
    private String importTestRootId;
    private byte[] zipFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile importTestRoot = mountPoint.getRoot().createFolder(name);
        importTestRootId = importTestRoot.getId();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder2/"));
        zipOut.putNextEntry(new ZipEntry("folder3/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.putNextEntry(new ZipEntry("folder2/file2.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.putNextEntry(new ZipEntry("folder3/file3.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.close();
        zipFolder = bout.toByteArray();
    }

    public void testImportFolder() throws Exception {
        String path = SERVICE_URI + "import/" + importTestRootId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, zipFolder, null);
        assertEquals(204, response.getStatus());
        VirtualFile parent = mountPoint.getVirtualFileById(importTestRootId);
        VirtualFile folder1 = parent.getChild("folder1");
        assertNotNull(folder1);
        VirtualFile folder2 = parent.getChild("folder2");
        assertNotNull(folder2);
        VirtualFile folder3 = parent.getChild("folder3");
        assertNotNull(folder3);
        VirtualFile file1 = folder1.getChild("file1.txt");
        assertNotNull(file1);
        checkFileContext(DEFAULT_CONTENT, "text/plain", file1);
        VirtualFile file2 = folder2.getChild("file2.txt");
        assertNotNull(file2);
        checkFileContext(DEFAULT_CONTENT, "text/plain", file2);
        VirtualFile file3 = folder3.getChild("file3.txt");
        assertNotNull(file3);
        checkFileContext(DEFAULT_CONTENT, "text/plain", file3);
    }
}
