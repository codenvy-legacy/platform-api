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
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ValueProvider;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        ProjectTypeDescriptionRegistry ptdr = new ProjectTypeDescriptionRegistry();
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
        myVfRoot.createFolder("my_project").createFolder(Constants.CODENVY_FOLDER).createFile(Constants.CODENVY_PROJECT_FILE, null, null);
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
    public void testGetProjectModules() throws Exception {
        List<String> moduleNames = Arrays.asList("module_1", "module_2", "module_3");
        VirtualFile myProjectVirtualFile = pm.getProject("my_ws", "my_project").getBaseFolder().getVirtualFile();
        for (String name : moduleNames) {
            myProjectVirtualFile.createFolder(name)
                                .createFolder(Constants.CODENVY_FOLDER)
                                .createFile(Constants.CODENVY_PROJECT_FILE, null, null);
        }
        List<Project> modules = pm.getProject("my_ws", "my_project").getModules();
        List<String> _moduleNames = new ArrayList<>(modules.size());
        for (Project module : modules) {
            _moduleNames.add(module.getName());
        }
        Assert.assertEquals(_moduleNames.size(), moduleNames.size());
        Assert.assertTrue(moduleNames.containsAll(_moduleNames));
    }

    @Test
    public void testGetModulesWhenFoldersOnTheSameLevelExist() throws Exception {
        List<String> names = Arrays.asList("module_1", "module_2", "module_3", "module_4");
        // create two "modules" and two folders on the same level
        VirtualFile myProjectVirtualFile = pm.getProject("my_ws", "my_project").getBaseFolder().getVirtualFile();
        for (int i = 0, size = names.size(); i < size; i++) {
            String name = names.get(i);
            VirtualFile f = myProjectVirtualFile.createFolder(name);
            if ((i % 2) == 0) {
                f.createFolder(Constants.CODENVY_FOLDER).createFile(Constants.CODENVY_PROJECT_FILE, null, null);
            }
        }
        List<Project> modules = pm.getProject("my_ws", "my_project").getModules();
        List<String> _moduleNames = new ArrayList<>(modules.size());
        for (Project module : modules) {
            _moduleNames.add(module.getName());
        }
        Assert.assertEquals(_moduleNames.size(), 2);
        Assert.assertTrue(names.contains("module_2"));
        Assert.assertTrue(names.contains("module_4"));
    }

    @Test
    public void testGetProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        List<ProjectProperty> propertiesList = new ArrayList<>(2);
        propertiesList.add(new ProjectProperty("my_property_1", Arrays.asList("value_1", "value_2")));
        propertiesList.add(new ProjectProperty("my_property_2", Arrays.asList("value_3", "value_4")));
        new ProjectProperties().withType("my_project_type").withProperties(propertiesList).save(myProject);
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
        ProjectProperties properties = new ProjectProperties("my_project_type",
                                                             null,
                                                             Arrays.asList(new ProjectProperty("my_property_1",
                                                                                               Arrays.asList("value_1",
                                                                                                             "value_2"))));
        properties.save(myProject);
        ProjectDescription myProjectDescription = myProject.getDescription();
        myProjectDescription.setProjectType(new ProjectType("new_project_type", "new_project_type", "new_category"));
        myProjectDescription.getAttribute("calculated_attribute").setValue("updated calculated_attribute");
        myProjectDescription.getAttribute("my_property_1").setValue("updated value 1");
        myProjectDescription.setAttributes(Arrays.asList(new Attribute("new_my_property_2", "new value 2")));

        myProject.updateDescription(myProjectDescription);

        properties = ProjectProperties.load(myProject);

        Assert.assertEquals(properties.getType(), "new_project_type");
        Assert.assertEquals(calculateAttributeValueHolder, Arrays.asList("updated calculated_attribute"));
        Map<String, ProjectProperty> pm = new LinkedHashMap<>(2);
        for (ProjectProperty projectProperty : properties.getProperties()) {
            pm.put(projectProperty.getName(), projectProperty);
        }
        Assert.assertEquals(pm.size(), 2);
        Assert.assertEquals(pm.get("my_property_1").getValue(), Arrays.asList("updated value 1"));
        Assert.assertEquals(pm.get("new_my_property_2").getValue(), Arrays.asList("new value 2"));
    }

    @Test
    public void testCreateModule() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Project myModule = myProject
                .createModule("my_module", new ProjectDescription(new ProjectType("my_module_type", "my_module_type", "my_module_type")));
        ProjectDescription myModuleDescription = myModule.getDescription();
        Assert.assertEquals(myModuleDescription.getProjectType().getId(), "my_module_type");
        Assert.assertEquals(myModuleDescription.getProjectType().getName(), "my_module_type");
        Assert.assertEquals(myModuleDescription.getProjectType().getCategory(), "my_module_type");
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
