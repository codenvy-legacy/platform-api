/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemUserContext;
import com.codenvy.api.vfs.server.impl.memory.MemoryFileSystem;
import com.codenvy.api.vfs.server.impl.memory.MemoryMountPoint;
import com.codenvy.api.vfs.server.observation.EventListenerList;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.RuntimeDelegateImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.ext.RuntimeDelegate;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
public class PersistentProjectDescriptionTest {
    VirtualFileSystem         vfs;
    ProjectDescriptionFactory factory;
    Project                   project;

    @BeforeMethod
    public void setUp() throws Exception {
        RuntimeDelegate rd = new RuntimeDelegateImpl();
        RuntimeDelegate.setInstance(rd);
        ConversationState user =
                new ConversationState(new Identity("john", new HashSet<MembershipEntry>(), new HashSet<>(Arrays.asList("developer"))));
        ConversationState.setCurrent(user);
        EnvironmentContext env = EnvironmentContext.getCurrent();
        env.setVariable(EnvironmentContext.WORKSPACE_ID, "test_workspace");
        env.setVariable(EnvironmentContext.WORKSPACE_NAME, "test_workspace");

        VirtualFileSystemUserContext userContext = VirtualFileSystemUserContext.newInstance();
        MemoryMountPoint mountPoint = new MemoryMountPoint(null, userContext);
        VirtualFile root = mountPoint.getRoot();
        vfs = new MemoryFileSystem(URI.create(""), new EventListenerList(), "test_workspace", userContext, mountPoint, null);
        ProjectTypeRegistry typeRegistry = new ProjectTypeRegistry();
        typeRegistry.registerProjectType(new ProjectType("test_type", "test type"));
        factory = new ProjectDescriptionFactory(typeRegistry, new ProjectTypeDescriptionRegistry(typeRegistry));
        List<Property> properties = new ArrayList<>();
        properties.add(DtoFactory.getInstance().createDto(Property.class)
                                 .withName("test_property")
                                 .withValue(Arrays.asList("a", "b", "c")));
        project = vfs.createProject(root.getId(), "test_project", "test_type", properties);
    }

    @Test
    public void createProjectDescription() {
        PersistentProjectDescription description = factory.getDescription(project);
        ProjectType type = description.getProjectType();
        Assert.assertEquals(type.getId(), "test_type");
        Assert.assertEquals(type.getName(), "test type");
        // Get directly from the properties of project
        Attribute attribute = description.getAttribute("test_property");
        Assert.assertNotNull(attribute);
        Assert.assertEquals(attribute.getValues(), Arrays.asList("a", "b", "c"));
    }

    @Test
    public void updateProjectDescription() throws Exception {
        PersistentProjectDescription description = factory.getDescription(project);
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("test_property", Arrays.asList("to be or not to be"));
        attributes.put("new_test_property", Arrays.asList("to be"));
        description.update(DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                     .withProjectTypeId("new_test_type")
                                     .withProjectTypeName("new test type")
                                     .withAttributes(attributes));
        description.store(project, vfs);
        Project updatedProject = (Project)vfs.getItem(project.getId(), false, PropertyFilter.ALL_FILTER);
        Assert.assertEquals(getPropertyValue(updatedProject, "vfs:projectType"), Arrays.asList("new_test_type"));
        Assert.assertEquals(getPropertyValue(updatedProject, "test_property"), Arrays.asList("to be or not to be"));
        Assert.assertEquals(getPropertyValue(updatedProject, "new_test_property"), Arrays.asList("to be"));
    }

    private List<String> getPropertyValue(Item item, String name) {
        for (Property property : item.getProperties()) {
            if (name.equals(property.getName())) {
                return property.getValue();
            }
        }
        return null;
    }
}
