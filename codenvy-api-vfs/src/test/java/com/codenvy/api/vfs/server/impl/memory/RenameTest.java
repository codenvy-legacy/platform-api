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
import com.codenvy.api.vfs.shared.AccessControlEntry;
import com.codenvy.api.vfs.shared.AccessControlEntryImpl;
import com.codenvy.api.vfs.shared.ExitCodes;
import com.codenvy.api.vfs.shared.Principal;
import com.codenvy.api.vfs.shared.PrincipalImpl;
import com.codenvy.api.vfs.shared.Property;
import com.codenvy.api.vfs.shared.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class RenameTest extends MemoryFileSystemTest {
    private VirtualFile renameTestProject;
    private String      fileId;
    private String      folderId;
    private VirtualFile file;
    private VirtualFile folder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        renameTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        folder = renameTestProject.createFolder("RenameFileTest_FOLDER");
        folderId = folder.getId();

        file = renameTestProject.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
    }

    public void testRenameFile() throws Exception {
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_" + '&' + "mediaType=" +
                      "text/*;charset=ISO-8859-1";
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = renameTestProject.getPath() + '/' + "_FILE_NEW_NAME_";
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
        renameTestProject.createFile("_FILE_NEW_NAME_", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
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
        String expectedPath = renameTestProject.getPath() + '/' + "_FILE_NEW_NAME_";
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
        String expectedPath = renameTestProject.getPath() + '/' + "_FILE_NEW_NAME_";
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
        AccessControlEntry adminACE = new AccessControlEntryImpl();
        adminACE.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        adminACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        AccessControlEntry userACE = new AccessControlEntryImpl();
        userACE.setPrincipal(new PrincipalImpl("john", Principal.Type.USER));
        userACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.READ.value())));
        file.updateACL(Arrays.asList(adminACE, userACE), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_";
        String originPath = file.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
        String expectedPath = renameTestProject.getPath() + '/' + "_FILE_NEW_NAME_";
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
        String expectedPath = renameTestProject.getPath() + '/' + "_FOLDER_NEW_NAME_";
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
        String expectedPath = renameTestProject.getPath() + '/' + "_FOLDER_NEW_NAME_";
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
        AccessControlEntry adminACE = new AccessControlEntryImpl();
        adminACE.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        adminACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        AccessControlEntry userACE = new AccessControlEntryImpl();
        userACE.setPrincipal(new PrincipalImpl("john", Principal.Type.USER));
        userACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.READ.value())));
        myFile.updateACL(Arrays.asList(adminACE, userACE), true, null);

        String path = SERVICE_URI + "rename/" + folderId + '?' + "newname=" + "_FOLDER_NEW_NAME_";
        String originPath = folder.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(403, response.getStatus());
        String expectedPath = renameTestProject.getPath() + '/' + "_FOLDER_NEW_NAME_";
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

    public void testConvertFolder() throws Exception {
        String path = SERVICE_URI + "rename/" + folderId + '?' + "newname=" + "_FOLDER_NEW_NAME_" +
                      '&' + "mediaType=" + "text/vnd.ideproject%2Bdirectory";
        String originPath = folder.getPath();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        String expectedPath = renameTestProject.getPath() + '/' + "_FOLDER_NEW_NAME_";
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
        VirtualFile folder = mountPoint.getVirtualFile(expectedPath);
        assertTrue("Regular folder must be converted to project. ", folder.isProject());
    }
}
