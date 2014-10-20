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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.rest.ApiExceptionMapper;
import com.codenvy.api.core.rest.CodenvyJsonProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.LineConsumerFactory;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.project.shared.dto.GenerateDescriptor;
import com.codenvy.api.project.shared.dto.ImportProject;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.ProjectUpdate;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentLeaf;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentTree;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.project.shared.dto.TreeElement;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.ContentStreamWriter;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.user.UserImpl;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ProviderBinder;
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
import javax.ws.rs.core.Application;
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
    private static final String      vfsUser       = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));
    private static final String      workspace     = "my_ws";

    private ProjectManager           pm;
    private ResourceLauncher         launcher;
    private ProjectImporterRegistry  importerRegistry;
    private ProjectGeneratorRegistry generatorRegistry;

    private com.codenvy.commons.env.EnvironmentContext env;

    @Mock
    private UserDao                     userDao;
    private ProjectTypeResolverRegistry resolverRegistry;

    @BeforeMethod
    public void setUp() throws Exception {
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
        final EventService eventService = new EventService();
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();

        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(workspace, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUser, vfsUserGroups);
                    }
                }, vfsRegistry);
        MemoryMountPoint mmp = (MemoryMountPoint)memoryFileSystemProvider.getMountPoint(true);
        vfsRegistry.registerProvider(workspace, memoryFileSystemProvider);
        pm = new DefaultProjectManager(ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry, eventService);
        final ProjectType projectType = new ProjectType("my_project_type", "my project type", "my_category");
        ProjectDescription pd = new ProjectDescription(projectType);
        pd.setDescription("my test project");
        pd.setAttributes(Arrays.asList(new Attribute("my_attribute", "attribute value 1")));
        pm.createProject(workspace, "my_project", pd);

        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        importerRegistry = new ProjectImporterRegistry(Collections.<ProjectImporter>emptySet());
        generatorRegistry = new ProjectGeneratorRegistry(Collections.<ProjectGenerator>emptySet());
        HashSet<ProjectTypeResolver> resolvers = new HashSet<>();
        resolvers.add(new ProjectTypeResolver() {

            @Override
            public boolean resolve(FolderEntry projectFolder) throws ServerException, ValueStorageException, InvalidValueException {
                ProjectDescription description = new ProjectDescription();
                description.setProjectType(projectType);
                Project project = new Project(projectFolder, pm);
                project.updateDescription(description);
                return true;
            }
        });
        resolverRegistry = new ProjectTypeResolverRegistry(resolvers);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(ProjectManager.class, pm);
        dependencies.addComponent(ProjectImporterRegistry.class, importerRegistry);
        dependencies.addComponent(ProjectGeneratorRegistry.class, generatorRegistry);
        dependencies.addComponent(SearcherProvider.class, mmp.getSearcherProvider());
        dependencies.addComponent(ProjectTypeResolverRegistry.class, resolverRegistry);
        dependencies.addComponent(EventService.class, eventService);
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);

        processor.addApplication(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return java.util.Collections.<Class<?>>singleton(ProjectService.class);
            }

            @Override
            public Set<Object> getSingletons() {
                return new HashSet<>(Arrays.asList(new CodenvyJsonProvider(new HashSet<>(Arrays.asList(ContentStream.class))),
                                                   new ContentStreamWriter(),
                                                   new ApiExceptionMapper()));
            }
        });

        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));

        env = com.codenvy.commons.env.EnvironmentContext.getCurrent();
        env.setUser(new UserImpl(vfsUser, vfsUser, "dummy_token", vfsUserGroups, false));
        env.setWorkspaceName(workspace);
        env.setWorkspaceId(workspace);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetProjects() throws Exception {
        ContainerResponse response =
                launcher.service("GET", "http://localhost:8080/api/project/my_ws", "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectReference> result = (List<ProjectReference>)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(),1);
        ProjectReference projectReference = result.get(0);
        Assert.assertEquals(projectReference.getName(), "my_project");
        Assert.assertEquals(projectReference.getUrl(), String.format("http://localhost:8080/api/project/%s/my_project", workspace));
        Assert.assertEquals(projectReference.getDescription(), "my test project");
        Assert.assertEquals(projectReference.getWorkspaceId(), workspace);
        Assert.assertEquals(projectReference.getType(), "my_project_type");
        Assert.assertEquals(projectReference.getTypeName(), "my project type");
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

        Project myProject = pm.getProject(workspace, "my_project");
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_module_type", "my module type", "my_category"));
        pd.setDescription("my test module");
        pd.setAttributes(Arrays.asList(new Attribute("my_module_attribute", "attribute value 1")));
        FolderEntry moduleFolder = myProject.getBaseFolder().createFolder("my_module");
        Project module = new Project(moduleFolder, pm);
        module.updateDescription(pd);

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/modules/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectDescriptor> result = (List<ProjectDescriptor>)response.getEntity();
        Assert.assertNotNull(result);

        Assert.assertEquals(result.size(), 1);
        ProjectDescriptor moduleDescriptor = result.get(0);
        Assert.assertEquals(moduleDescriptor.getDescription(), "my test module");
        Assert.assertEquals(moduleDescriptor.getType(), "my_module_type");
        Assert.assertEquals(moduleDescriptor.getTypeName(), "my module type");
        Assert.assertEquals(moduleDescriptor.getVisibility(), "public");
        validateProjectLinks(moduleDescriptor);
    }

    @Test
    public void testGetProject() throws Exception {
        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "my test project");
        Assert.assertEquals(result.getType(), "my_project_type");
        Assert.assertEquals(result.getTypeName(), "my project type");
        Assert.assertEquals(result.getVisibility(), "public");
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("my_attribute"), Arrays.asList("attribute value 1"));
        validateProjectLinks(result);
    }


