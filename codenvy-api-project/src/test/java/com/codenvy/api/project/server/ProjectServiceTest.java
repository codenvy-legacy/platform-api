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
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;

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
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author andrew00x
 */
public class ProjectServiceTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

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
        ProjectManager pm = new ProjectManager(ptr, ptdr, Collections.<ValueProviderFactory>emptySet(), vfsRegistry);
        ProjectDescription pd = new ProjectDescription(new ProjectType("my_project_type", "my project type"));
        pd.setDescription("my test project");
        pd.setAttributes(Arrays.asList(new Attribute("my_attribute", "attribute value 1")));
        pm.createProject("my_ws", "my_project", pd);

        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(ProjectManager.class, pm);
        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
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
}
