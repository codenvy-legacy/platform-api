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

import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.ACLCapability;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.QueryCapability;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** @author andrew00x */
public class GetAvailableFileSystemsTest extends MemoryFileSystemTest {
    public void testAvailableFS() throws Exception {
        String path = BASE_URI + "/vfs/my-ws";
        ByteArrayContainerResponseWriter wr = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, wr, null);
        //log.info(new String(wr.getBody()));
        assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
        @SuppressWarnings("unchecked")
        Collection<VirtualFileSystemInfo> entity = (Collection<VirtualFileSystemInfo>)response.getEntity();
        assertNotNull(entity);
        //assertEquals(1, entity.size());
        VirtualFileSystemInfo vfsInfo = null;
        for (VirtualFileSystemInfo e : entity) {
            if (e.getId().equals(MY_WORKSPACE_ID)) {
                if (vfsInfo != null)
                    fail("More then one VFS with the same ID found. ");
                vfsInfo = e;
            }
        }
        assertNotNull(vfsInfo);
        assertEquals(false, vfsInfo.isVersioningSupported());
        assertEquals(true, vfsInfo.isLockSupported());
        assertEquals(ACLCapability.MANAGE, vfsInfo.getAclCapability());
        assertEquals(QueryCapability.FULLTEXT, vfsInfo.getQueryCapability());
        assertEquals("anonymous", vfsInfo.getAnonymousPrincipal());
        assertEquals("any", vfsInfo.getAnyPrincipal());
        assertEquals(MY_WORKSPACE_ID, vfsInfo.getId());
        BasicPermissions[] basicPermissions = BasicPermissions.values();
        List<String> expectedPermissions = new ArrayList<>(basicPermissions.length);
        for (BasicPermissions bp : basicPermissions)
            expectedPermissions.add(bp.value());
        Collection<String> permissions = vfsInfo.getPermissions();
        assertTrue(permissions.containsAll(expectedPermissions));
        assertNotNull(vfsInfo.getRoot());
        assertEquals("/", vfsInfo.getRoot().getPath());
        validateLinks(vfsInfo.getRoot());
        validateUrlTemplates(vfsInfo);
    }
}
