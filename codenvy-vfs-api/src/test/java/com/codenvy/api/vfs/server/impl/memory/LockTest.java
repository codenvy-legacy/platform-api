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
public class LockTest extends MemoryFileSystemTest {
    private String folderId;
    private String fileId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile lockTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        VirtualFile folder = lockTestProject.createFolder("LockTest_FOLDER");
        folderId = folder.getId();

        VirtualFile file = lockTestProject.createFile("LockTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
    }

    public void testLockFile() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "lock/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        log.info(new String(writer.getBody()));
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        assertTrue("File must be locked. ", file.isLocked());
        validateLinks(getItem(fileId));
    }

    public void testLockFileAlreadyLocked() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "lock/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testLockFileNoPermissions() throws Exception {
        AccessControlEntry adminACE = new AccessControlEntryImpl();
        adminACE.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        adminACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        AccessControlEntry userACE = new AccessControlEntryImpl();
        userACE.setPrincipal(new PrincipalImpl("john", Principal.Type.USER));
        userACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.READ.value())));
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.updateACL(Arrays.asList(adminACE, userACE), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "lock/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
        file = mountPoint.getVirtualFileById(fileId);
        assertFalse("File must not be locked. ", file.isLocked());
    }

    public void testLockFolder() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "lock/" + folderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(400, response.getStatus());
        assertEquals(ExitCodes.INVALID_ARGUMENT, Integer.parseInt((String)response.getHttpHeaders().getFirst("X-Exit-Code")));
    }

    public void testLockTimeout() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.lock(100);
        assertTrue(file.isLocked());
        Thread.sleep(200);
        assertFalse(file.isLocked());
    }
}
