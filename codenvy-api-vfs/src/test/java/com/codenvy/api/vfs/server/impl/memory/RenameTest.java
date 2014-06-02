/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
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
public class RenameTest extends MemoryFileSystemTest {
    private VirtualFile renameTestFolder;
    private String      fileId;
    private String      folderId;
    private VirtualFile file;
    private VirtualFile folder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        renameTestFolder = mountPoint.getRoot().createFolder(name);

        folder = renameTestFolder.createFolder("RenameFileTest_FOLDER");
        folderId = folder.getId();

        file = renameTestFolder.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
    }

    public void testRenameFile() throws Exception {
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_" + '&' + "mediaType=" +
                      "text/*;charset=ISO-8859-1";
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = renameTestFolder.getPath() + '/' + "_FILE_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
            fail("File must be renamed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Can't find file after rename. ");
        }

        checkFileContext(DEFAULT_CONTENT, "text/*;charset=ISO-8859-1", mountPoint.getVirtualFile(expectedPath));
    }

    public void testRenameFileAlreadyExists() throws Exception {
        renameTestFolder.createFile("_FILE_NEW_NAME_", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_" + '&' + "mediaType=" +
                      "text/*;charset=ISO-8859-1";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.ITEM_EXISTS, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
    }

    public void testRenameFileLocked() throws Exception {
        String lockToken = file.lock(0);
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_" + '&' + "mediaType=" +
                      "text/*;charset=ISO-8859-1" + '&' + "lockToken=" + lockToken;
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = renameTestFolder.getPath() + '/' + "_FILE_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
            fail("File must be renamed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Can't find file after rename. ");
        }
    }

    public void testRenameFileLockedNoLockToken() throws Exception {
        file.lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_" + '&' + "mediaType=" +
                      "text/*;charset=ISO-8859-1";
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
        String expectedPath = renameTestFolder.getPath() + '/' + "_FILE_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("File must not be renamed since it is locked. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testRenameFileNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        file.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_";
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
        String expectedPath = renameTestFolder.getPath() + '/' + "_FILE_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("File must not be renamed since permissions restriction. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testRenameFolder() throws Exception {
        String path = SERVICE_URI + "rename/" + folderId + '?' + "newname=" + "_FOLDER_NEW_NAME_";
        String originPath = folder.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = renameTestFolder.getPath() + '/' + "_FOLDER_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
            fail("Folder must be renamed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
        } catch (ItemNotFoundException e) {
            fail("Can't folder file after rename. ");
        }
    }

    public void testRenameFolderWithLockedFile() throws Exception {
        folder.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes())).lock(0);
        String path = SERVICE_URI + "rename/" + folderId + '?' + "newname=" + "_FOLDER_NEW_NAME_";
        String originPath = folder.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(423, response.getStatus());
        String expectedPath = renameTestFolder.getPath() + '/' + "_FOLDER_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("Folder must not be renamed since it contains locked file. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testRenameFolderNoPermissionForChild() throws Exception {
        VirtualFile myFile = folder.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        myFile.updateACL(createAcl(permissions), true, null);

        String path = SERVICE_URI + "rename/" + folderId + '?' + "newname=" + "_FOLDER_NEW_NAME_";
        String originPath = folder.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(403, response.getStatus());
        String expectedPath = renameTestFolder.getPath() + '/' + "_FOLDER_NEW_NAME_";
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(expectedPath);
            fail("Folder must not be renamed since permissions restriction. ");
        } catch (ItemNotFoundException e) {
        }
    }
}
