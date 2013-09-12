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
import com.codenvy.api.vfs.shared.ExitCodes;
import com.codenvy.api.vfs.shared.Property;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ExportTest extends MemoryFileSystemTest {
    private String exportProjectId;

    private Set<String> expectedExportTestRootZipItems = new HashSet<>();
    private Set<String> expectedExportProjectZipItems  = new HashSet<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile exportTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

//      Create in exportTestFolder folder next files and folders:
//      ----------------------------
//         folder1/
//         folder2/
//         folder3/
//         folder1/file1.txt
//         folder1/folder12/
//         folder2/file2.txt
//         folder2/folder22/
//         folder3/file3.txt
//         folder3/folder32/
//         folder1/folder12/file12.txt
//         folder2/folder22/file22.txt
//         folder3/folder32/file32.txt
//      ----------------------------

        VirtualFile folder1 = exportTestProject.createFolder("folder1");
        VirtualFile folder2 = exportTestProject.createFolder("folder2");
        VirtualFile folder3 = exportTestProject.createFolder("folder3");

        VirtualFile file1 = folder1.createFile("file1.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        VirtualFile file2 = folder2.createFile("file2.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        VirtualFile file3 = folder3.createFile("file3.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));

        VirtualFile folder12 = folder1.createFolder("folder12");
        VirtualFile folder22 = folder2.createFolder("folder22");
        VirtualFile folder32 = folder3.createFolder("folder32");

        VirtualFile file12 = folder12.createFile("file12.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        VirtualFile file22 = folder22.createFile("file22.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        VirtualFile file32 = folder32.createFile("file32.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));

        expectedExportProjectZipItems.add("folder1/");
        expectedExportProjectZipItems.add("folder2/");
        expectedExportProjectZipItems.add("folder3/");
        expectedExportProjectZipItems.add("folder1/file1.txt");
        expectedExportProjectZipItems.add("folder1/folder12/");
        expectedExportProjectZipItems.add("folder2/file2.txt");
        expectedExportProjectZipItems.add("folder2/folder22/");
        expectedExportProjectZipItems.add("folder3/file3.txt");
        expectedExportProjectZipItems.add("folder3/folder32/");
        expectedExportProjectZipItems.add("folder1/folder12/file12.txt");
        expectedExportProjectZipItems.add("folder2/folder22/file22.txt");
        expectedExportProjectZipItems.add("folder3/folder32/file32.txt");
        expectedExportProjectZipItems.add(".project");

        exportProjectId = exportTestProject.getId();

        expectedExportTestRootZipItems.add(exportTestProject.getName() + '/');
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder1/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder2/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder3/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder1/file1.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder1/folder12/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder2/file2.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder2/folder22/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder3/file3.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder3/folder32/");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder1/folder12/file12.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder2/folder22/file22.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/folder3/folder32/file32.txt");
        expectedExportTestRootZipItems.add(exportTestProject.getName() + "/.project");
    }

    public void testExportProject() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "export/" + exportProjectId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        assertEquals("application/zip", writer.getHeaders().getFirst("Content-Type"));
        checkZipItems(expectedExportProjectZipItems, new ZipInputStream(new ByteArrayInputStream(writer.getBody())));
    }

    public void testExportRootFolder() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "export/" + mountPoint.getRoot().getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        assertEquals("application/zip", writer.getHeaders().getFirst("Content-Type"));
        checkZipItems(expectedExportTestRootZipItems, new ZipInputStream(new ByteArrayInputStream(writer.getBody())));
    }

    public void testExportFile() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(exportProjectId)
                                     .createFile("export_test_file.txt", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "export/" + file.getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.INVALID_ARGUMENT, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
    }

    private void checkZipItems(Set<String> expected, ZipInputStream zip) throws Exception {
        ZipEntry zipEntry;
        while ((zipEntry = zip.getNextEntry()) != null) {
            String name = zipEntry.getName();
         /*if (!zipEntry.isDirectory())
         {
            byte[] buf = new byte[1024];
            int i = zip.read(buf);
            System.out.println(new String(buf, 0, i));
         }*/
            zip.closeEntry();
            assertTrue("Not found " + name + " entry in zip. ", expected.remove(name));
        }
        assertTrue(expected.isEmpty());
    }
}
