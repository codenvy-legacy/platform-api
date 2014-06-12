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
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
public class UpdateContentTest extends MemoryFileSystemTest {
    private String fileId;
    private String folderId;
    private String content = "__UpdateContentTest__";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile updateContentTestFolder = mountPoint.getRoot().createFolder(name);
        VirtualFile file = updateContentTestFolder.createFile("UpdateContentTest_FILE", "text/plain",
                                                              new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
        VirtualFile folder = updateContentTestFolder.createFolder("UpdateContentTest_FOLDER");
        folderId = folder.getId();
    }

    public void testUpdateContent() throws Exception {
        String path = SERVICE_URI + "content/" + fileId;

        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("text/plain;charset=utf8");
        headers.put("Content-Type", contentType);

        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(204, response.getStatus());

        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        checkFileContext(content, "text/plain;charset=utf8", file);
    }

    public void testUpdateContentFolder() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "content/" + folderId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, content.getBytes(), writer, null);
        assertEquals(400, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testUpdateContentNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<VirtualFileSystemInfo.BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(VirtualFileSystemInfo.BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(VirtualFileSystemInfo.BasicPermissions.READ));
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "content/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testUpdateContentLocked() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        String lockToken = file.lock(0);

        String path = SERVICE_URI + "content/" + fileId + '?' + "lockToken=" + lockToken;

        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("text/plain;charset=utf8");
        headers.put("Content-Type", contentType);

        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(204, response.getStatus());

        file = mountPoint.getVirtualFileById(fileId);
        checkFileContext(content, "text/plain;charset=utf8", file);
    }

    public void testUpdateContentLockedNoLockToken() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "content/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }
}
