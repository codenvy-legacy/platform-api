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
import com.codenvy.api.vfs.shared.Principal;
import com.codenvy.api.vfs.shared.PrincipalImpl;
import com.codenvy.api.vfs.shared.Property;
import com.codenvy.api.vfs.shared.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class UpdateACLTest extends MemoryFileSystemTest {
    private String objectId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile updateAclTestFolder = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

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
        Map<String, Set<String>> m = toMap(acl);
        assertEquals(m.get("admin"), new HashSet<>(Arrays.asList("all")));
        assertEquals(m.get("john"), new HashSet<>(Arrays.asList("read")));
    }

    public void testUpdateAclOverride() throws Exception {
        AccessControlEntry ace = new AccessControlEntryImpl();
        ace.setPrincipal(new PrincipalImpl("any", Principal.Type.USER));
        ace.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        mountPoint.getVirtualFileById(objectId).updateACL(Arrays.asList(ace), false, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + objectId + '?' + "override=" + true;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), writer, null);
        assertEquals(204, response.getStatus());

        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, Set<String>> m = toMap(acl);
        assertEquals(m.get("admin"), new HashSet<>(Arrays.asList("all")));
        assertEquals(m.get("john"), new HashSet<>(Arrays.asList("read")));
        assertNull("Anonymous permissions must be removed.", m.get("anonymous"));
    }

    public void testUpdateAclMerge() throws Exception {
        AccessControlEntry ace = new AccessControlEntryImpl();
        ace.setPrincipal(new PrincipalImpl("any", Principal.Type.USER));
        ace.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        mountPoint.getVirtualFileById(objectId).updateACL(Arrays.asList(ace), false, null);

        String path = SERVICE_URI + "acl/" + objectId;
        String body = "[{\"principal\":{\"name\":\"admin\",\"type\":\"USER\"},\"permissions\":[\"all\"]}," + //
                      "{\"principal\":{\"name\":\"john\",\"type\":\"USER\"},\"permissions\":[\"read\"]}]";
        Map<String, List<String>> h = new HashMap<>(1);
        h.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response = launcher.service("POST", path, BASE_URI, h, body.getBytes(), null);
        assertEquals(204, response.getStatus());

        List<AccessControlEntry> acl = mountPoint.getVirtualFileById(objectId).getACL();
        Map<String, Set<String>> m = toMap(acl);
        assertEquals(m.get("admin"), new HashSet<>(Arrays.asList("all")));
        assertEquals(m.get("john"), new HashSet<>(Arrays.asList("read")));
        assertEquals(m.get("any"), new HashSet<>(Arrays.asList("all")));
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
        Map<String, Set<String>> m = toMap(acl);
        assertEquals(m.get("admin"), new HashSet<>(Arrays.asList("all")));
        assertEquals(m.get("john"), new HashSet<>(Arrays.asList("read")));
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
        AccessControlEntry adminACE = new AccessControlEntryImpl();
        adminACE.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        adminACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        AccessControlEntry userACE = new AccessControlEntryImpl();
        userACE.setPrincipal(new PrincipalImpl("john", Principal.Type.USER));
        userACE.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.READ.value())));
        mountPoint.getVirtualFileById(objectId).updateACL(Arrays.asList(adminACE, userACE), true, null);

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

    private Map<String, Set<String>> toMap(List<AccessControlEntry> acl) {
        Map<String, Set<String>> m = new HashMap<>();
        for (AccessControlEntry e : acl) {
            m.put(e.getPrincipal().getName(), e.getPermissions());
        }
        return m;
    }
}
