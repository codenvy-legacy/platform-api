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
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
public class MoveTest extends MemoryFileSystemTest {
    private VirtualFile moveTestDestinationFolder;
    private VirtualFile folderForMove;
    private VirtualFile fileForMove;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile moveTestFolder = mountPoint.getRoot().createFolder(name);

        moveTestDestinationFolder = mountPoint.getRoot().createFolder(name + "_MoveTest_DESTINATION");

        folderForMove = moveTestFolder.createFolder("MoveTest_FOLDER");
        folderForMove.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));

        fileForMove = moveTestFolder.createFile("MoveTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
    }

    public void testMoveFile() throws Exception {
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = fileForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + fileForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
            fail("File must be moved. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found file in destination location. ");
        }
    }

    public void testMoveFileAlreadyExist() throws Exception {
        moveTestDestinationFolder.createFile(fileForMove.getName(), "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String originPath = fileForMove.getPath();
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
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
        final String originPath = fileForMove.getPath();
        VirtualFile destination =
                mountPoint.getRoot().createFile("destination", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + destination.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.INVALID_ARGUMENT, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
    }

    public void testMoveLockedFile() throws Exception {
        String lockToken = fileForMove.lock(0);
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId() +
                      '&' + "lockToken=" + lockToken;
        String originPath = fileForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + fileForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
            fail("File must be moved. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found file in destination location. ");
        }
    }

    public void testMoveLockedFileNoLockToken() throws Exception {
        fileForMove.lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = fileForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(423, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + fileForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("File must not be moved since it is locked. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testMoveFileNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        fileForMove.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = fileForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(403, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + fileForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("File must not be moved since permissions restriction.");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testMoveFileDestinationNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        moveTestDestinationFolder.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "move/" + fileForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = fileForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(403, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + fileForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("File must not be moved since permissions restriction on destination folder. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testMoveFolder() throws Exception {
        String path = SERVICE_URI + "move/" + folderForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = folderForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + folderForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
            fail("Folder must be moved. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Not found folder in destination location. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath + "/file");
        } catch (ItemNotFoundException e) {
            fail("Child of folder missing after moving. ");
        }
    }

    public void testMoveFolderWithLockedFile() throws Exception {
        folderForMove.getChild("file").lock(0);
        String path = SERVICE_URI + "move/" + folderForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = folderForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(423, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + folderForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("Folder must not be moved since it contains locked file. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testMoveFolderNoPermissionForChild() throws Exception {
        VirtualFile myFile = folderForMove.getChild("file");
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        myFile.updateACL(createAcl(permissions), true, null);

        String path = SERVICE_URI + "move/" + folderForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        String originPath = folderForMove.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(403, response.getStatus());
        String expectedPath = moveTestDestinationFolder.getPath() + '/' + folderForMove.getName();
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("Folder must not be moved since permissions restriction. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testMoveFolderAlreadyExist() throws Exception {
        moveTestDestinationFolder.createFolder(folderForMove.getName());
        String path = SERVICE_URI + "move/" + folderForMove.getId() + '?' + "parentId=" + moveTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.ITEM_EXISTS, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
    }
}
