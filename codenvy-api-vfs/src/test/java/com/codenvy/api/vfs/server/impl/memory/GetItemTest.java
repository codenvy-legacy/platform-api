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
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.dto.File;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class GetItemTest extends MemoryFileSystemTest {
    private String folderId;
    private String folderPath;
    private String fileId;
    private String filePath;
    private String projectId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile parentProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());
        VirtualFile folder = parentProject.createFolder("GetObjectTest_PARENT_PROJECT");
        folderId = folder.getId();
        folderPath = folder.getPath();

        VirtualFile file =
                parentProject.createFile("GetObjectTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        file.updateProperties(Arrays.<Property>asList(
                createProperty("MyProperty01", "hello world"),
                createProperty("MyProperty02", "to be or not to be"),
                createProperty("MyProperty03", "123"),
                createProperty("MyProperty04", "true"),
                createProperty("MyProperty05", "123.456")), null);
        fileId = file.getId();
        filePath = file.getPath();

        VirtualFile childProject = parentProject
                .createProject("GetObjectTest_CHILD_PROJECT", Arrays.<Property>asList(createProperty("vfs:projectType", "java"),
                                                                                      createProperty("prop1", "val1")));
        assertTrue(childProject.isProject());
        projectId = childProject.getId();
    }

    public void testGetFile() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "item/" + fileId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        //log.info(new String(writer.getBody()));
        Item item = (Item)response.getEntity();
        assertEquals(ItemType.FILE, item.getItemType());
        assertEquals(fileId, item.getId());
        assertEquals(filePath, item.getPath());
        validateLinks(item);
    }

    public void testGetFileByPath() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "itembypath" + filePath;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        Item item = (Item)response.getEntity();
        assertEquals(ItemType.FILE, item.getItemType());
        assertEquals(fileId, item.getId());
        assertEquals(filePath, item.getPath());
        validateLinks(item);
    }

    @SuppressWarnings("rawtypes")
    public void testGetFilePropertyFilter() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        // No filter - all properties
        String path = SERVICE_URI + "item/" + fileId;

        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        List<Property> properties = ((Item)response.getEntity()).getProperties();
        Map<String, List> m = new HashMap<>(properties.size());
        for (Property p : properties) {
            m.put(p.getName(), p.getValue());
        }
        assertTrue(m.size() >= 6);
        assertTrue(m.containsKey("MyProperty01"));
        assertTrue(m.containsKey("MyProperty02"));
        assertTrue(m.containsKey("MyProperty03"));
        assertTrue(m.containsKey("MyProperty04"));
        assertTrue(m.containsKey("MyProperty05"));

        // With filter
        path = SERVICE_URI + "item/" + fileId + '?' + "propertyFilter=" + "MyProperty02";

        response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        m.clear();
        properties = ((Item)response.getEntity()).getProperties();
        for (Property p : properties) {
            m.put(p.getName(), p.getValue());
        }
        assertEquals(1, m.size());
        assertEquals("to be or not to be", m.get("MyProperty02").get(0));
    }

    public void testGetFileNotFound() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "item/" + fileId + "_WRONG_ID_";
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(404, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testGetFileNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions. ALL));
        mountPoint.getVirtualFileById(fileId).updateACL(createAcl(permissions), true, null);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "item/" + fileId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testGetFileByPathNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions. ALL));
        mountPoint.getVirtualFileById(fileId).updateACL(createAcl(permissions), true, null);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "itembypath" + filePath;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testGetFolder() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "item/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        Item item = (Item)response.getEntity();
        assertEquals(ItemType.FOLDER, item.getItemType());
        assertEquals(folderId, item.getId());
        assertEquals(folderPath, item.getPath());
        validateLinks(item);
    }

    public void testGetFolderByPath() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "itembypath" + folderPath;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        Item item = (Item)response.getEntity();
        assertEquals(ItemType.FOLDER, item.getItemType());
        assertEquals(folderId, item.getId());
        assertEquals(folderPath, item.getPath());
        validateLinks(item);
    }

    public void testGetFolderByPathWithVersionID() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "itembypath" + folderPath + '?' + "versionId=" + "0";
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(400, response.getStatus());
    }

    public void testGetProjectItem() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "item/" + projectId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);

        assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
        assertEquals("application/json", response.getContentType().toString());

        Project project = (Project)response.getEntity();
        validateLinks(project);
        assertEquals("GetObjectTest_CHILD_PROJECT", project.getName());
        assertEquals(Project.PROJECT_MIME_TYPE, project.getMimeType());
        assertEquals("java", project.getProjectType());
        String val1 = null;
        for (Property property : project.getProperties()) {
            if (property.getName().equals("prop1")) {
                val1 = property.getValue().get(0);
                break;
            }
        }
        assertEquals("val1", val1);
        assertEquals(Project.PROJECT_MIME_TYPE, project.getMimeType());
    }
}
