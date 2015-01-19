/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.server;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.project.server.handlers.ProjectHandler;
import com.codenvy.api.project.server.handlers.ProjectHandlerRegistry;
import com.codenvy.api.project.server.type.AttributeValue;
import com.codenvy.api.project.server.type.ProjectType2;
import com.codenvy.api.project.server.type.ProjectTypeRegistry;
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

import java.util.*;

/**
 * @author andrew00x
 */
public class ProjectTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager pm;

    private static List<String> calculateAttributeValueHolder = Collections.singletonList("hello");

    @BeforeMethod
    public void setUp() throws Exception {

        final ValueProviderFactory vpf1 = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) {

                        return calculateAttributeValueHolder;
                        //Collections.singletonList("hello");
                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) {

                        calculateAttributeValueHolder = value;
                    }
                };
            }
        };


        ProjectType2 pt = new ProjectType2("my_project_type", "my project type") {

            {
                addVariableDefinition("calculated_attribute", "attr description", true, vpf1);
                addVariableDefinition("my_property_1", "attr description", true);
                addVariableDefinition("my_property_2", "attr description", false);
                setDefaultBuilder("builder1");
                setDefaultRunner("system:/runner/runner1");
            }

        };

        Set <ProjectType2> types = new HashSet<ProjectType2>();
        types.add(pt);
        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(types);

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


        //ProjectGeneratorRegistry pgRegistry = new ProjectGeneratorRegistry(new HashSet<ProjectGenerator>());

        ProjectHandlerRegistry phRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());

        pm = new DefaultProjectManager(vfsRegistry, eventService, ptRegistry, phRegistry);

        ((DefaultProjectManager)pm).start();
        VirtualFile myVfRoot = mmp.getRoot();
        myVfRoot.createFolder("my_project").createFolder(Constants.CODENVY_DIR).createFile(Constants.CODENVY_PROJECT_FILE, null, null);
