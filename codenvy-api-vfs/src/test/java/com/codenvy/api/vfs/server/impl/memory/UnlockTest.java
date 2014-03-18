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

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;

/** @author andrew00x */
public class UnlockTest extends MemoryFileSystemTest {
    private String lockedFileId;
    private String notLockedFileId;
    private String fileLockToken;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile unlockTestFolder = mountPoint.getRoot().createFolder(name);

        VirtualFile lockedFile = unlockTestFolder.createFile("UnlockTest_LOCKED", "text/plain",
                                                             new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileLockToken = lockedFile.lock(0);
        lockedFileId = lockedFile.getId();

        VirtualFile notLockedFile = unlockTestFolder.createFile("UnlockTest_NOTLOCKED", "text/plain",
                                                                new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        notLockedFileId = notLockedFile.getId();
    }

    public void testUnlockFile() throws Exception {
        String path = SERVICE_URI + "unlock/" + lockedFileId + '?' + "lockToken=" + fileLockToken;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        VirtualFile file = mountPoint.getVirtualFileById(lockedFileId);
        assertFalse("Lock must be removed. ", file.isLocked());
    }

    public void testUnlockFileNoLockToken() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "unlock/" + lockedFileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testUnlockFileWrongLockToken() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "unlock/" + lockedFileId + '?' + "lockToken=" + fileLockToken + "_WRONG";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }


    public void testUnlockFileNotLocked() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "unlock/" + notLockedFileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }
}
