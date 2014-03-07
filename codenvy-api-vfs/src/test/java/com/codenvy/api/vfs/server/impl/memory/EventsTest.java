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

import com.codenvy.api.core.notification.MessageReceiver;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.observation.CreateEvent;
import com.codenvy.api.vfs.server.observation.DeleteEvent;
import com.codenvy.api.vfs.server.observation.VirtualFileEvent;
import com.codenvy.api.vfs.server.observation.MoveEvent;
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

    private String                 destinationFolderID;
    private String                 destinationFolderPath;
    private List<VirtualFileEvent> events;

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
        events = new ArrayList<>();
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<CreateEvent>() {
            @Override
            public void onEvent(String channel, CreateEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<MoveEvent>() {
            @Override
            public void onEvent(String channel, MoveEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<RenameEvent>() {
            @Override
            public void onEvent(String channel, RenameEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<DeleteEvent>() {
            @Override
            public void onEvent(String channel, DeleteEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<UpdateContentEvent>() {
            @Override
            public void onEvent(String channel, UpdateContentEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<UpdatePropertiesEvent>() {
            @Override
            public void onEvent(String channel, UpdatePropertiesEvent data) {
                events.add(data);
            }
        });
        mountPoint.getEventService().subscribe("vfs", new MessageReceiver<UpdateACLEvent>() {
            @Override
            public void onEvent(String channel, UpdateACLEvent data) {
                events.add(data);
            }
        });

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
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.CREATED, events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, events.get(0).getVirtualFile().getPath());
    }

    public void testCreateFolder() throws Exception {
        String name = "testCreateFolder";
        String path = SERVICE_URI + "folder/" + testFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.CREATED, events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, events.get(0).getVirtualFile().getPath());
    }

    public void testCopy() throws Exception {
        String path = SERVICE_URI + "copy/" + testFileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.CREATED, events.get(0).getType());
        assertEquals(destinationFolderPath + "/file", events.get(0).getVirtualFile().getPath());
    }

    public void testMove() throws Exception {
        String path = SERVICE_URI + "move/" + testFileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.MOVED, events.get(0).getType());
        assertEquals(destinationFolderPath + "/file", events.get(0).getVirtualFile().getPath());
        assertEquals(testFilePath, ((MoveEvent)events.get(0)).getOldPath());
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
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.CONTENT_UPDATED, events.get(0).getType());
        assertEquals(testFilePath, events.get(0).getVirtualFile().getPath());
        assertEquals("application/xml", events.get(0).getVirtualFile().getMediaType());
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
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.PROPERTIES_UPDATED, events.get(0).getType());
        assertEquals(testFilePath, events.get(0).getVirtualFile().getPath());
    }

    public void testDelete() throws Exception {
        String path = SERVICE_URI + "delete/" + testFileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.DELETED, events.get(0).getType());
        assertEquals(testFilePath, ((DeleteEvent)events.get(0)).getPath());
    }

    public void testRename() throws Exception {
        String path = SERVICE_URI + "rename/" + testFileId + '?' + "newname=" + "_FILE_NEW_NAME_";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(1, events.size());
        assertEquals(VirtualFileEvent.ChangeType.RENAMED, events.get(0).getType());
        assertEquals(testFolderPath + '/' + "_FILE_NEW_NAME_", events.get(0).getVirtualFile().getPath());
        assertEquals(testFilePath, ((RenameEvent)events.get(0)).getOldPath());
    }
}
