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
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
public class UpdateACLTest extends MemoryFileSystemTest {
    private String objectId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile updateAclTestFolder = mountPoint.getRoot().createFolder(name);

        VirtualFile file = updateAclTestFolder.createFile("UpdateACLTest_FILE", "text/plain",
                                                          new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        objectId = file.getId();
    }

    public void testUpdateAcl() throws Exception {
        String path = SERVICE_URI + "acl/" + objectId;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), null);
        assertEquals(204, response.getStatus());
        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, List<String>> m = toMap(acl);
        assertEquals(m.get("admin"), Arrays.asList("all"));
        assertEquals(m.get("john"), Arrays.asList("read"));
    }

    public void testUpdateAclOverride() throws Exception {
        Principal anyPrincipal = createPrincipal("any", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(anyPrincipal, EnumSet.of(BasicPermissions.ALL));
        mountPoint.getVirtualFileById(objectId).updateACL(createAcl(permissions), false, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + objectId + '?' + "override=" + true;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), writer, null);
        assertEquals(204, response.getStatus());

        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, List<String>> m = toMap(acl);
        assertEquals(m.get("admin"), Arrays.asList("all"));
        assertEquals(m.get("john"), Arrays.asList("read"));
        assertNull("Anonymous permissions must be removed.", m.get("anonymous"));
    }

    public void testUpdateAclMerge() throws Exception {
        Principal anyPrincipal = createPrincipal("any", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(anyPrincipal, EnumSet.of(BasicPermissions.ALL));
        mountPoint.getVirtualFileById(objectId).updateACL(createAcl(permissions), false, null);

        String path = SERVICE_URI + "acl/" + objectId;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), null);
        assertEquals(204, response.getStatus());

        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, List<String>> m = toMap(acl);
        assertEquals(m.get("admin"), Arrays.asList("all"));
        assertEquals(m.get("john"), Arrays.asList("read"));
        assertEquals(m.get("any"), Arrays.asList("all"));
    }

    public void testUpdateAclLocked() throws Exception {
        String lockToken = mountPoint.getVirtualFileById(objectId).lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + objectId + '?' + "lockToken=" + lockToken;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), writer, null);
        assertEquals(204, response.getStatus());

        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, List<String>> m = toMap(acl);
        assertEquals(m.get("admin"), Arrays.asList("all"));
        assertEquals(m.get("john"), Arrays.asList("read"));
    }

    public void testUpdateAclLockedNoLockToken() throws Exception {
        mountPoint.getVirtualFileById(objectId).lock(0);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + objectId;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), writer, null);
        assertEquals(423, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testUpdateAclNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        mountPoint.getVirtualFileById(objectId).updateACL(createAcl(permissions), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + objectId;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    private Map<String, List<String>> toMap(List<AccessControlEntry> acl) {
        Map<String, List<String>> m = new HashMap<>();
        for (AccessControlEntry e : acl) {
            m.put(e.getPrincipal().getName(), e.getPermissions());
        }
        return m;
    }
}
