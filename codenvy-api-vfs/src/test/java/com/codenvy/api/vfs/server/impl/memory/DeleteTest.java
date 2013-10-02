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
public class DeleteTest extends MemoryFileSystemTest {
    private String      folderId;
    private String      folderChildId;
    private String      fileId;
    private String      folderPath;
    private String      folderChildPath;
    private String      filePath;
    private VirtualFile file;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile deleteTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        VirtualFile folder = deleteTestProject.createFolder("DeleteTest_FOLDER");
        // add child in folder
        VirtualFile childFile = folder.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        folderId = folder.getId();
        folderChildId = childFile.getId();
        folderPath = folder.getPath();
        folderChildPath = childFile.getPath();

        file = deleteTestProject.createFile("DeleteTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
        filePath = file.getPath();
    }

    public void testDeleteFile() throws Exception {
        String path = SERVICE_URI + "delete/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        try {
            mountPoint.getVirtualFileById(fileId);
            fail("File must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(filePath);
            fail("File must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        assertFalse(file.exists());
    }

    public void testDeleteFileLocked() throws Exception {
        String lockToken = file.lock(0);
        String path = SERVICE_URI + "delete/" + fileId + '?' + "lockToken=" + lockToken;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        try {
            mountPoint.getVirtualFileById(fileId);
            fail("File must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(filePath);
            fail("File must be removed. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testDeleteFileLockedNoLockToken() throws Exception {
        file.lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "delete/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
        try {
            mountPoint.getVirtualFileById(fileId);
        } catch (ItemNotFoundException e) {
            fail("File must not be removed since it is locked. ");
        }
    }

    public void testDeleteFileNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        file.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "delete/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
        try {
            mountPoint.getVirtualFileById(fileId);
        } catch (ItemNotFoundException e) {
            fail("File must not be removed since permissions restriction. ");
        }
    }

    public void testDeleteFileWrongId() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "delete/" + fileId + "_WRONG_ID";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(404, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testDeleteRootFolder() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "delete/" + mountPoint.getRoot().getId();
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.INVALID_ARGUMENT, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
        log.info(new String(writer.getBody()));
    }

    public void testDeleteFolder() throws Exception {
        String path = SERVICE_URI + "delete/" + folderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        try {
            mountPoint.getVirtualFileById(folderId);
            fail("Folder must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFileById(folderChildId);
            fail("Child file must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(folderPath);
            fail("Folder must be removed. ");
        } catch (ItemNotFoundException e) {
        }
        try {
            mountPoint.getVirtualFile(folderChildPath);
            fail("Child file must be removed. ");
        } catch (ItemNotFoundException e) {
        }
    }

    public void testDeleteFolderNoPermissionForChild() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        mountPoint.getVirtualFileById(folderChildId).updateACL(createAcl(permissions), true, null);

        String path = SERVICE_URI + "delete/" + folderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(403, response.getStatus());
        try {
            mountPoint.getVirtualFileById(folderId);
        } catch (ItemNotFoundException e) {
            fail("Folder must not be removed since permissions restriction. ");
        }
    }

    public void testDeleteFolderLockedChild() throws Exception {
        mountPoint.getVirtualFileById(folderChildId).lock(0);
        String path = SERVICE_URI + "delete/" + folderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(423, response.getStatus());
        try {
            mountPoint.getVirtualFileById(folderId);
        } catch (ItemNotFoundException e) {
            fail("Folder must not be removed since child file is locked. ");
        }
    }
}
