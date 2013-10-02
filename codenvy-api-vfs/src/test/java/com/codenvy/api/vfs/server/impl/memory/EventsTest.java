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
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.ChangeEvent;
import com.codenvy.api.vfs.server.observation.ChangeEventFilter;
import com.codenvy.api.vfs.server.observation.EventListener;
import com.codenvy.api.vfs.shared.dto.Property;

import org.everrest.core.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class EventsTest extends MemoryFileSystemTest {
    private VirtualFile testEventsProject;
    private String      testFolderId;
    private String      testFolderPath;

    private String destinationFolderID;
    private String destinationFolderPath;

    private ChangeEventFilter filter;
    private Listener          listener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String name = getClass().getName();
        testEventsProject = mountPoint.getRoot().createProject(name, Collections.<Property>emptyList());
        VirtualFile destinationProject =
                mountPoint.getRoot().createProject("EventsTest_DESTINATION_FOLDER", Collections.<Property>emptyList());
        testFolderId = testEventsProject.getId();
        testFolderPath = testEventsProject.getPath();
        destinationFolderID = destinationProject.getId();
        destinationFolderPath = destinationProject.getPath();

        assertNotNull(eventListenerList);
        listener = new Listener();
        filter = ChangeEventFilter.ANY_FILTER;
        eventListenerList.addEventListener(filter, listener);
    }

    @Override
    protected void tearDown() throws Exception {
        assertTrue("Unable remove listener. ", eventListenerList.removeEventListener(filter, listener));
        super.tearDown();
    }

    private class Listener implements EventListener {
        List<ChangeEvent> events = new ArrayList<>();

        @Override
        public void handleEvent(ChangeEvent event) throws VirtualFileSystemException {
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
        assertEquals(ChangeEvent.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, listener.events.get(0).getItemPath());
        assertEquals(contentType.get(0), listener.events.get(0).getMimeType());
    }

    public void testCreateFolder() throws Exception {
        String name = "testCreateFolder";
        String path = SERVICE_URI + "folder/" + testFolderId + '?' + "name=" + name;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + name, listener.events.get(0).getItemPath());
    }

    public void testCopy() throws Exception {
        String fileId =
                testEventsProject.createFile("CopyTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes())).getId();

        String path = SERVICE_URI + "copy/" + fileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);

        assertEquals(200, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.CREATED, listener.events.get(0).getType());
        assertEquals(destinationFolderPath + '/' + "CopyTest_FILE", listener.events.get(0).getItemPath());
        assertEquals("text/plain", listener.events.get(0).getMimeType());
    }

    public void testMove() throws Exception {
        String fileId =
                testEventsProject.createFile("MoveTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes())).getId();

        String path = SERVICE_URI + "move/" + fileId + '?' + "parentId=" + destinationFolderID;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.MOVED, listener.events.get(0).getType());
        assertEquals(destinationFolderPath + '/' + "MoveTest_FILE", listener.events.get(0).getItemPath());
        assertEquals("text/plain", listener.events.get(0).getMimeType());
    }

    public void testUpdateContent() throws Exception {
        String fileId =
                testEventsProject.createFile("UpdateContentTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()))
                                 .getId();

        String path = SERVICE_URI + "content/" + fileId;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("application/xml");
        headers.put("Content-Type", contentType);
        String content = "<?xml version='1.0'><root/>";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, content.getBytes(), null);
        assertEquals(204, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.CONTENT_UPDATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + "UpdateContentTest_FILE", listener.events.get(0).getItemPath());
        assertEquals("application/xml", listener.events.get(0).getMimeType());
    }

    public void testUpdateProperties() throws Exception {
        String fileId =
                testEventsProject
                        .createFile("UpdatePropertiesTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes()))
                        .getId();

        String path = SERVICE_URI + "item/" + fileId;
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentType = new ArrayList<>();
        contentType.add("application/json");
        headers.put("Content-Type", contentType);
        String properties = "[{\"name\":\"MyProperty\", \"value\":[\"MyValue\"]}]";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, properties.getBytes(), null);
        assertEquals(200, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.PROPERTIES_UPDATED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + "UpdatePropertiesTest_FILE", listener.events.get(0).getItemPath());
        assertEquals("text/plain", listener.events.get(0).getMimeType());
    }

    public void testDelete() throws Exception {
        String fileId =
                testEventsProject.createFile("DeleteTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes())).getId();

        String path = SERVICE_URI + "delete/" + fileId;
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.DELETED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + "DeleteTest_FILE", listener.events.get(0).getItemPath());
        assertEquals("text/plain", listener.events.get(0).getMimeType());
    }

    public void testRename() throws Exception {
        String fileId =
                testEventsProject.createFile("RenameTest_FILE", "text/plain", new ByteArrayInputStream(DEFAULT_CONTENT.getBytes())).getId();

        String path = SERVICE_URI + "rename/" + fileId + '?' + "newname=" + "_FILE_NEW_NAME_";
        ContainerResponse response = launcher.service("POST", path, BASE_URI, null, null, null);

        assertEquals(200, response.getStatus());

        assertEquals(1, listener.events.size());
        assertEquals(ChangeEvent.ChangeType.RENAMED, listener.events.get(0).getType());
        assertEquals(testFolderPath + '/' + "_FILE_NEW_NAME_", listener.events.get(0).getItemPath());
        assertEquals("text/plain", listener.events.get(0).getMimeType());
        VirtualFileSystem vfs = listener.events.get(0).getVirtualFileSystem();
        vfs.updateItem(fileId, Collections.<Property>emptyList(), null);
    }

    public void testStartProjectUpdateListener() throws Exception {
        int configuredListeners = eventListenerList.size();
        String path = SERVICE_URI + "watch/start/" + testEventsProject.getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);

        assertEquals(204, response.getStatus());
        assertEquals("Project update listener must be added. ", configuredListeners + 1, eventListenerList.size());
    }

    public void testStopProjectUpdateListener() throws Exception {
        String path = SERVICE_URI + "watch/start/" + testEventsProject.getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());

        int configuredListeners = eventListenerList.size();
        path = SERVICE_URI + "watch/stop/" + testEventsProject.getId();
        response = launcher.service("GET", path, BASE_URI, null, null, null);

        assertEquals(204, response.getStatus());
        assertEquals("Project update listener must be removed. ", configuredListeners - 1, eventListenerList.size());
    }

    public void testProjectUpdateListener() throws Exception {
        String path = SERVICE_URI + "watch/start/" + testEventsProject.getId();
        ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null);
        assertEquals(204, response.getStatus());
        assertEquals("0", testEventsProject.getPropertyValue("vfs:lastUpdateTime"));

        String name = "testProjectUpdateListenerFolder";
        path = SERVICE_URI + "folder/" + testEventsProject.getId() + '?' + "name=" + name;
        response = launcher.service("POST", path, BASE_URI, null, null, null);
        assertEquals(200, response.getStatus());
        assertFalse("Lst update time must be changed. ", "0".equals(testEventsProject.getPropertyValue("vfs:lastUpdateTime")));
    }
}
