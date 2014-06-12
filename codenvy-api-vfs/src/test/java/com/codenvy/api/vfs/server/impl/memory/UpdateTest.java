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

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
public class UpdateTest extends MemoryFileSystemTest {
    private String fileId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile updateTestFolder = mountPoint.getRoot().createFolder(name);
        VirtualFile file =
                updateTestFolder.createFile("UpdateTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        fileId = file.getId();
    }

    public void testUpdatePropertiesFile() throws Exception {
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        doUpdate(fileId, properties);
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        assertEquals("MyValue", file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesLockedFile() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        String lockToken = file.lock(0);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        String path = SERVICE_URI + "item/" + fileId + "?lockToken=" + lockToken;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals(200, response.getStatus());
        file = mountPoint.getVirtualFileById(fileId);
        assertEquals("MyValue", file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesLockedFileNoLockToken() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        file.lock(0);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        String path = SERVICE_URI + "item/" + fileId;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals(423, response.getStatus());
        file = mountPoint.getVirtualFileById(fileId);
        assertEquals(null, file.getPropertyValue("MyProperty"));
    }

    public void testUpdatePropertiesNoPermissions() throws Exception {
        VirtualFile file = mountPoint.getVirtualFileById(fileId);
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<VirtualFileSystemInfo.BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(VirtualFileSystemInfo.BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(VirtualFileSystemInfo.BasicPermissions.READ));
        file.updateACL(createAcl(permissions), true, null);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        String path = SERVICE_URI + "item/" + fileId;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, properties.getBytes(), null);
        assertEquals(403, response.getStatus());
        file = mountPoint.getVirtualFileById(fileId);
        assertEquals(null, file.getPropertyValue("MyProperty"));
    }

    public void doUpdate(String id, String rawData) throws Exception {
        String path = SERVICE_URI + "item/" + id;
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, rawData.getBytes(), null);
        assertEquals(200, response.getStatus());
    }
}
