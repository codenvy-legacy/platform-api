/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
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
package com.codenvy.api.resources;

import com.codenvy.api.resources.server.ResourceAccessControlEntryImpl;
import com.codenvy.api.resources.shared.ResourceAccessControlEntry;
import com.codenvy.api.vfs.shared.Principal;
import com.codenvy.api.vfs.shared.PrincipalImpl;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ResourceAccessControlEntryTest {
    @Test
    public void testAddPermissions() {
        ResourceAccessControlEntry ace = new ResourceAccessControlEntryImpl(new PrincipalImpl("user", Principal.Type.USER));
        ace.getPermissions().add("read");
        Assert.assertTrue(ace.hasPermission("read"));
        Assert.assertTrue(ace.isUpdated());
    }

    @Test
    public void testRemovePermissions() {
        ResourceAccessControlEntry ace =
                new ResourceAccessControlEntryImpl(new PrincipalImpl("user", Principal.Type.USER), new HashSet<>(Arrays.asList("read")));
        Assert.assertTrue(ace.hasPermission("read"));
        ace.getPermissions().remove("read");
        Assert.assertFalse(ace.hasPermission("read"));
        Assert.assertTrue(ace.isUpdated());
    }

    @Test
    public void testClearPermissions() {
        ResourceAccessControlEntry ace =
                new ResourceAccessControlEntryImpl(new PrincipalImpl("user", Principal.Type.USER),
                                                   new HashSet<>(Arrays.asList("read", "write")));
        Assert.assertTrue(ace.hasPermission("read"));
        Assert.assertTrue(ace.hasPermission("write"));
        ace.getPermissions().clear();
        Assert.assertFalse(ace.hasPermission("read"));
        Assert.assertFalse(ace.hasPermission("write"));
        Assert.assertTrue(ace.isUpdated());
    }

    @Test
    public void testSetPermissions() {
        ResourceAccessControlEntry ace = new ResourceAccessControlEntryImpl(new PrincipalImpl("user", Principal.Type.USER));
        Assert.assertFalse(ace.hasPermission("read"));
        ace.setPermissions(new HashSet<>(Arrays.asList("read", "write")));
        Assert.assertTrue(ace.hasPermission("read"));
        Assert.assertTrue(ace.hasPermission("write"));
        Assert.assertTrue(ace.isUpdated());
    }
}