//    @Test
//    public void testGetNotValidProject() throws Exception {
//        MountPoint mountPoint = pm.getProjectsRoot(workspace).getVirtualFile().getMountPoint();
//        mountPoint.getRoot().createFolder("not_project");
//        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/not_project", workspace),
//                                                      "http://localhost:8080/api", null, null, null);
//        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
//        ProjectDescriptor badProject = (ProjectDescriptor)response.getEntity();
//        Assert.assertNotNull(badProject);
//        Assert.assertEquals(badProject.getName(), "not_project");
//        Assert.assertEquals(badProject.getWorkspaceId(), workspace);
//        Assert.assertEquals(badProject.getVisibility(), "public");
//        Assert.assertNotNull(badProject.getProblems());
//        Assert.assertTrue(badProject.getProblems().size() > 0);
//        Assert.assertEquals(1, badProject.getProblems().get(0).getCode());
//        validateProjectLinks(badProject);
//    }

    @Test
    public void testGetProjectCheckUserPermissions() throws Exception {
        // Without roles Collections.<String>emptySet() should get default set of permissions
        env.setUser(new UserImpl(vfsUser, vfsUser, "dummy_token", Collections.<String>emptySet(), false));
        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getPermissions(), Arrays.asList("read"));
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

        Project myProject = pm.getProject(workspace, "my_project");
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_module_type", "my module type", "my_category"));
        pd.setDescription("my test module");
        pd.setAttributes(Arrays.asList(new Attribute("my_module_attribute", "attribute value 1")));
        FolderEntry moduleFolder = myProject.getBaseFolder().createFolder("my_module");
        Project module = new Project(moduleFolder, pm);
        module.updateDescription(pd);

        ContainerResponse response =
                launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project/my_module", workspace),
                                 "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "my test module");
        Assert.assertEquals(result.getType(), "my_module_type");
        Assert.assertEquals(result.getTypeName(), "my module type");
        Assert.assertEquals(result.getVisibility(), "public");
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("my_module_attribute"), Arrays.asList("attribute value 1"));
        validateProjectLinks(result);
    }

    @Test
    public void testGetProjectInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/my_project_invalid", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 404);
    }

    @Test
    public void testCreateProject() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        NewProject descriptor = DtoFactory.getInstance().createDto(NewProject.class)
                                          .withType("my_project_type")
                                          .withDescription("new project")
                                          .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s?name=new_project", workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "new project");
        Assert.assertEquals(result.getType(), "my_project_type");
        Assert.assertEquals(result.getTypeName(), "my project type");
        Assert.assertEquals(result.getVisibility(), "public");
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("new project attribute"), Arrays.asList("to be or not to be"));
        validateProjectLinks(result);

        Project project = pm.getProject(workspace, "new_project");
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
        NewProject descriptor = DtoFactory.getInstance().createDto(NewProject.class)
                                          .withType("my_project_type")
                                          .withDescription("new module")
                                          .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/my_project?name=new_module",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getDescription(), "new module");
        Assert.assertEquals(result.getType(), "my_project_type");
        Assert.assertEquals(result.getTypeName(), "my project type");
        Assert.assertEquals(result.getVisibility(), "public");
        Map<String, List<String>> attributes = result.getAttributes();
        Assert.assertNotNull(attributes);
        Assert.assertEquals(attributes.size(), 1);
        Assert.assertEquals(attributes.get("new module attribute"), Arrays.asList("to be or not to be"));
        validateProjectLinks(result);

        Project project = pm.getProject(workspace, "my_project/new_module");
        Assert.assertNotNull(project);
        ProjectDescription description = project.getDescription();

        Assert.assertEquals(description.getDescription(), "new module");
        Assert.assertEquals(description.getProjectType().getId(), "my_project_type");
        Assert.assertEquals(description.getProjectType().getName(), "my project type");
        Attribute attribute = description.getAttribute("new module attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));
    }

    @Test
    public void testCreateProjectUnknownProjectType() throws Exception {
        final String newProjectTypeId = "new_project_type";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withType(newProjectTypeId)
                                                 .withDescription("new project")
                                                 .withAttributes(attributeValues);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws?name=new_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      writer,
                                                      null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Project project = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(project);
        ProjectDescription description = project.getDescription();

        Assert.assertEquals(description.getDescription(), "new project");
        Assert.assertEquals(description.getProjectType().getId(), "new_project_type");
        Assert.assertEquals(description.getProjectType().getName(), "new_project_type");
        Attribute attribute = description.getAttribute("new project attribute");
        Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));
    }

    @Test
    public void testUpdateProject() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectUpdate descriptor = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                                             .withType("my_project_type")
                                             .withDescription("updated project")
                                             .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Project project = pm.getProject(workspace, "my_project");
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
        ProjectUpdate descriptor = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                                             .withType("my_project_type")
                                             .withDescription("updated project")
                                             .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/my_project_invalid",
                                                                    workspace),
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
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project?name=test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      myContent.getBytes(),
                                                      null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        ItemReference fileItem = (ItemReference)response.getEntity();
        Assert.assertEquals(fileItem.getType(), "file");
        Assert.assertEquals(fileItem.getMediaType(), "text/plain");
        Assert.assertEquals(fileItem.getName(), "test.txt");
        Assert.assertEquals(fileItem.getPath(), "/my_project/test.txt");
        validateFileLinks(fileItem);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/test.txt", workspace)));
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test.txt");
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
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/uploadfile/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      formData,
                                                      env);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild(fileName);
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), fileMediaType);
        Assert.assertEquals(new String(_file.contentAsBytes()), fileContent);
    }

    @Test
    public void testGetFileContent() throws Exception {
        String myContent = "to be or not to be";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test.txt", myContent.getBytes(), "text/plain");
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project/test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, writer, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Assert.assertEquals(response.getContentType().toString(), "text/plain");
        Assert.assertEquals(new String(writer.getBody()), myContent);
    }

    @Test
    public void testUpdateFileContent() throws Exception {
        String myContent = "<test>hello</test>";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test", "to be or not to be".getBytes(), "text/plain");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("text/xml"));
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project/test", workspace),
                                                      "http://localhost:8080/api", headers, myContent.getBytes(), null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        Assert.assertEquals(_file.getMediaType(), "text/xml");
        Assert.assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testCreateFolder() throws Exception {
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/folder/my_project/test",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        ItemReference fileItem = (ItemReference)response.getEntity();
        Assert.assertEquals(fileItem.getType(), "folder");
        Assert.assertEquals(fileItem.getMediaType(), "text/directory");
        Assert.assertEquals(fileItem.getName(), "test");
        Assert.assertEquals(fileItem.getPath(), "/my_project/test");
        validateFolderLinks(fileItem);
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/test", workspace)));
        VirtualFileEntry folder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testCreatePath() throws Exception {
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/folder/my_project/a/b/c",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b/c", workspace)));
        VirtualFileEntry folder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("a/b/c");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testDeleteFile() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test.txt", "to be or not to be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/test.txt", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testDeleteFolder() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFolder("test");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/test", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("test"));
    }

    @Test
    public void testDeletePath() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/a/b/c", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testDeleteInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/a/b/c", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 404);
        Assert.assertNotNull(pm.getProject(workspace, "my_project"));
    }

    @Test
    public void testDeleteProject() throws Exception {
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project"));
    }

    @Test
    public void testCopyFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/copy/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/a/b/c/test.txt", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testCopyFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/copy/my_project/a/b?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b/c/b", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt"));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/b/test.txt"));
    }

    @Test
    public void testMoveFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/move/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/a/b/c/test.txt", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testMoveFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b/c")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/move/my_project/a/b/c?to=/my_project/a",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/c", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testRenameFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/rename/my_project/test.txt?name=_test.txt",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/_test.txt", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("_test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFileAndUpdateMediaType() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/*");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/rename/my_project/test.txt?name=_test.txt&mediaType=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/_test.txt", workspace)));
        FileEntry renamed = (FileEntry)myProject.getBaseFolder().getChild("_test.txt");
        Assert.assertNotNull(renamed);
        Assert.assertEquals(renamed.getMediaType(), "text/plain");
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/rename/my_project/a/b?name=x",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/x", workspace)));
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
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"chuck_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"source\":{\"project\":{\"location\":null,\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getDescription(), "import test");
        Assert.assertEquals(descriptor.getType(), "chuck_project_type");
        Assert.assertEquals(descriptor.getAttributes().get("x"), Arrays.asList("a", "b"));

        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
    }


    @Test
    public void testImportProjectWithVisibility() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        String visibility = "private";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format("{\"project\":{\"name\":null,\"type\":\"%s\",\"attributes\":{},\"visibility\":\"%s\",\"description\":null," +
                                 "\"builders\":null,\"runners\":null},\"source\":{\"project\":{\"location\":null,\"type\":\"%s\"," +
                                 "\"parameters\":{}},\"runners\":{}}}", "mytype", visibility, importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(descriptor.getVisibility());
        Assert.assertEquals(descriptor.getVisibility(), visibility);
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getVisibility());
        Assert.assertEquals(newProject.getVisibility(), visibility);
    }


    @Test
    public void testImportProjectWithProjectType() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });


        ImportProject dto = DtoFactory.getInstance().createDto(ImportProject.class).withProject(DtoFactory.getInstance().createDto(NewProject.class)).withSource(DtoFactory.getInstance().createDto(Source.class).withProject(DtoFactory.getInstance().createDto(ImportSourceDescriptor.class)));
        System.out.println(dto.toString());

        String myType = "superType";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format("{\"project\":{\"name\":null,\"type\":\"%s\",\"attributes\":{},\"visibility\":null,\"description\":null," +
                                 "\"builders\":null,\"runners\":null},\"source\":{\"project\":{\"location\":null,\"type\":\"%s\"," +
                                 "\"parameters\":{}},\"runners\":{}}}",myType, importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertNotNull(descriptor.getType());
        Assert.assertEquals(descriptor.getType(), myType);
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getDescription());
        Assert.assertNotNull(newProject.getDescription().getProjectType());
        Assert.assertEquals(newProject.getDescription().getProjectType().getId(), myType);
    }

