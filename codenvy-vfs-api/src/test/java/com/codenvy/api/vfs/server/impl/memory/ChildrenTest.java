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
import com.codenvy.api.vfs.shared.Item;
import com.codenvy.api.vfs.shared.ItemImpl;
import com.codenvy.api.vfs.shared.ItemList;
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.Principal;
import com.codenvy.api.vfs.shared.PrincipalImpl;
import com.codenvy.api.vfs.shared.Property;
import com.codenvy.api.vfs.shared.PropertyImpl;
import com.codenvy.api.vfs.shared.VirtualFileSystemInfo.BasicPermissions;

import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/** @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a> */
public class ChildrenTest extends MemoryFileSystemTest {
    private String folderId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        VirtualFile parentProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());

        VirtualFile folder = parentProject.createFolder("ChildrenTest_FOLDER");

        VirtualFile file = folder.createFile("ChildrenTest_FILE01", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        file.updateProperties(Arrays.<Property>asList(new PropertyImpl("PropertyA", "A"), new PropertyImpl("PropertyB", "B")), null);

        VirtualFile folder1 = folder.createFolder("ChildrenTest_FOLDER01");
        folder1.updateProperties(Arrays.<Property>asList(new PropertyImpl("PropertyA", "A"), new PropertyImpl("PropertyB", "B")), null);

        VirtualFile folder2 = folder.createFolder("ChildrenTest_FOLDER02");
        folder2.updateProperties(Arrays.<Property>asList(new PropertyImpl("PropertyA", "A"), new PropertyImpl("PropertyB", "B")), null);

        folderId = folder.getId();
    }

    public void testGetChildren() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        //log.info(new String(writer.getBody()));
        @SuppressWarnings("unchecked")
        ItemList children = (ItemList)response.getEntity();
        List<String> list = new ArrayList<>(3);
        for (Item i : children.getItems()) {
            validateLinks(i);
            list.add(i.getName());
        }
        assertEquals(3, list.size());
        assertTrue(list.contains("ChildrenTest_FOLDER01"));
        assertTrue(list.contains("ChildrenTest_FOLDER02"));
        assertTrue(list.contains("ChildrenTest_FILE01"));
    }

    public void testGetChildrenNoPermissions() throws Exception {
        AccessControlEntry ace = new AccessControlEntryImpl();
        ace.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        ace.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        mountPoint.getVirtualFileById(folderId).updateACL(Arrays.asList(ace), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(403, response.getStatus());
        log.info(new String(writer.getBody()));
    }

    public void testGetRootChildrenNoPermissions() throws Exception {
        // Special behaviour for root folder.
        // Never check permission when read root folder but hide content of it.
        AccessControlEntry ace = new AccessControlEntryImpl();
        ace.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        ace.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        mountPoint.getRoot().updateACL(Arrays.asList(ace), true, null);

        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "children/" + mountPoint.getRoot().getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        assertEquals(0, children.getItems().size());
    }

    public void testGetChildrenNoPermissionsFiltering() throws Exception {
        VirtualFile folder = mountPoint.getVirtualFileById(folderId);
        VirtualFile protectedItem = folder.getChild("ChildrenTest_FILE01");
        AccessControlEntry ace = new AccessControlEntryImpl();
        ace.setPrincipal(new PrincipalImpl("admin", Principal.Type.USER));
        ace.setPermissions(new HashSet<>(Arrays.asList(BasicPermissions.ALL.value())));
        // after that item must not appear in response
        protectedItem.updateACL(Arrays.asList(ace), true, null);

        // Have permission for read folder but have not permission to read one of its child.
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        assertEquals(200, response.getStatus());
        //log.info(new String(writer.getBody()));
        @SuppressWarnings("unchecked")
        ItemList children = (ItemList)response.getEntity();
        List<String> list = new ArrayList<>(2);
        for (Item i : children.getItems()) {
            validateLinks(i);
            list.add(i.getName());
        }
        assertEquals(2, list.size());
        assertTrue(list.contains("ChildrenTest_FOLDER01"));
        assertTrue(list.contains("ChildrenTest_FOLDER02"));
    }

    @SuppressWarnings("unchecked")
    public void testGetChildrenPagingSkipCount() throws Exception {
        // Get all children.
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        List<Object> all = new ArrayList<>(3);
        for (Item i : children.getItems()) {
            all.add(i.getName());
        }

        Iterator<Object> iteratorAll = all.iterator();
        iteratorAll.next();
        iteratorAll.remove();

        // Skip first item in result.
        path = SERVICE_URI + "children/" + folderId + "?" + "skipCount=" + 1;
        checkPage(path, "GET", ItemImpl.class.getMethod("getName"), all);
    }

    @SuppressWarnings("unchecked")
    public void testGetChildrenPagingMaxItems() throws Exception {
        // Get all children.
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        List<Object> all = new ArrayList<>(3);
        for (Item i : children.getItems()) {
            all.add(i.getName());
        }

        // Exclude last item from result.
        path = SERVICE_URI + "children/" + folderId + "?" + "maxItems=" + 2;
        all.remove(2);
        checkPage(path, "GET", ItemImpl.class.getMethod("getName"), all);
    }

    @SuppressWarnings("unchecked")
    public void testGetChildrenNoPropertyFilter() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        // Get children without filter.
        String path = SERVICE_URI + "children/" + folderId;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        assertEquals(3, children.getItems().size());
        for (Item i : children.getItems()) {
            // No properties without filter. 'none' filter is used if nothing set by client.
            assertFalse(hasProperty(i, "PropertyA"));
            assertFalse(hasProperty(i, "PropertyB"));
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetChildrenPropertyFilter() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        // Get children and apply filter for properties.
        String propertyFilter = "PropertyA";
        String path = SERVICE_URI + "children/" + folderId + "?" + "propertyFilter=" + propertyFilter;
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        assertEquals(3, children.getItems().size());
        for (Item i : children.getItems()) {
            assertTrue(hasProperty(i, "PropertyA"));
            assertFalse(hasProperty(i, "PropertyB")); // must be excluded
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetChildrenTypeFilter() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        // Get children and apply filter for properties.
        String path = SERVICE_URI + "children/" + folderId + "?" + "itemType=folder";
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, writer, null);
        //log.info(new String(writer.getBody()));
        assertEquals(200, response.getStatus());
        ItemList children = (ItemList)response.getEntity();
        assertEquals(2, children.getItems().size());
        for (Item i : children.getItems()) {
            assertTrue(i.getItemType() == ItemType.FOLDER);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean hasProperty(Item i, String propertyName) {
        List<Property> properties = i.getProperties();
        if (properties.size() == 0) {
            return false;
        }
        for (Property p : properties) {
            if (p.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }
}
