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
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
public class ProjectTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager pm;

    private List<String> calculateAttributeValueHolder;

    @BeforeMethod
    public void setUp() throws Exception {
        ProjectTypeDescriptionRegistry ptdr = new ProjectTypeDescriptionRegistry("test");
        final String projectType = "my_project_type";
        final String category = "my_category";
        Set<ValueProviderFactory> vpf = Collections.<ValueProviderFactory>singleton(new ValueProviderFactory() {
            @Override
            public String getName() {
                return "calculated_attribute";
            }

            @Override
            public ValueProvider newInstance(Project project) {
                return new ValueProvider() {
                    @Override
                    public List<String> getValues() {
                        return Collections.singletonList("hello");
                    }

                    @Override
                    public void setValues(List<String> value) {
                        calculateAttributeValueHolder = value;
                    }
                };
            }
        });
        ptdr.registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(new ProjectType(projectType, projectType, category));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Arrays.asList(new AttributeDescription("calculated_attribute"));
            }
        });
        final EventService eventService = new EventService();
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();

        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider("my_ws", eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
                    }
                }, vfsRegistry);
        MemoryMountPoint mmp = (MemoryMountPoint)memoryFileSystemProvider.getMountPoint(true);
        vfsRegistry.registerProvider("my_ws", memoryFileSystemProvider);
        pm = new DefaultProjectManager(ptdr, vpf, vfsRegistry, eventService);
        ((DefaultProjectManager)pm).start();
        VirtualFile myVfRoot = mmp.getRoot();
        myVfRoot.createFolder("my_project").createFolder(Constants.CODENVY_DIR).createFile(Constants.CODENVY_PROJECT_FILE, null, null);
    }

    @AfterMethod
    public void tearDown() {
        ((DefaultProjectManager)pm).stop();
    }


    @Test
    public void testGetProject() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Assert.assertNotNull(myProject);
    }

    @Test
    public void testGetProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        attributes.put("my_property_2", Arrays.asList("value_3", "value_4"));
        new ProjectJson2().withType("my_project_type").withAttributes(attributes).save(myProject);
        ProjectDescription myProjectDescription = myProject.getDescription();
        Assert.assertEquals(myProjectDescription.getProjectType().getId(), "my_project_type");
        Assert.assertEquals(myProjectDescription.getProjectType().getName(), "my_project_type");

        Assert.assertEquals(myProjectDescription.getAttributes().size(), 3);

        Assert.assertTrue(myProjectDescription.hasAttribute("calculated_attribute"));
        Attribute attribute = myProjectDescription.getAttribute("calculated_attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("hello"));

        Assert.assertTrue(myProjectDescription.hasAttribute("my_property_1"));
        attribute = myProjectDescription.getAttribute("my_property_1");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("value_1", "value_2"));

        Assert.assertTrue(myProjectDescription.hasAttribute("my_property_2"));
        attribute = myProjectDescription.getAttribute("my_property_2");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("value_3", "value_4"));
    }

    @Test
    public void testUpdateProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        ProjectJson2 projectJson = new ProjectJson2("my_project_type", attributes, null, null, "test project");
        projectJson.save(myProject);
        ProjectDescription myProjectDescription = myProject.getDescription();
        myProjectDescription.setProjectType(new ProjectType("new_project_type", "new_project_type", "new_category"));
        myProjectDescription.getAttribute("calculated_attribute").setValue("updated calculated_attribute");
        myProjectDescription.getAttribute("my_property_1").setValue("updated value 1");
        myProjectDescription.setAttributes(Arrays.asList(new Attribute("new_my_property_2", "new value 2")));

        myProject.updateDescription(myProjectDescription);

        projectJson = ProjectJson2.load(myProject);

        Assert.assertEquals(projectJson.getType(), "new_project_type");
        Assert.assertEquals(calculateAttributeValueHolder, Arrays.asList("updated calculated_attribute"));
        Map<String, List<String>> pm = projectJson.getAttributes();
        Assert.assertEquals(pm.size(), 2);
        Assert.assertEquals(pm.get("my_property_1"), Arrays.asList("updated value 1"));
        Assert.assertEquals(pm.get("new_my_property_2"), Arrays.asList("new value 2"));
    }

    @Test
    public void testModificationDate() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        long modificationDate1 = myProject.getModificationDate();
        Thread.sleep(1000);
        myProject.getBaseFolder().createFile("test.txt", "test".getBytes(), "text/plain");
        long modificationDate2 = myProject.getModificationDate();
        Assert.assertTrue(modificationDate2 > modificationDate1);
    }
}
