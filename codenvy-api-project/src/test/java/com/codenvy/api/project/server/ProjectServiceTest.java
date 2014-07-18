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
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.TreeElement;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.vfs.server.ContentStreamWriter;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.user.UserImpl;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.everrest.test.mock.MockHttpServletRequest;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class ProjectServiceTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager           pm;
    private ResourceLauncher         launcher;
    private ProjectImporterRegistry  importerRegistry;
    private ProjectGeneratorRegistry generatorRegistry;

    @Mock
    private UserDao userDao;

    @BeforeMethod
    public void setUp() throws Exception {
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
        pm = new DefaultProjectManager(ptr, ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry, eventService);
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_project_type", "my project type", "my_category"));
        pd.setDescription("my test project");
        pd.setAttributes(Arrays.asList(new Attribute("my_attribute", "attribute value 1")));
        pm.createProject("my_ws", "my_project", pd);

        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        importerRegistry = new ProjectImporterRegistry(Collections.<ProjectImporter>emptySet());
        generatorRegistry = new ProjectGeneratorRegistry(Collections.<ProjectGenerator>emptySet());
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(ProjectManager.class, pm);
        dependencies.addComponent(ProjectTypeRegistry.class, ptr);
        dependencies.addComponent(ProjectImporterRegistry.class, importerRegistry);
        dependencies.addComponent(ProjectGeneratorRegistry.class, generatorRegistry);
        dependencies.addComponent(SearcherProvider.class, mmp.getSearcherProvider());
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        providers.addMessageBodyWriter(new ContentStreamWriter());
        providers.addExceptionMapper(new FileSystemLevelExceptionMapper());
        providers.addExceptionMapper(new ProjectStructureConstraintExceptionMapper());
        RequestHandler requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                               providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        resources.addResource(ProjectService.class, null);
        // for log events
        com.codenvy.commons.env.EnvironmentContext env = com.codenvy.commons.env.EnvironmentContext.getCurrent();
        env.setUser(new UserImpl(vfsUserName, vfsUserName, "dummy_token", vfsUserGroups));
        env.setWorkspaceName("my_ws");
        env.setWorkspaceId("my_ws");
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
        Assert.assertEquals(projectReference.getWorkspaceId(), "my_ws");
        Assert.assertEquals(projectReference.getProjectTypeId(), "my_project_type");
        Assert.assertEquals(projectReference.getProjectTypeName(), "my project type");
        Assert.assertEquals(projectReference.getVisibility(), "public");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetModules() throws Exception {
        pm.getTypeDescriptionRegistry().registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(new ProjectType("my_module_type", "my module type", "my_category"));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Collections.emptyList();
            }
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_module_type", "my module type", "my_category"));
        pd.setDescription("my test module");
        pd.setAttributes(Arrays.asList(new Attribute("my_module_attribute", "attribute value 1")));
        myProject.createModule("my_module", pd);

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/modules/my_project",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ProjectDescriptor> result = (List<ProjectDescriptor>)response.getEntity();
        Assert.assertNotNull(result);

        Assert.assertEquals(result.size(), 1);
        ProjectDescriptor projectDescriptor_1 = result.get(0);
        Assert.assertEquals(projectDescriptor_1.getDescription(), "my test module");
        Assert.assertEquals(projectDescriptor_1.getProjectTypeId(), "my_module_type");
        Assert.assertEquals(projectDescriptor_1.getProjectTypeName(), "my module type");
        Assert.assertEquals(projectDescriptor_1.getVisibility(), "public");
        Map<String, List<String>> attributes = projectDescriptor_1.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("my_module_attribute"), Arrays.asList("attribute value 1"));
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
                return Arrays.asList(new ProjectType("my_module_type", "my module type", "my_category"));
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return Collections.emptyList();
            }
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_module_type", "my module type", "my_category"));
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
        headers.put("Content-Type", Arrays.asList("application/json"));
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
        headers.put("Content-Type", Arrays.asList("application/json"));
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
        headers.put("Content-Type", Arrays.asList("application/json"));
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
        headers.put("Content-Type", Arrays.asList("application/json"));
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
        headers.put("Content-Type", Arrays.asList("application/json"));
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
        headers.put("Content-Type", Arrays.asList("text/plain"));
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/file/my_project?name=test.txt",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      myContent.getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/file/my_project/test.txt"));
        AbstractVirtualFileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test.txt");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), "text/plain");
        Assert.assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testUploadFile() throws Exception {
        String fileContent = "to be or not to be";
        String fileName = "test.txt";
        String fileMediaType = "text/plain";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("multipart/form-data; boundary=abcdef"));
        String uploadBodyPattern =
                "--abcdef\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%1$s\"\r\nContent-Type: %2$s\r\n\r\n%3$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"mimeType\"\r\n\r\n%4$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n%5$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\n%6$b"
                + "\r\n--abcdef--\r\n";
        byte[] formData = String.format(uploadBodyPattern, fileName, fileMediaType, fileContent, fileMediaType, fileName, false).getBytes();
        EnvironmentContext env = new EnvironmentContext();
        env.put(HttpServletRequest.class, new MockHttpServletRequest("", new ByteArrayInputStream(formData),
                                                                     formData.length, "POST", headers));
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/uploadFile/my_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      formData,
                                                      env);
        Assert.assertEquals(response.getStatus(), 200);
        AbstractVirtualFileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().getChild(fileName);
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), fileMediaType);
        Assert.assertEquals(new String(_file.contentAsBytes()), fileContent);
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
        headers.put("Content-Type", Arrays.asList("text/xml"));
        ContainerResponse response = launcher.service("PUT", "http://localhost:8080/api/project/my_ws/file/my_project/test",
                                                      "http://localhost:8080/api", headers, myContent.getBytes(), null);
        Assert.assertEquals(response.getStatus(), 200);
        AbstractVirtualFileEntry file = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test");
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
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/test"));
        AbstractVirtualFileEntry folder = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testCreatePath() throws Exception {
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws/folder/my_project/a/b/c",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/a/b/c"));
        AbstractVirtualFileEntry folder = pm.getProject("my_ws", "my_project").getBaseFolder().getChild("a/b/c");
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
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/file/my_project/a/b/c/test.txt"));
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
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/a/b/c/b"));
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
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/file/my_project/a/b/c/test.txt"));
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
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/a/c"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testRenameFile() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/rename/my_project/test.txt?name=_test.txt",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/file/my_project/_test.txt"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("_test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFileAndUpdateMediaType() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/*");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/rename/my_project/test.txt?name=_test.txt&mediaType=text/plain",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/file/my_project/_test.txt"));
        FileEntry renamed = (FileEntry)myProject.getBaseFolder().getChild("_test.txt");
        Assert.assertNotNull(renamed);
        Assert.assertEquals(renamed.getMediaType(), "text/plain");
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFolder() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/rename/my_project/a/b?name=x",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/a/x"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/x"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/x/c"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b"));
    }

    @Test
    public void testImportProject() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_FOLDER + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"my_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"properties\":[{\"name\":\"x\",\"value\":[\"a\",\"b\"]}]}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location) throws IOException {
                // Don't really use location in this test.
                try {
                    baseFolder.getVirtualFile().unzip(zip, true);
                } catch (VirtualFileSystemException e) {
                    throw new IOException(e.getMessage(), e);
                }
                folderHolder.set(baseFolder);
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"type\":\"%s\"}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/import/new_project",
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getDescription(), "import test");
        Assert.assertEquals(descriptor.getProjectTypeId(), "my_project_type");
        Assert.assertEquals(descriptor.getProjectTypeName(), "my project type");
        Assert.assertEquals(descriptor.getAttributes().get("x"), Arrays.asList("a", "b"));

        Project newProject = pm.getProject("my_ws", "new_project");
        Assert.assertNotNull(newProject);
    }

    @Test
    public void testImportZip() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        byte[] zip = bout.toByteArray();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/zip"));
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/import/my_project/a/b",
                                                      "http://localhost:8080/api", headers, zip, null);
        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create("http://localhost:8080/api/project/my_ws/children/my_project/a/b"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/file1.txt"));
    }

    @Test
    public void testGenerate() throws Exception {
        pm.createProject("my_ws", "new_project",
                         new ProjectDescription(new ProjectType("my_project_type", "my project type", "my_category")));
        generatorRegistry.register(new ProjectGenerator() {
            @Override
            public String getId() {
                return "my_generator";
            }

            @Override
            public void generateProject(FolderEntry baseFolder, Map<String, String> options) throws IOException {
                baseFolder.createFolder("a");
                baseFolder.createFolder("b");
                baseFolder.createFile("test.txt", "test".getBytes(), "text/plain");
            }
        });
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/generate/new_project?generator=my_generator",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getProjectTypeId(), "my_project_type");
        Assert.assertEquals(descriptor.getProjectTypeName(), "my project type");
        Project newProject = pm.getProject("my_ws", "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getBaseFolder().getChild("a"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("b"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testExportZip() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/export/my_project",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(response.getContentType().toString(), "application/zip");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChildren() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/children/my_project/a",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 2);
        Set<String> names = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            names.add(itemReference.getName());
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("test.txt"));
    }

    @Test
    public void testGetTree() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/tree/my_project/a",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        TreeElement tree = (TreeElement)response.getEntity();
        Assert.assertEquals(tree.getNode().getName(), "a");
        List<TreeElement> children = tree.getChildren();
        Assert.assertNotNull(children);
        Assert.assertEquals(children.size(), 2);
        Set<String> names = new LinkedHashSet<>(2);
        for (TreeElement subTree : children) {
            names.add(subTree.getNode().getName());
            Assert.assertTrue(subTree.getChildren().isEmpty()); // default depth is 1
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
    }

    @Test
    public void testGetTreeWithDepth() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/tree/my_project/a?depth=2",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        TreeElement tree = (TreeElement)response.getEntity();
        Assert.assertEquals(tree.getNode().getName(), "a");
        List<TreeElement> children = tree.getChildren();
        Assert.assertNotNull(children);
        Set<String> names = new LinkedHashSet<>(4);
        for (TreeElement subTree : children) {
            String name = subTree.getNode().getName();
            names.add(name);
            for (TreeElement subSubTree : subTree.getChildren()) {
                names.add(name + "/" + subSubTree.getNode().getName());
            }
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
        Assert.assertTrue(names.contains("b/c"));
        Assert.assertTrue(names.contains("x/y"));
    }

    @Test
    public void testSwitchProjectVisibilityToPrivate() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/switch_visibility/my_project?visibility=private",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Map<Principal, Set<BasicPermissions>> permissions = myProject.getBaseFolder().getVirtualFile().getPermissions();
        Assert.assertEquals(permissions.size(), 1);
        Principal principal = DtoFactory.getInstance().createDto(Principal.class)
                                        .withName("workspace/developer")
                                        .withType(Principal.Type.GROUP);
        Assert.assertEquals(permissions.get(principal), Arrays.asList(BasicPermissions.ALL));

        response = launcher.service("GET",
                                    "http://localhost:8080/api/project/my_ws/my_project",
                                    "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getVisibility(), "private");
    }

    @Test
    public void testUpdateProjectVisibilityToPublic() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.setVisibility("private");
        ContainerResponse response = launcher.service("POST",
                                                      "http://localhost:8080/api/project/my_ws/switch_visibility/my_project?visibility=public",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204);
        Map<Principal, Set<BasicPermissions>> permissions =
                myProject.getBaseFolder().getVirtualFile().getPermissions();
        Assert.assertEquals(permissions.size(), 0);

        response = launcher.service("GET",
                                    "http://localhost:8080/api/project/my_ws/my_project",
                                    "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getVisibility(), "public");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByName() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("exclude", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/search/my_project?name=test.txt",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 2);
        Set<String> paths = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/a/b/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/x/y/test.txt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByText() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("__test.txt", "searchhit".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("_test", "searchhit".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/search/my_project?text=searchhit",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 2);
        Set<String> paths = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/x/y/__test.txt"));
        Assert.assertTrue(paths.contains("/my_project/c/_test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByMediaType() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "6769675".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "132434".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("test", "2343124".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/search/my_project?mediatype=text/plain",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 3);
        Set<String> paths = new LinkedHashSet<>(3);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/x/y/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/a/b/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/c/test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByNameAndTextAndMediaType() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/*");
        myProject.getBaseFolder().createFolder("c").createFile("test", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/search/my_project?text=test&name=test&mediatype=text/plain",
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200);
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getPath().equals("/my_project/c/test"));
    }

    @Test
    public void testSetBasicPermissions() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.setVisibility("private");
        clearAcl(myProject);
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("all"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUserName)
                                                                      .withType(Principal.Type.USER));
        launcher.service("POST",
                         "http://localhost:8080/api/project/my_ws/permissions/my_project",
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        Assert.assertEquals(acl.size(), 1);
        Assert.assertTrue(acl.contains(entry));
    }

    @Test
    public void testSetCustomPermissions() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.setVisibility("private");
        clearAcl(myProject);
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("custom"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUserName)
                                                                      .withType(Principal.Type.USER));
        launcher.service("POST",
                         "http://localhost:8080/api/project/my_ws/permissions/my_project",
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        Assert.assertEquals(acl.size(), 0);
        ProjectMisc misc = pm.getProjectMisc("my_ws", "my_project");
        Assert.assertEquals(misc.getAccessControlEntry(entry.getPrincipal()), entry);
    }

    @Test
    public void testSetBothBasicAndCustomPermissions() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.setVisibility("private");
        clearAcl(myProject);
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("build", "run", "update_acl", "read", "write"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUserName)
                                                                      .withType(Principal.Type.USER));
        launcher.service("POST",
                         "http://localhost:8080/api/project/my_ws/permissions/my_project",
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry)).getBytes(),
                         null
                        );

        //check project ACL it should contain entry with "read" and "write" permissions
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        Assert.assertEquals(acl.size(), 1);
        AccessControlEntry expected = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("read", "write", "update_acl"))
                                                .withPrincipal(entry.getPrincipal());
        Assert.assertTrue(acl.contains(expected));

        //check project misc it should contain entry with "run" and "build" permissions
        expected.setPermissions(Arrays.asList("run", "build"));
        ProjectMisc misc = pm.getProjectMisc("my_ws", myProject.getBaseFolder().getPath());
        AccessControlEntry actual = misc.getAccessControlEntry(entry.getPrincipal());
        Assert.assertEquals(actual.getPrincipal(), expected.getPrincipal());
        Assert.assertEquals(actual.getPermissions().size(), expected.getPermissions().size());
        for (String expectedPermission : expected.getPermissions()) {
            Assert.assertTrue(actual.getPermissions().contains(expectedPermission));
        }
    }

    @Test
    public void testUpdatePermissions() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName("other")
                                                                         .withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName(vfsUserName)
                                                                          .withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);
        //set up custom permissions
        ProjectMisc misc = pm.getProjectMisc("my_ws", myProject.getBaseFolder().getPath());
        misc.putAccessControlEntry(newEntry.withPermissions(Arrays.asList("run", "build")));

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry update = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("only_custom"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(vfsUserName)
                                                                       .withType(Principal.Type.USER));
        launcher.service("POST",
                         "http://localhost:8080/api/project/my_ws/permissions/my_project",
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(update)).getBytes(),
                         null
                        );

        Assert.assertEquals(misc.getAccessControlEntry(update.getPrincipal()), update);
        Assert.assertEquals(myProject.getBaseFolder().getVirtualFile().getACL().size(), 1);
    }

    @Test
    public void testGetPermissionsForCertainUser() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUserName)
                                                                         .withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other")
                                                                          .withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);
        //set up custom permissions
        ProjectMisc misc = pm.getProjectMisc("my_ws", myProject.getBaseFolder().getPath());
        misc.putAccessControlEntry(newEntry.withPermissions(Arrays.asList("run", "build")));

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/permissions/my_project?userid=" +
                                                      vfsUserName,
                                                      "http://localhost:8080/api",
                                                      null,
                                                      null,
                                                      null
                                                     );
        //response entity is ACL
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> entries = (List<AccessControlEntry>)response.getEntity();

        Assert.assertEquals(entries.size(), 1);
        //"all" should be replaced with "read" & "write" & "update_acl"
        Set<String> permissions = new HashSet<>(entries.get(0).getPermissions());
        Assert.assertTrue(permissions.contains("read"));
        Assert.assertTrue(permissions.contains("write"));
        Assert.assertTrue(permissions.contains("update_acl"));
        Assert.assertTrue(permissions.contains("run"));
        Assert.assertTrue(permissions.contains("build"));
    }

    @Test
    public void testGetAllProjectPermissions() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUserName)
                                                                         .withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other")
                                                                          .withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);
        //set up custom permissions
        ProjectMisc misc = pm.getProjectMisc("my_ws", myProject.getBaseFolder().getPath());
        misc.putAccessControlEntry(newEntry.withPermissions(Arrays.asList("run", "build")));

        ContainerResponse response = launcher.service("GET",
                                                      "http://localhost:8080/api/project/my_ws/permissions/my_project",
                                                      "http://localhost:8080/api",
                                                      null,
                                                      null,
                                                      null
                                                     );
        //response entity is ACL
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> entries = (List<AccessControlEntry>)response.getEntity();

        Assert.assertEquals(entries.size(), 2);
    }

    @Test
    public void testClearPermissionsForCertainUserToCertainProject() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("all"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUserName)
                                                                      .withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(entry), false, null);
        //set up custom permissions
        ProjectMisc misc = pm.getProjectMisc("my_ws", myProject.getBaseFolder().getPath());
        misc.putAccessControlEntry(entry.withPermissions(Arrays.asList("run", "build")));

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        launcher.service("POST",
                         "http://localhost:8080/api/project/my_ws/permissions/my_project",
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry.withPermissions(Collections.<String>emptyList()))).getBytes(),
                         null
                        );

        Assert.assertEquals(myProject.getBaseFolder().getVirtualFile().getACL().size(), 0);
        Assert.assertNull(misc.getAccessControlEntry(entry.getPrincipal()));
    }

    private void clearAcl(Project project) throws VirtualFileSystemException {
        project.getBaseFolder().getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
    }
}
