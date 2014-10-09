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
package com.codenvy.api.project.server;

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author andrew00x
 */
public class ProjectEventTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager      pm;
    private ProjectEventService projectEventService;

    @BeforeMethod
    public void setUp() throws Exception {
        EventService eventService = new EventService();
        ProjectTypeDescriptionRegistry ptdr = new ProjectTypeDescriptionRegistry("test");
        ptdr.registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(new ProjectType("my_project_type", "my project type", "my_category"));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Collections.emptyList();
            }
        });

        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider("my_ws", eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
                    }
                }, vfsRegistry);

        vfsRegistry.registerProvider("my_ws", memoryFileSystemProvider);
        pm = new DefaultProjectManager(ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry, eventService);
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_project_type", "my project type", "my_category"));
        pd.setDescription("my test project");
        pd.setAttributes(Arrays.asList(new Attribute("my_attribute", "attribute value 1")));
        pm.createProject("my_ws", "my_project", pd);

        projectEventService = new ProjectEventService(eventService);
    }


    @Test
    public void testAddListener() {
        ProjectEventListener listener = new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
            }
        };
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", listener));
        Assert.assertFalse(projectEventService.addListener("my_ws", "my_project", listener));
    }


    @Test
    public void testRemoveListener() {
        ProjectEventListener listener = new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
            }
        };
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", listener));
        Assert.assertTrue(projectEventService.removeListener("my_ws", "my_project", listener));
        Assert.assertFalse(projectEventService.removeListener("my_ws", "my_project", listener));
    }

    @Test
    public void testCreateFile() throws Exception {
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.CREATED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testCreateFolder() throws Exception {
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        pm.getProject("my_ws", "my_project").getBaseFolder().createFolder("a/b/c");
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.CREATED);
        Assert.assertTrue(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "a/b/c");
    }

    @Test
    public void testUpdateFile() throws Exception {
        FileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        file.updateContent("new content".getBytes());
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.UPDATED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testDelete() throws Exception {
        FileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        file.remove();
        Assert.assertEquals(events.size(), 1);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.DELETED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testMove() throws Exception {
        FileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        FolderEntry folder = pm.getProject("my_ws", "my_project").getBaseFolder().createFolder("a/b/c");
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        file.moveTo(folder.getPath());
        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.CREATED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "a/b/c/test.txt");
        Assert.assertEquals(events.get(1).getType(), ProjectEvent.EventType.DELETED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(1).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(1).getProject(), "my_project");
        Assert.assertEquals(events.get(1).getPath(), "test.txt");
    }

    @Test
    public void testRename() throws Exception {
        FileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        final List<ProjectEvent> events = new ArrayList<>();
        Assert.assertTrue(projectEventService.addListener("my_ws", "my_project", new ProjectEventListener() {
            @Override
            public void onEvent(ProjectEvent event) {
                events.add(event);
            }
        }));
        file.rename("_test.txt");
        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0).getType(), ProjectEvent.EventType.CREATED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "_test.txt");
        Assert.assertEquals(events.get(1).getType(), ProjectEvent.EventType.DELETED);
        Assert.assertFalse(events.get(0).isFolder());
        Assert.assertEquals(events.get(1).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(1).getProject(), "my_project");
        Assert.assertEquals(events.get(1).getPath(), "test.txt");
    }
}