//        myVfRoot.createFolder("testEstimateProject");
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
        Map<String, List<String>> attributes = new HashMap<>(3);
        //attributes.put("calculated_attribute", Arrays.asList("hello"));
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        attributes.put("my_property_2", Arrays.asList("value_3", "value_4"));
        ProjectJson2 json = new ProjectJson2();
        json.withType("my_project_type").withAttributes(attributes).save(myProject);
        //ProjectDescription myProjectDescription = myProject.getDescription();

        //System.out.println("JSON >> "+json.getAttributes());

        //System.out.println(">>    >>"+pm.getValueProviderFactories());

        ProjectConfig myConfig = myProject.getConfig();
        Assert.assertEquals(myConfig.getTypeId(), "my_project_type");
        //Assert.assertEquals(myProjectDescription.getProjectType().getName(), "my_project_type");

        Assert.assertEquals(pm.getProjectTypeRegistry().getProjectType("my_project_type").getAttributes().size(), 4);


        //System.out.println(">>>>"+myConfig.getAttribute("calculated_attribute"));

        Assert.assertEquals(myConfig.getAttributes().size(), 3);

        AttributeValue attributeVal;

        Assert.assertNotNull(myConfig.getAttributes().get("calculated_attribute"));
        attributeVal = myConfig.getAttributes().get("calculated_attribute");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("hello"));

        Assert.assertNotNull(myConfig.getAttributes().get("my_property_1"));
        attributeVal = myConfig.getAttributes().get("my_property_1");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("value_1", "value_2"));

        Assert.assertNotNull(myConfig.getAttributes().get("my_property_2"));
        attributeVal = myConfig.getAttributes().get("my_property_2");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("value_3", "value_4"));
    }

    @Test
    public void testUpdateProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        ProjectJson2 projectJson = new ProjectJson2("my_project_type", attributes, null, null, "test project");
        projectJson.save(myProject);

        Map <String, AttributeValue> attrs = new HashMap<>();
        attrs.put("calculated_attribute", new AttributeValue("updated calculated_attribute"));
        attrs.put("my_property_1", new AttributeValue("updated value 1"));
        attrs.put("new_my_property_2", new AttributeValue("new value 2"));

        ProjectConfig myConfig = new ProjectConfig("descr", "my_project_type", attrs, null, null, null);

        myProject.updateConfig(myConfig);

        projectJson = ProjectJson2.load(myProject);

        Assert.assertEquals(projectJson.getType(), "my_project_type");
        Assert.assertEquals(calculateAttributeValueHolder, Arrays.asList("updated calculated_attribute"));
        Map<String, List<String>> pm = projectJson.getAttributes();
        Assert.assertEquals(pm.size(), 2);
        Assert.assertEquals(pm.get("my_property_1"), Arrays.asList("updated value 1"));

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

    @Test
    public void testIfDefaultBuilderRunnerAppearsInProject() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        ProjectJson2 projectJson = new ProjectJson2("my_project_type", attributes, null , null, "test project");
        projectJson.save(myProject);

        Assert.assertNotNull(myProject.getConfig().getRunners());
        Assert.assertEquals(myProject.getConfig().getRunners().getDefault(), "system:/runner/runner1");

        Assert.assertNotNull(myProject.getConfig().getBuilders());
        Assert.assertEquals(myProject.getConfig().getBuilders().getDefault(), "builder1");
    }

    @Test
    public void testEstimateProject() throws Exception {

        VirtualFile root = pm.getVirtualFileSystemRegistry().getProvider("my_ws").getMountPoint(false).getRoot();
        root.createFolder("testEstimateProjectGood").createFolder("check");
        root.createFolder("testEstimateProjectBad");

        final ValueProviderFactory vpf1 = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = null;
                        try {
                            file = projectFolder.getChild("check");
                       } catch (ForbiddenException e) {
                            throw new ValueStorageException(e.getMessage());
                        } catch (ServerException e) {
                            throw new ValueStorageException(e.getMessage());
                        }

                        if(file == null)
                            throw new ValueStorageException("Check not found");
                       return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) {

                        //calculateAttributeValueHolder = value;
                    }
                };
            }
        };


        ProjectType2 pt = new ProjectType2("testEstimateProjectPT", "my testEstimateProject type") {

            {
                addVariableDefinition("calculated_attribute", "attr description", true, vpf1);
                addVariableDefinition("my_property_1", "attr description", true);
                addVariableDefinition("my_property_2", "attr description", false);
                setDefaultBuilder("builder1");
                setDefaultRunner("system:/runner/runner1");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);

        Map<String, AttributeValue> attrs = pm.estimateProject("my_ws", "testEstimateProjectGood", "testEstimateProjectPT");
        Assert.assertEquals(attrs.size(), 1);
        Assert.assertNotNull(attrs.get("calculated_attribute"));
        Assert.assertEquals(attrs.get("calculated_attribute").getString(), "checked");


        try {
            pm.estimateProject("my_ws", "testEstimateProjectBad", "testEstimateProjectPT");
            Assert.fail("ValueStorageException should be thrown");
        } catch (ValueStorageException e) {

        }
    }


    @Test
    public void testPTConstraints() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testMixinAndPrimary", "my type", true, true) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testPrimary", "my type", true, false) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testMixin", "my type", false, true) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testAbstract", "my type", false, false) {});

        pm.createProject("my_ws", "all", new ProjectConfig("proj", "testMixinAndPrimary"), null, null);
        pm.createProject("my_ws", "prim", new ProjectConfig("proj", "testPrimary"), null, null);


        // not possible to create Project with wrong PT
        try {
            pm.createProject("my_ws", "mix", new ProjectConfig("proj", "testMixin"), null, null);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) { }
        try {
            pm.createProject("my_ws", "abstr", new ProjectConfig("proj", "testAbstract"), null, null);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) {  }

        ProjectConfig config = pm.getProject("my_ws", "all").getConfig();
        config.getMixinTypes().add("testMixin");
        pm.getProject("my_ws", "all").updateConfig(config);

        // not possible to add wrong mixin PT
        config.getMixinTypes().add("testAbstract");
        try {
            pm.getProject("my_ws", "all").updateConfig(config);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) {}


    }

    @Test
    public void testAddMixin() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testPrimary", "my type", true, false) {

            {
                addConstantDefinition("c1", "","c1");
            }

        });
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testMixin", "my type", false, true) {
            {
                addConstantDefinition("m1", "","m1");
            }
        });

        pm.createProject("my_ws", "p1", new ProjectConfig("proj", "testPrimary"), null, null);
