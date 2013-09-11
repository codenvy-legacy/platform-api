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
import com.codenvy.api.vfs.shared.Property;
import com.codenvy.api.vfs.shared.PropertyImpl;

import org.everrest.core.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class UpdateTest extends MemoryFileSystemTest {
    private String fileId;
    private String folderId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile updateTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());
        VirtualFile file =
                updateTestProject.createFile("UpdateTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
        VirtualFile folder = updateTestProject.createFolder("UpdateTest_FOLDER");
        folderId = folder.getId();
    }

    public void testUpdatePropertiesFile() throws Exception {
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        doUpdate(fileId, properties);
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        assertEquals("MyValue", file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesLockedFile() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        String lockToken = file.lock(0);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        String path = SERVICE_URI + "item/" + fileId + "?lockToken=" + lockToken;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals(200, response.getStatus());
        file = mountPoint.getVirtualFileById(fileId);
        assertEquals("MyValue", file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesLockedFileNoLockToken() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.lock(0);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        String path = SERVICE_URI + "item/" + fileId;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals(423, response.getStatus());
        file = mountPoint.getVirtualFileById(fileId);
        assertEquals(null, file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesAndChangeFolderType() throws Exception {
        VirtualFile folder = mountPoint.getVirtualFileById(folderId);
        assertFalse(folder.isProject());
        String properties = "[{\"name\":\"vfs:mimeType\", \"value\":[\"text/vnd.ideproject+directory\"]}]";
        doUpdate(folderId, properties);
        folder = mountPoint.getVirtualFileById(folderId);
        assertTrue("Regular folder must be converted to project. ", folder.isProject());
    }

    public void testUpdatePropertiesAndChangeFolderTypeBack() throws Exception {
        VirtualFile folder = mountPoint.getVirtualFileById(folderId);
        folder.updateProperties(Arrays.<Property>asList(new PropertyImpl("vfs:mimeType", "text/vnd.ideproject+directory")), null);
        assertTrue(folder.isProject());
        String properties = "[{\"name\":\"vfs:mimeType\", \"value\":[\"text/directory\"]}]";
        doUpdate(folderId, properties);
        folder = mountPoint.getVirtualFileById(folderId);
        assertFalse("Project must be converted to regular folder . ", folder.isProject());
    }

    public void doUpdate(String id, String rawData) throws Exception {
        String path = SERVICE_URI + "item/" + id;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, rawData.getBytes(), null);
        assertEquals(200, response.getStatus());
    }
}
