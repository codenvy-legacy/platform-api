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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
public class ProjectServiceTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager   pm;
    private ResourceLauncher launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        ProjectTypeRegistry ptr = new ProjectTypeRegistry();
        ProjectTypeDescriptionRegistry ptdr = new ProjectTypeDescriptionRegistry(ptr);
        ptdr.registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(new ProjectType("my_project_type", "my project type"));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Collections.emptyList();
            }
        });
        MemoryMountPoint mmp = new MemoryMountPoint(null, new VirtualFileSystemUserContext() {
            @Override
            public VirtualFileSystemUser getVirtualFileSystemUser() {
                return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
            }
        });
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();
        vfsRegistry.registerProvider("my_ws", new MemoryFileSystemProvider("my_ws", mmp));
        pm = new ProjectManager(ptr, ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry);
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_project_type", "my project type"));
        pd.setDescription("my test project");
        pd.setAttributes(Arrays.asList(new Attribute("my_attribute", "attribute value 1")));
        pm.createProject("my_ws", "my_project", pd);

        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(ProjectManager.class, pm);
        dependencies.addComponent(ProjectTypeRegistry.class, ptr);
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        providers.addExceptionMapper(new ProjectNotFoundExceptionMapper());
        providers.addExceptionMapper(new InvalidProjectTypeExceptionMapper());
        RequestHandler requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                               providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        resources.addResource(ProjectService.class, null);

        launcher = new ResourceLauncher(requestHandler);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetProjects() throws Exception {
        ContainerResponse response =
                launcher.service("GET", "http://localhost:8080/api/project/my_ws", "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ProjectReference> result = (List<ProjectReference>)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        ProjectReference projectReference = result.get(0);
        Assert.assertEquals(projectReference.getName(), "my_project");
        Assert.assertEquals(projectReference.getUrl(), "http://localhost:8080/api/project/my_ws/my_project");
        Assert.assertEquals(projectReference.getDescription(), "my test project");
        Assert.assertEquals(projectReference.getWorkspace(), "my_ws");
        Assert.assertEquals(projectReference.getProjectTypeId(), "my_project_type");
        Assert.assertEquals(projectReference.getProjectTypeName(), "my project type");
        Assert.assertEquals(projectReference.getVisibility(), "public");
    }

    @Test
    public void testGetProject() throws Exception {
        ContainerResponse response = launcher.service("GET", "http://localhost:8080/api/project/my_ws/my_project",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "my test project");
        Assert.assertEquals(result.getProjectTypeId(), "my_project_type");
        Assert.assertEquals(result.getProjectTypeName(), "my project type");
        Assert.assertEquals(result.getVisibility(), "public");
        Assert.assertTrue(result.getModificationDate() > 0);
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("my_attribute"), Arrays.asList("attribute value 1"));
    }

    @Test
    public void testGetModule() throws Exception {
        pm.getTypeDescriptionRegistry().registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(new ProjectType("my_module_type", "my module type"));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Collections.emptyList();
            }
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_module_type", "my module type"));
        pd.setDescription("my test module");
        pd.setAttributes(Arrays.asList(new Attribute("my_module_attribute", "attribute value 1")));
        myProject.createModule("my_module", pd);

        ContainerResponse response = launcher.service("GET", "http://localhost:8080/api/project/my_ws/my_project/my_module",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "my test module");
        Assert.assertEquals(result.getProjectTypeId(), "my_module_type");
        Assert.assertEquals(result.getProjectTypeName(), "my module type");
        Assert.assertEquals(result.getVisibility(), "public");
        Assert.assertTrue(result.getModificationDate() > 0);
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("my_module_attribute"), Arrays.asList("attribute value 1"));
    }

    @Test
    public void testGetProjectInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("GET", "http://localhost:8080/api/project/my_ws/my_project_invalid",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 404);
    }

    @Test
    public void testCreateProject() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withProjectTypeId("my_project_type")
                                                 .withDescription("new project")
                                                 .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws?name=new_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200);
        Project project = pm.getProject("my_ws", "new_project");
        Assert.assertNotNull(project);
        ProjectDescription description = project.getDescription();

        Assert.assertEquals(description.getDescription(), "new project");
        Assert.assertEquals(description.getProjectType().getId(), "my_project_type");
        Assert.assertEquals(description.getProjectType().getName(), "my project type");
        Attribute attribute = description.getAttribute("new project attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));
    }

    @Test
    public void testCreateModule() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new module attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withProjectTypeId("my_project_type")
                                                 .withDescription("new module")
                                                 .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/my_project?name=new_module",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200);
        Project project = pm.getProject("my_ws", "my_project/new_module");
        Assert.assertNotNull(project);
        ProjectDescription description = project.getDescription();

        Assert.assertEquals(description.getDescription(), "new module");
        Assert.assertEquals(description.getProjectType().getId(), "my_project_type");
        Assert.assertEquals(description.getProjectType().getName(), "my project type");
        Attribute attribute = description.getAttribute("new module attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));
    }

    @Test
    public void testCreateProjectInvalidProjectType() throws Exception {
        final String newProjectTypeId = "new_project_type";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withProjectTypeId(newProjectTypeId)
                                                 .withDescription("new project")
                                                 .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws?name=new_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 400);
        Project project = pm.getProject("my_ws", "new_project");
        Assert.assertNull(project);
    }

    @Test
    public void testUpdateProject() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withProjectTypeId("my_project_type")
                                                 .withDescription("updated project")
                                                 .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT", "http://localhost:8080/api/project/my_ws/my_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200);
        Project project = pm.getProject("my_ws", "my_project");
        Assert.assertNotNull(project);
        ProjectDescription description = project.getDescription();

        Assert.assertEquals(description.getDescription(), "updated project");
        Assert.assertEquals(description.getProjectType().getId(), "my_project_type");
        Assert.assertEquals(description.getProjectType().getName(), "my project type");
        Attribute attribute = description.getAttribute("my_attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));
    }

    @Test
    public void testUpdateProjectInvalidPath() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withProjectTypeId("my_project_type")
                                                 .withDescription("updated project")
                                                 .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT", "http://localhost:8080/api/project/my_ws/my_project_invalid",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 404);
    }

    @Test
    public void testCreateFile() throws Exception {
        String myContent = "to be or not to be";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("text/plain"));
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/file/my_project?name=test.txt",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      myContent.getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 201);
        VirtualFileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test.txt");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), "text/plain");
        Assert.assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testGetFileContent() throws Exception {
        String myContent = "to be or not to be";
        pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", myContent.getBytes(), "text/plain");
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("GET", "http://localhost:8080/api/project/my_ws/file/my_project/test.txt",
                                                      "http://localhost:8080/api", null, null, writer, null);
        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(response.getContentType().toString(), "text/plain");
        Assert.assertEquals(new String(writer.getBody()), myContent);
    }

    @Test
    public void testUpdateFileContent() throws Exception {
        String myContent = "<test>hello</test>";
        pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test", "to be or not to be".getBytes(), "text/plain");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Arrays.asList("text/xml"));
        ContainerResponse response = launcher.service("PUT", "http://localhost:8080/api/project/my_ws/file/my_project/test",
                                                      "http://localhost:8080/api", headers, myContent.getBytes(), null);
        Assert.assertEquals(response.getStatus(), 200);
        VirtualFileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), "text/xml");
        Assert.assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testCreateFolder() throws Exception {
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/folder/my_project/test",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        VirtualFileEntry folder = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testCreatePath() throws Exception {
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/folder/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        VirtualFileEntry folder = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("a/b/c");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testDeleteFile() throws Exception {
        pm.getProject("my_ws", "my_project").getBaseFolder().createFile("test.txt", "to be or not to be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("DELETE", "http://localhost:8080/api/project/my_ws/my_project/test.txt",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Assert.assertNull(pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testDeleteFolder() throws Exception {
        pm.getProject("my_ws", "my_project").getBaseFolder().createFolder("test");
        ContainerResponse response = launcher.service("DELETE", "http://localhost:8080/api/project/my_ws/my_project/test",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Assert.assertNull(pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test"));
    }

    @Test
    public void testDeletePath() throws Exception {
        pm.getProject("my_ws", "my_project").getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("DELETE", "http://localhost:8080/api/project/my_ws/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Assert.assertNull(pm.getProject("my_ws", "my_project").getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testDeleteInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("DELETE", "http://localhost:8080/api/project/my_ws/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 404);
        Assert.assertNotNull(pm.getProject("my_ws", "my_project"));
    }

    @Test
    public void testDeleteProject() throws Exception {
        ContainerResponse response = launcher.service("DELETE", "http://localhost:8080/api/project/my_ws/my_project",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Assert.assertNull(pm.getProject("my_ws", "my_project"));
    }

    @Test
    public void testCopyFile() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/copy/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testCopyFolder() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/copy/my_project/a/b?to=/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/b/test.txt"));
    }

    @Test
    public void testMoveFile() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/move/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testMoveFolder() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b/c")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/move/my_project/a/b/c?to=/my_project/a",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c"));
    }
}