//        pm.createProject("my_ws", "p2", new ProjectConfig("proj", "testPrimary"), null, null);


        ProjectConfig config = pm.getProject("my_ws", "p1").getConfig();
        Assert.assertEquals(config.getMixinTypes().size(), 0);
        Assert.assertEquals(config.getAttributes().size(), 1);
        config.getMixinTypes().add("testMixin");
        pm.getProject("my_ws", "p1").updateConfig(config);
        config = pm.getProject("my_ws", "p1").getConfig();
        Assert.assertEquals(config.getMixinTypes().size(), 1);
        Assert.assertEquals("testMixin", config.getMixinTypes().get(0));
        Assert.assertEquals(config.getAttributes().size(), 2);


        // add same mixin as existed
        config.getMixinTypes().add("testMixin");
        try {
            pm.getProject("my_ws", "p1").updateConfig(config);
            Assert.fail("ProjectTypeConstraintException (duplicated attributes) expected");
        } catch (ProjectTypeConstraintException e) { }
        //config = pm.getProject("my_ws", "p1").getConfig();
        //Assert.assertEquals(config.getMixinTypes().size(), 1);

    }


    @Test
    public void testAddMixinWithProvidedAttrs() throws Exception {

        final ValueProviderFactory vpfPrimary = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = checkFolder();

                        if(file == null)
                            throw new ValueStorageException("Primary folder not found");
                        return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) throws ValueStorageException {
                        if(checkFolder() == null) {
                            try {
                                projectFolder.createFolder("primary");
                            } catch (Exception e) {
                                throw new ValueStorageException(e.getMessage());
                            }
                        }

                    }

                    private VirtualFileEntry checkFolder() throws ValueStorageException {
                        VirtualFileEntry file = null;
                        try {
                            file = projectFolder.getChild("primary");
                        } catch (Exception e) {
                            throw new ValueStorageException(e.getMessage());
                        }
                        return file;
                    }


                };
            }
        };


        final ValueProviderFactory vpfMixin = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = checkFolder();

                        if(file == null)
                            throw new ValueStorageException("Mixin folder not found");
                        return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) throws ValueStorageException {
                        if(checkFolder() == null) {
                            try {
                                projectFolder.createFolder("mixin");
                            } catch (Exception e) {
                                throw new ValueStorageException(e.getMessage());
                            }
                        }
                    }

                    private VirtualFileEntry checkFolder() throws ValueStorageException {
                        VirtualFileEntry file = null;
                        try {
                            file = projectFolder.getChild("mixin");
                        } catch (Exception e) {
                            throw new ValueStorageException(e.getMessage());
                        }
                        return file;
                    }


                };
            }
        };


        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testPrimary", "my type", true, false) {

            {
                addVariableDefinition("p.calculate", "", true, vpfPrimary);
            }

        });
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType2("testMixin", "my type", false, true) {
            {
                addVariableDefinition("m.calculate", "", true, vpfMixin);
            }
        });

        Map <String, AttributeValue> attrs = new HashMap<>();
        attrs.put("p.calculate", new AttributeValue(""));
        ProjectConfig config = new ProjectConfig("proj", "testPrimary", attrs, null, null, null);
        Project proj = pm.createProject("my_ws", "provided", config , null, null);

        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 0);
        Assert.assertEquals(proj.getConfig().getAttributes().get("p.calculate").getString(), "checked");

        config.getMixinTypes().add("testMixin");
        config.getAttributes().put("m.calculate", new AttributeValue(""));
        proj.updateConfig(config);

        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 1);
        Assert.assertEquals(proj.getConfig().getAttributes().get("m.calculate").getString(), "checked");

        // reread it in case
        proj = pm.getProject("my_ws", "provided");
        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 1);
        Assert.assertEquals(proj.getConfig().getAttributes().get("p.calculate").getString(), "checked");
        Assert.assertEquals(proj.getConfig().getAttributes().get("m.calculate").getString(), "checked");


    }

}

