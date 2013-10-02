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

import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class CreateTest extends MemoryFileSystemTest {
    private String      createTestFolderId;
    private String      createTestFolderPath;
    private VirtualFile createTestFolder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        createTestFolder = mountPoint.getRoot().createProject(name, java.util.Collections.<Property>emptyList());
        createTestFolderId = createTestFolder.getId();
        createTestFolderPath = createTestFolder.getPath();
    }

    public void testCreateFile() throws Exception {
        String name = "testCreateFile";
        String content = "test create file";
        String path = SERVICE_URI + "file/" + createTestFolderId + '?' + "name=" + name; //
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("text/plain;charset=utf8");
        headers.put("Content-Type", contentType);

        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("File was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created file not accessible by id. ");
        }
        VirtualFile file = mountPoint.getVirtualFile(expectedPath);
        checkFileContext(content, "text/plain;charset=utf8", file);
    }

    public void testCreateFileInRoot() throws Exception {
        String name = "testCreateFileInRoot";
        String content = "test create file";
        String path = SERVICE_URI + "file/" + mountPoint.getRoot().getId() + '?' + "name=" + name;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("text/plain;charset=utf8");
        headers.put("Content-Type", contentType);

        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(200, response.getStatus());
        String expectedPath = "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("File was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created file not accessible by id. ");
        }
        VirtualFile file = mountPoint.getVirtualFile(expectedPath);
        checkFileContext(content, "text/plain;charset=utf8", file);
    }

    public void testCreateFileNoContent() throws Exception {
        String name = "testCreateFileNoContent";
        String path = SERVICE_URI + "file/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);

        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("File was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created file not accessible by id. ");
        }
        VirtualFile file = mountPoint.getVirtualFile(expectedPath);
        ContentStream contentStream = file.getContent();
        assertEquals(0, contentStream.getLength());
    }

    public void testCreateFileNoMediaType() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String name = "testCreateFileNoMediaType";
        String content = "test create file without media type";
        String path = SERVICE_URI + "file/" + createTestFolderId + '?' + "name=" + name;

        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, content.getBytes(), writer, null);
        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("File was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created file not accessible by id. ");
        }
        VirtualFile file = mountPoint.getVirtualFile(expectedPath);
        checkFileContext(content, MediaType.APPLICATION_OCTET_STREAM, file);
    }

    public void testCreateFileNoName() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "file/" + createTestFolderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, DEFAULT_CONTENT.getBytes(), writer, null);
        assertEquals(400, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFileNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions. ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions. READ));
        createTestFolder.updateACL(createAcl(permissions), true, null);

        String name = "testCreateFileNoPermissions";
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "file/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, DEFAULT_CONTENT.getBytes(), writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFileWrongParent() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String name = "testCreateFileWrongParent";
        String path = SERVICE_URI + "file/" + createTestFolderId + "_WRONG_ID" + '?' + "name=" + name;
        ContainerResponse response =
                launcher.service("POST", path, BASE_URI, null, DEFAULT_CONTENT.getBytes(), writer, null);
        assertEquals(404, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFolder() throws Exception {
        String name = "testCreateFolder";
        String path = SERVICE_URI + "folder/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Folder was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created folder not accessible by id. ");
        }
    }

    public void testCreateFolderInRoot() throws Exception {
        String name = "testCreateFolderInRoot";
        String path = SERVICE_URI + "folder/" + mountPoint.getRoot().getId() + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Folder was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created folder not accessible by id. ");
        }
    }

    public void testCreateFolderNoName() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "folder/" + createTestFolderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(400, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFolderNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        createTestFolder.updateACL(createAcl(permissions), true, null);

        String name = "testCreateFolderNoPermissions";
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "folder/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFolderWrongParent() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String name = "testCreateFolderWrongParent";
        String path = SERVICE_URI + "folder/" + createTestFolderId + "_WRONG_ID" + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(404, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testCreateFolderHierarchy() throws Exception {
        String name = "testCreateFolderHierarchy/1/2/3/4/5";
        String path = SERVICE_URI + "folder/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Folder was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created folder not accessible by id. ");
        }
    }

    public void testCreateFolderHierarchy2() throws Exception {
        // create some items in path
        String name = "testCreateFolderHierarchy/1/2/3";
        String path = SERVICE_URI + "folder/" + createTestFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Folder was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created folder not accessible by id. ");
        }
        // create the rest of path
        name += "/4/5";
        path = SERVICE_URI + "folder/" + createTestFolderId + '?' + "name=" + name;
        response = launcher.service("POST", path, BASE_URI, null, null, null, null);
        assertEquals(200, response.getStatus());
        expectedPath = createTestFolderPath + "/" + name;
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Folder was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created folder not accessible by id. ");
        }
    }

    public void testCreateProject() throws Exception {
        String name = "testCreateProject";
        String properties = "[{\"name\":\"vfs:projectType\", \"value\":[\"java\"]}]";
        String path = SERVICE_URI + "project/" + createTestFolderId + '?' + "name=" + name + '&' + "type=" + "java";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
        String expectedPath = createTestFolderPath + "/" + name;
        try {
            VirtualFile project = mountPoint.getVirtualFile(expectedPath);
            List<String> values = project.getProperties(PropertyFilter.valueOf("vfs:projectType")).get(0).getValue();
            assertEquals("java", values.get(0));
            assertEquals("text/vnd.ideproject+directory", project.getMediaType());
        } catch (ItemNotFoundException e) {
            fail("Project was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created project not accessible by id. ");
        }
    }

    public void testCreateProjectInsideProject() throws Exception {
        VirtualFile parentProject = mountPoint.getVirtualFileById(createTestFolderId);
        String path = SERVICE_URI + "project/" + parentProject.getId() + '?' + "name=" + "childProject" + '&' + "type=" + "java";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, null, null);
        assertEquals("Unexpected status " + response.getStatus(), 200, response.getStatus());
        String expectedPath = parentProject.getPath() + "/childProject";
        try {
            VirtualFile project = mountPoint.getVirtualFile(expectedPath);
            List<String> values = project.getProperties(PropertyFilter.valueOf("vfs:projectType")).get(0).getValue();
            assertEquals("java", values.get(0));
            assertEquals("text/vnd.ideproject+directory", project.getMediaType());
        } catch (ItemNotFoundException e) {
            fail("Project was not created in expected location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Created project not accessible by id. ");
        }
    }
}
