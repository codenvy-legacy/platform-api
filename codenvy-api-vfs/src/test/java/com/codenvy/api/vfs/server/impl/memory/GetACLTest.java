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

import com.codenvy.api.core.user.UserImpl;
import com.codenvy.api.core.user.UserState;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class GetACLTest extends MemoryFileSystemTest {
    private VirtualFile file;
    private String      fileId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile getAclTestProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        file = getAclTestProject.createFile(name, "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));

        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Principal userPrincipal = createPrincipal("john", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(2);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        permissions.put(userPrincipal, EnumSet.of(BasicPermissions.READ));
        file.updateACL(createAcl(permissions), true, null);

        fileId = file.getId();
    }

    public void testGetACL() throws Exception {
        String path = SERVICE_URI + "acl/" + fileId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> acl = (List<AccessControlEntry>)response.getEntity();
        for (AccessControlEntry ace : acl) {
            if ("root".equals(ace.getPrincipal().getName())) {
                ace.getPermissions().contains("all");
            }
            if ("john".equals(ace.getPrincipal().getName())) {
                ace.getPermissions().contains("read");
            }
        }
    }

    public void testGetACLNoPermissions() throws Exception {
        Principal adminPrincipal = createPrincipal("admin", Principal.Type.USER);
        Map<Principal, Set<BasicPermissions>> permissions = new HashMap<>(1);
        permissions.put(adminPrincipal, EnumSet.of(BasicPermissions.ALL));
        UserState previous = UserState.get();
        UserState user = new UserState(new UserImpl("admin"));
        UserState.set(user);
        file.updateACL(createAcl(permissions), true, null);

        UserState.set(previous); // restore
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "acl/" + fileId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }
}
