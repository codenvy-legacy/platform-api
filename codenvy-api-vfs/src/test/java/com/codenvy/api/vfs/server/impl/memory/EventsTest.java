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
import com.codenvy.api.vfs.server.observation.CreateEvent;
import com.codenvy.api.vfs.server.observation.DeleteEvent;
import com.codenvy.api.vfs.server.observation.Event;
import com.codenvy.api.vfs.server.observation.EventListener;
import com.codenvy.api.vfs.server.observation.MoveEvent;
import com.codenvy.api.vfs.server.observation.NotificationService;
import com.codenvy.api.vfs.server.observation.RenameEvent;
import com.codenvy.api.vfs.server.observation.UpdateACLEvent;
import com.codenvy.api.vfs.server.observation.UpdateContentEvent;
import com.codenvy.api.vfs.server.observation.UpdatePropertiesEvent;

import org.everrest.core.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
public class EventsTest extends MemoryFileSystemTest {
    private VirtualFile testEventsFolder;
    private String      testFolderId;
    private String      testFolderPath;
    private String      testFileId;
    private String      testFilePath;

    private String destinationFolderID;
    private String destinationFolderPath;

    private Listener listener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        testEventsFolder = mountPoint.getRoot().createFolder(name);
        VirtualFile destinationFolder = mountPoint.getRoot().createFolder("EventsTest_DESTINATION_FOLDER");
        testFolderId = testEventsFolder.getId();
        testFolderPath = testEventsFolder.getPath();
        destinationFolderID = destinationFolder.getId();
        destinationFolderPath = destinationFolder.getPath();
        VirtualFile testFile = testEventsFolder.createFile("file", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()));
        testFileId = testFile.getId();
        testFilePath = testFile.getPath();

        listener = new Listener();
        NotificationService.register(listener);
    }

    @Override
    public void tearDown() throws Exception {
        NotificationService.unregister(listener);
        super.tearDown();
    }

    private class Listener implements EventListener {
        List<Event> events = new ArrayList<>();

        @Override
        public void create(CreateEvent event) {
            events.add(event);
        }

        @Override
        public void move(MoveEvent event) {
            events.add(event);
        }

        @Override
        public void rename(RenameEvent event) {
            events.add(event);
        }

        @Override
        public void delete(DeleteEvent event) {
            events.add(event);
        }

        @Override
        public void updateContent(UpdateContentEvent event) {
            events.add(event);
        }

        @Override
        public void updateProperties(UpdatePropertiesEvent event) {
            events.add(event);
        }

        @Override
        public void updateACL(UpdateACLEvent event) {
            events.add(event);
        }
    }

    public void testCreateFile() throws Exception {
        String name = "testCreateFile";
        String content = "test create file";
        String path = SERVICE_URI + "file/" + testFolderId + '?' + "name=" + name;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("text/plain;charset=utf8");
        headers.put("Content-Type", contentType);
        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, listener.events.get(0).getVirtualFile().getPath());
    }

    public void testCreateFolder() throws Exception {
        String name = "testCreateFolder";
        String path = SERVICE_URI + "folder/" + testFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, listener.events.get(0).getVirtualFile().getPath());
    }

    public void testCopy() throws Exception {
        String path = SERVICE_URI + "copy/" + testFileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(destinationFolderPath + "/file", listener.events.get(0).getVirtualFile().getPath());
    }

    public void testMove() throws Exception {
        String path = SERVICE_URI + "move/" + testFileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.MOVED, listener.events.get(0).getType());
        assertEquals(destinationFolderPath + "/file", listener.events.get(0).getVirtualFile().getPath());
        assertEquals(testFilePath, ((MoveEvent)listener.events.get(0)).getOldPath());
    }

    public void testUpdateContent() throws Exception {
        String path = SERVICE_URI + "content/" + testFileId;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("application/xml");
        headers.put("Content-Type", contentType);
        String content = "<?xml version='1.0'><root/>";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(204, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.CONTENT_UPDATED, listener.events.get(0).getType());
        assertEquals(testFilePath, listener.events.get(0).getVirtualFile().getPath());
        assertEquals("application/xml", listener.events.get(0).getVirtualFile().getMediaType());
    }

    public void testUpdateProperties() throws Exception {
        String path = SERVICE_URI + "item/" + testFileId;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("application/json");
        headers.put("Content-Type", contentType);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, properties.getBytes(), null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.PROPERTIES_UPDATED, listener.events.get(0).getType());
        assertEquals(testFilePath, listener.events.get(0).getVirtualFile().getPath());
    }

    public void testDelete() throws Exception {
        String path = SERVICE_URI + "delete/" + testFileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.DELETED, listener.events.get(0).getType());
        assertEquals(testFilePath, ((DeleteEvent)listener.events.get(0)).getPath());
    }

    public void testRename() throws Exception {
        String path = SERVICE_URI + "rename/" + testFileId + '?' + "newname=" + "_FILE_NEW_NAME_";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, listener.events.size());
        assertEquals(Event.ChangeType.RENAMED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + "_FILE_NEW_NAME_", listener.events.get(0).getVirtualFile().getPath());
        assertEquals(testFilePath, ((RenameEvent)listener.events.get(0)).getOldPath());
    }
}
