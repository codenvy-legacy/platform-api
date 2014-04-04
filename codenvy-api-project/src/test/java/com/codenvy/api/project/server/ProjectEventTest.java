/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
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
        ProjectTypeRegistry ptr = new ProjectTypeRegistry();
        ProjectTypeDescriptionRegistry ptdr = new ProjectTypeDescriptionRegistry(ptr);
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
        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider("my_ws", eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
                    }
                });
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
        vfsRegistry.registerProvider("my_ws", memoryFileSystemProvider);
        pm = new ProjectManager(ptr, ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry);
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
    public void testCreateFile() {
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
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testCreateFolder() {
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
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testDelete() {
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
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "test.txt");
    }

    @Test
    public void testMove() {
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
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "a/b/c/test.txt");
        Assert.assertEquals(events.get(1).getType(), ProjectEvent.EventType.DELETED);
        Assert.assertEquals(events.get(1).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(1).getProject(), "my_project");
        Assert.assertEquals(events.get(1).getPath(), "test.txt");
    }

    @Test
    public void testRename() {
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
        Assert.assertEquals(events.get(0).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(0).getProject(), "my_project");
        Assert.assertEquals(events.get(0).getPath(), "_test.txt");
        Assert.assertEquals(events.get(1).getType(), ProjectEvent.EventType.DELETED);
        Assert.assertEquals(events.get(1).getWorkspace(), "my_ws");
        Assert.assertEquals(events.get(1).getProject(), "my_project");
        Assert.assertEquals(events.get(1).getPath(), "test.txt");
    }
}
