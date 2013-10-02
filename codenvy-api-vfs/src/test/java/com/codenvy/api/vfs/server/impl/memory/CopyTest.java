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
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.shared.ExitCodes;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class CopyTest extends MemoryFileSystemTest {
    private VirtualFile copyTestDestinationProject;
    private VirtualFile fileForCopy;
    private VirtualFile folderForCopy;
    private VirtualFile projectForCopy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile parentProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        folderForCopy = parentProject.createFolder("CopyTest_FOLDER");
        // add child in folder
        fileForCopy = folderForCopy.createFile("CopyTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));

        projectForCopy = parentProject.createProject("CopyTest_PROJECT", Collections.<Property>emptyList());

        copyTestDestinationProject =
                mountPoint.getRoot().createProject("CopyTest_DESTINATION", Collections.<Property>emptyList());
    }

    public void testCopyFile() throws Exception {
        final String originPath = fileForCopy.getPath();
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationProject.getPath() + '/' + fileForCopy.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found file in destination location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Copied file not accessible by id. ");
        }
    }

    public void testCopyFileAlreadyExist() throws Exception {
        final String originPath = fileForCopy.getPath();
        copyTestDestinationProject.createFile("CopyTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.ITEM_EXISTS, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
    }

    public void testCopyFileWrongParent() throws Exception {
        final String originPath = fileForCopy.getPath();
        VirtualFile destination =
                mountPoint.getRoot().createFile("destination", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + destination.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.INVALID_ARGUMENT, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
    }

    public void testCopyFileDestinationNoPermissions() throws Exception {
        final String originPath = fileForCopy.getPath();
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        copyTestDestinationProject.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(403, response.getStatus());
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(copyTestDestinationProject.getPath() + "/CopyTest_FILE");
            fail("File must not be copied since destination accessible for reading only. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testCopyFolder() throws Exception {
        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + '?' + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationProject.getPath() + "/" + folderForCopy.getName();
        final String originPath = folderForCopy.getPath();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source folder not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found folder in destination location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Copied folder not accessible by id. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath + "/CopyTest_FILE");
        } catch (ItemNotFoundException e) {
            fail("Child of folder missing after coping. ");
        }
        String childCopyId = mountPoint.getVirtualFile(expectedPath + "/CopyTest_FILE").getId();
        try {
            mountPoint.getVirtualFileById(childCopyId);
        } catch (ItemNotFoundException e) {
            fail("Child of copied folder not accessible by id. ");
        }
    }

    public void testCopyFolderNoPermissionForChild() throws Exception {
        VirtualFile myFile = folderForCopy.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        myFile.updateACL(createAcl(permissions), true, null);

        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + '?' + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationProject.getPath() + "/" + folderForCopy.getName();
        // one file must not be copied since permission restriction
        assertNull(mountPoint.getVirtualFile(expectedPath).getChild("file"));
    }

    public void testCopyFolderAlreadyExist() throws Exception {
        final String originPath = folderForCopy.getPath();
        copyTestDestinationProject.createFolder("CopyTest_FOLDER");
        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + "?" + "parentId=" + copyTestDestinationProject.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.ITEM_EXISTS, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source folder not found. ");
        }
    }

    public void testCopyProjectToProject() throws Exception {
        final String originPath = projectForCopy.getPath();
        String path = SERVICE_URI + "copy/" + projectForCopy.getId() + '?' +
                      "parentId=" + copyTestDestinationProject.getId();

        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals("Unexpected status " + response.getStatus(), 200, response.getStatus());
        String expectedPath = copyTestDestinationProject.getPath() + '/' + projectForCopy.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source project not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found project in destination location. ");
        }
        try {
            mountPoint.getVirtualFileById(((Item)response.getEntity()).getId());
        } catch (ItemNotFoundException e) {
            fail("Copied project not accessible by id. ");
        }
    }
}
