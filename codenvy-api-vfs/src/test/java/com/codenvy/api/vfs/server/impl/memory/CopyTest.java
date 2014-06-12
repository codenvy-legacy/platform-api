/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server.impl.memory;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.shared.ExitCodes;
import com.codenvy.api.vfs.shared.dto.Item;
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
public class CopyTest extends MemoryFileSystemTest {
    private VirtualFile copyTestDestinationFolder;
    private VirtualFile fileForCopy;
    private VirtualFile folderForCopy;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile parentFolder = mountPoint.getRoot().createFolder(name);

        folderForCopy = parentFolder.createFolder("CopyTest_FOLDER");
        // add child in folder
        fileForCopy = folderForCopy.createFile("CopyTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));

        copyTestDestinationFolder = mountPoint.getRoot().createFolder("CopyTest_DESTINATION");
    }

    public void testCopyFile() throws Exception {
        final String originPath = fileForCopy.getPath();
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationFolder.getPath() + '/' + fileForCopy.getName();
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
        copyTestDestinationFolder.createFile("CopyTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationFolder.getId();
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
        copyTestDestinationFolder.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "copy/" + fileForCopy.getId() + '?' + "parentId=" + copyTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        log.info(new String(writer.getBody()));
        assertEquals(403, response.getStatus());
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source file not found. ");
        }
        try {
            mountPoint.getVirtualFile(copyTestDestinationFolder.getPath() + "/CopyTest_FILE");
            fail("File must not be copied since destination accessible for reading only. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testCopyFolder() throws Exception {
        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + '?' + "parentId=" + copyTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationFolder.getPath() + "/" + folderForCopy.getName();
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

        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + '?' + "parentId=" + copyTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = copyTestDestinationFolder.getPath() + "/" + folderForCopy.getName();
        // one file must not be copied since permission restriction
        assertNull(mountPoint.getVirtualFile(expectedPath).getChild("file"));
    }

    public void testCopyFolderAlreadyExist() throws Exception {
        final String originPath = folderForCopy.getPath();
        copyTestDestinationFolder.createFolder("CopyTest_FOLDER");
        String path = SERVICE_URI + "copy/" + folderForCopy.getId() + "?" + "parentId=" + copyTestDestinationFolder.getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.ITEM_EXISTS, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        try {
            mountPoint.getVirtualFile(originPath);
        } catch (ItemNotFoundException e) {
            fail("Source folder not found. ");
        }
    }
}