//    @Test
    public void testImportProjectWithRunners() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"chuck_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        java.io.File runnerRecipe =
                new java.io.File(new java.io.File(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParentFile(),
                                 "recipe");
        byte[] recipeBytes = "# runner recipe\n".getBytes();
        try (java.io.FileOutputStream w = new java.io.FileOutputStream(runnerRecipe)) {
            w.write(recipeBytes);
        }

        String envName = "my_env_1";

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"project\":{\"type\":\"%s\"},\"runners\":{\"/docker/%s\":{\"location\":\"%s\"}}}",
                                 importType, envName, runnerRecipe.toURI().toURL()).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getDescription(), "import test");
        Assert.assertEquals(descriptor.getType(), "chuck_project_type");
        Assert.assertEquals(descriptor.getAttributes().get("x"), Arrays.asList("a", "b"));

        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        VirtualFileEntry environments = newProject.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        Assert.assertNotNull(environments);
        Assert.assertTrue(environments.isFolder());
        VirtualFileEntry recipe = ((FolderEntry)environments).getChild(envName + "/Dockerfile");
        Assert.assertNotNull(recipe);
        Assert.assertTrue(recipe.isFile());
        Assert.assertEquals(((FileEntry)recipe).getMediaType(), "text/x-docker");
        Assert.assertEquals(((FileEntry)recipe).contentAsBytes(), recipeBytes);
    }

    @Test
    public void testImportNotConfigProjectWithoutResolvers() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        Set<ProjectTypeResolver> resolvers = resolverRegistry.getResolvers();
        for (ProjectTypeResolver resolver : resolvers) {//unregistered all resolvers
            resolverRegistry.unregister(resolver);
        }

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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"source\":{\"project\":{\"location\":null,\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getType(), "blank");
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getDescription());
        Assert.assertEquals("blank", newProject.getDescription().getProjectType().getId());
    }

    @Test
    public void testProjectTypeResolver() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
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
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"source\":{\"project\":{\"location\":null,\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getType(), "my_project_type");
        Assert.assertNotNull(descriptor.getProblems());
        Assert.assertTrue(descriptor.getProblems().size()>0);
        Assert.assertEquals(300, descriptor.getProblems().get(0).getCode());
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
    }

    @Test
    public void testImportZip() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
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
                                                      String.format("http://localhost:8080/api/project/%s/import/my_project/a/b",
                                                                    workspace),
                                                      "http://localhost:8080/api", headers, zip, null);
        Assert.assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        Assert.assertEquals(response.getHttpHeaders().getFirst("Location"),
                            URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b", workspace)));
        Assert.assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/file1.txt"));
    }

    @Test
    public void testGenerate() throws Exception {
        pm.createProject(workspace, "new_project",
                         new ProjectDescription(new ProjectType("my_project_type", "my project type", "my_category")));
        generatorRegistry.register(new ProjectGenerator() {
            @Override
            public String getId() {
                return "my_generator";
            }

            @Override
            public void generateProject(FolderEntry baseFolder, Map<String, String> options)
                    throws ConflictException, ForbiddenException, ServerException {
                baseFolder.createFolder("a");
                baseFolder.createFolder("b");
                baseFolder.createFile("test.txt", "test".getBytes(), "text/plain");
            }
        });

        GenerateDescriptor generateDescriptor = DtoFactory.getInstance().createDto(GenerateDescriptor.class)
                                                          .withGeneratorName("my_generator");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/generate/new_project",
                                                              workspace),
                                                      "http://localhost:8080/api",
                                                      headers, DtoFactory.getInstance().toJson(generateDescriptor).getBytes(), null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getType(), "my_project_type");
        Assert.assertEquals(descriptor.getTypeName(), "my project type");
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getBaseFolder().getChild("a"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("b"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testGeneratePrivateProject() throws Exception {
        pm.createProject(workspace, "new_project",
                         new ProjectDescription(new ProjectType("my_project_type", "my project type", "my_category")));
        generatorRegistry.register(new ProjectGenerator() {
            @Override
            public String getId() {
                return "my_generator";
            }

            @Override
            public void generateProject(FolderEntry baseFolder, Map<String, String> options)
                    throws ConflictException, ForbiddenException, ServerException {
                baseFolder.createFolder("a");
                baseFolder.createFolder("b");
                baseFolder.createFile("test.txt", "test".getBytes(), "text/plain");
            }
        });

        GenerateDescriptor generateDescriptor = DtoFactory.getInstance().createDto(GenerateDescriptor.class)
                                                          .withGeneratorName("my_generator")
                                                          .withProjectVisibility("private");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/generate/new_project",
                                                              workspace),
                                                      "http://localhost:8080/api",
                                                      headers, DtoFactory.getInstance().toJson(generateDescriptor).getBytes(), null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getType(), "my_project_type");
        Assert.assertEquals(descriptor.getTypeName(), "my project type");
        Assert.assertEquals(descriptor.getVisibility(), "private");
        Project newProject = pm.getProject(workspace, "new_project");
        Assert.assertNotNull(newProject);
        Assert.assertNotNull(newProject.getBaseFolder().getChild("a"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("b"));
        Assert.assertNotNull(newProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testExportZip() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/export/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Assert.assertEquals(response.getContentType().toString(), "application/zip");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChildren() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/children/my_project/a",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
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
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/tree/my_project/a", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        TreeElement tree = (TreeElement)response.getEntity();
        ItemReference a_node = tree.getNode();
        Assert.assertEquals(a_node.getName(), "a");
        validateFolderLinks(a_node);
        List<TreeElement> children = tree.getChildren();
        Assert.assertNotNull(children);
        Assert.assertEquals(children.size(), 2);
        Set<String> names = new LinkedHashSet<>(2);
        for (TreeElement subTree : children) {
            ItemReference _node = subTree.getNode();
            validateFolderLinks(_node);
            names.add(_node.getName());
            Assert.assertTrue(subTree.getChildren().isEmpty()); // default depth is 1
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
    }

    @Test
    public void testGetTreeWithDepth() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/tree/my_project/a?depth=2",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        TreeElement tree = (TreeElement)response.getEntity();
        ItemReference a_node = tree.getNode();
        Assert.assertEquals(a_node.getName(), "a");
        List<TreeElement> children = tree.getChildren();
        Assert.assertNotNull(children);
        Set<String> names = new LinkedHashSet<>(4);
        for (TreeElement subTree : children) {
            ItemReference _node = subTree.getNode();
            validateFolderLinks(_node);
            String name = _node.getName();
            names.add(name);
            for (TreeElement subSubTree : subTree.getChildren()) {
                ItemReference __node = subSubTree.getNode();
                validateFolderLinks(__node);
                names.add(name + "/" + __node.getName());
            }
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
        Assert.assertTrue(names.contains("b/c"));
        Assert.assertTrue(names.contains("x/y"));
    }

    @Test
    public void testSwitchProjectVisibilityToPrivate() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/switch_visibility/my_project?visibility=private",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        // Private project is accessible only for user who are in the group "workspace/developer"
        Map<Principal, Set<String>> permissions = myProject.getBaseFolder().getVirtualFile().getPermissions();
        Assert.assertEquals(permissions.size(), 1);
        Principal principal = DtoFactory.getInstance().createDto(Principal.class)
                                        .withName("workspace/developer")
                                        .withType(Principal.Type.GROUP);
        Assert.assertEquals(permissions.get(principal), Arrays.asList(BasicPermissions.ALL.value()));

        response = launcher.service("GET",
                                    String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                    "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getVisibility(), "private");
    }

    @Test
    public void testUpdateProjectVisibilityToPublic() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.setVisibility("private");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/switch_visibility/my_project?visibility=public",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        // List of permissions should be cleared. After that project inherits permissions from parent folder (typically root folder)
        Map<Principal, Set<String>> permissions = myProject.getBaseFolder().getVirtualFile().getPermissions();
        Assert.assertEquals(permissions.size(), 0);

        response = launcher.service("GET",
                                    String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                    "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        Assert.assertEquals(descriptor.getVisibility(), "public");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByName() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("exclude", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/search/my_project?name=test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
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
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("__test.txt", "searchhit".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("_test", "searchhit".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/search/my_project?text=searchhit",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
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
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "6769675".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "132434".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("test", "2343124".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/search/my_project?mediatype=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
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
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/*");
        myProject.getBaseFolder().createFolder("c").createFile("test", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/search/my_project?text=test&name=test&mediatype=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getPath().equals("/my_project/c/test"));
    }

    @Test
    public void testSetBasicPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("all"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        Assert.assertNotNull(entry2, "Not found expected ACL entry after update");

        Assert.assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testSetCustomPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("custom"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        Assert.assertNotNull(entry2, "Not found expected ACL entry after update");

        Assert.assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testSetBothBasicAndCustomPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("build", "run", "update_acl", "read", "write"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );

        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        Assert.assertNotNull(entry2, "Not found expected ACL entry after update");

        Assert.assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testUpdatePermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName("other")
                                                                         .withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName(vfsUser).withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry update = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("only_custom"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(vfsUser).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(update)).getBytes(),
                         null
                        );

        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        Assert.assertEquals(acl.size(), 2);
        Map<Principal, Set<String>> map = new HashMap<>(2);
        for (AccessControlEntry ace : acl) {
            map.put(ace.getPrincipal(), new HashSet<>(ace.getPermissions()));
        }
        Assert.assertNotNull(map.get(newEntry.getPrincipal()));
        Assert.assertNotNull(map.get(newEntry2.getPrincipal()));
        Assert.assertEquals(map.get(newEntry.getPrincipal()).size(), 1);
        Assert.assertEquals(map.get(newEntry2.getPrincipal()).size(), 1);
        Assert.assertTrue(map.get(newEntry.getPrincipal()).contains("all"));
        Assert.assertTrue(map.get(newEntry2.getPrincipal()).contains("only_custom"));
    }

    @Test
    public void testGetPermissionsForCertainUser() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUser).withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other").withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/permissions/my_project?userid=%s",
                                                                    workspace, vfsUser),
                                                      "http://localhost:8080/api",
                                                      null,
                                                      null,
                                                      null
                                                     );
        //response entity is ACL
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> entries = (List<AccessControlEntry>)response.getEntity();

        Assert.assertEquals(entries.size(), 1);
        //"all" should be replaced with "read" & "write" & "update_acl", etc
        Set<String> permissions = new HashSet<>(entries.get(0).getPermissions());
        Assert.assertTrue(permissions.contains("read"));
        Assert.assertTrue(permissions.contains("write"));
        Assert.assertTrue(permissions.contains("update_acl"));
        Assert.assertTrue(permissions.contains("run"));
        Assert.assertTrue(permissions.contains("build"));
    }

    @Test
    public void testGetAllProjectPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUser).withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other").withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/permissions/my_project",
                                                                    workspace),
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
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("all"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUser)
                                                                      .withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(entry), false, null);

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry.withPermissions(null))).getBytes(),
                         null
                        );

        Assert.assertEquals(myProject.getBaseFolder().getVirtualFile().getACL().size(), 0);
    }

    @Test
    public void testGetRunnerEnvironments() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry environmentsFolder = myProject.getBaseFolder().createFolder(".codenvy/runners/environments");
        environmentsFolder.createFolder("my_env_1");
        environmentsFolder.createFolder("my_env_2");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/runner_environments/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        RunnerEnvironmentTree runnerEnvironmentTree = (RunnerEnvironmentTree)response.getEntity();
        Assert.assertEquals(runnerEnvironmentTree.getDisplayName(), "project");
        List<RunnerEnvironmentLeaf> environments = runnerEnvironmentTree.getLeaves();
        Assert.assertNotNull(environments);
        Assert.assertEquals(environments.size(), 2);

        Set<String> ids = new LinkedHashSet<>(2);
        Set<String> names = new LinkedHashSet<>(2);
        for (RunnerEnvironmentLeaf environment : environments) {
            ids.add(environment.getEnvironment().getId());
            names.add(environment.getDisplayName());
        }
        Assert.assertTrue(ids.contains("project://my_env_1"));
        Assert.assertTrue(ids.contains("project://my_env_2"));
        Assert.assertTrue(names.contains("my_env_1"));
        Assert.assertTrue(names.contains("my_env_2"));
    }

    private void validateFileLinks(ItemReference item) {
        Link link = item.getLink("delete");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "DELETE");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + item.getPath());

        link = item.getLink("get content");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getProduces(), item.getMediaType());
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/file" + item.getPath());

        link = item.getLink("update content");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "PUT");
        Assert.assertEquals(link.getConsumes(), "*/*");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/file" + item.getPath());
    }

    private void validateFolderLinks(ItemReference item) {
        Link link = item.getLink("children");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/children" + item.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = item.getLink("tree");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/tree" + item.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = item.getLink("modules");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/modules" + item.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = item.getLink("zipball sources");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/export" + item.getPath());
        Assert.assertEquals(link.getProduces(), "application/zip");

        link = item.getLink("delete");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "DELETE");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + item.getPath());
    }

    private void validateProjectLinks(ProjectDescriptor project) {
        Link link = project.getLink("update project");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "PUT");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + project.getPath());
        Assert.assertEquals(link.getConsumes(), "application/json");
        Assert.assertEquals(link.getProduces(), "application/json");

        link = project.getLink("children");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/children" + project.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = project.getLink("tree");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/tree" + project.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = project.getLink("modules");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/modules" + project.getPath());
        Assert.assertEquals(link.getProduces(), "application/json");

        link = project.getLink("zipball sources");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/export" + project.getPath());
        Assert.assertEquals(link.getProduces(), "application/zip");

        link = project.getLink("delete");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "DELETE");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + project.getPath());

        link = project.getLink("get runner environments");
        Assert.assertNotNull(link);
        Assert.assertEquals(link.getMethod(), "GET");
        Assert.assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/runner_environments" + project.getPath());
    }

    private void clearAcl(Project project) throws ServerException, ForbiddenException {
        project.getBaseFolder().getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
    }
}
