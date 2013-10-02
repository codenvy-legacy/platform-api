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
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.Property;

import org.everrest.core.impl.ContainerResponse;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** @author <a href="mailto:vparfonov@exoplatform.com">Vitaly Parfonov</a> */
public class ImportTest extends MemoryFileSystemTest {
    private String importTestRootId;
    private byte[] zipFolder;
    private byte[] zipProject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile importTestRoot = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());
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

        bout.reset();
        zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry(".project"));
        String projectProperties = "[{\"name\":\"vfs:projectType\",\"value\":[\"java\"]}," +
                                   "{\"name\":\"vfs:mimeType\",\"value\":[\"text/vnd.ideproject+directory\"]}]";
        zipOut.write(projectProperties.getBytes());
        zipOut.putNextEntry(new ZipEntry("readme.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.close();
        zipProject = bout.toByteArray();
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

    public void testImportProject() throws Exception {
        String path = SERVICE_URI + "import/" + importTestRootId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, zipProject, null);
        assertEquals(204, response.getStatus());
        VirtualFile parent = mountPoint.getVirtualFileById(importTestRootId);
        List<Property> properties = parent.getProperties(PropertyFilter.ALL_FILTER);
        for (Property property : properties) {
            if ("vfs:projectType".equals(property.getName())) {
                assertEquals("java", property.getValue().get(0));
            } else if ("vfs:mimeType".equals(property.getName())) {
                assertEquals("text/vnd.ideproject+directory", property.getValue().get(0));
            }
        }
        assertEquals(1, parent.getChildren(VirtualFileFilter.ALL).size()); // file .project must be store as project properties not like a file
        VirtualFile readme = parent.getChild("readme.txt");
        assertNotNull(readme);
        checkFileContext(DEFAULT_CONTENT, "text/plain", readme);
    }
}
