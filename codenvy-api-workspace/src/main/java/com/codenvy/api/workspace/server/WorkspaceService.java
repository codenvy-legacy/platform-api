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
package com.codenvy.api.workspace.server;

import com.codenvy.api.project.server.AttributeValueProviderFactory;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.dto.Attributes;
import com.codenvy.api.vfs.server.RequestContext;
import com.codenvy.api.vfs.server.RequestValidator;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.EventListenerList;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.Folder;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@Path("{ws-name}/workspace")
public class WorkspaceService {
    @Inject
    private VirtualFileSystemRegistry             registry;
    @Inject
    private EventListenerList                     listeners;
    @Inject
    private RequestValidator                      requestValidator;
    @Context
    private Providers                             providers;
    @Context
    private javax.servlet.http.HttpServletRequest request;

    // >>> "shortcuts" for some vfs methods related to the project

    @GET
    @Path("projects/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Project getProject(@PathParam("name") String name) throws VirtualFileSystemException {
        final List<Project> projects = getProjects();
        if (!projects.isEmpty()) {
            for (Item project : projects) {
                if (name.equals(project.getName())) {
                    // We know item is Project since we requests virtual filesystem to show only projects.
                    return (Project)project;
                }
            }
        }
        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
    }

    @GET
    @Path("projects")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<Project> getProjects() throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem();
        final Folder root = fileSystem.getInfo().getRoot();
        final List projects = fileSystem.getChildren(root.getId(), -1, 0, "project", true, PropertyFilter.ALL_FILTER).getItems();
        return projects;
    }

    @POST
    @Path("projects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Project createProject(@QueryParam("name") String name,
                                 @QueryParam("type") String type,
                                 List<Property> properties) throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem();
        final Folder root = fileSystem.getInfo().getRoot();
        return fileSystem.createProject(root.getId(), name, type, properties);
    }

    // <<<

    @GET
    @Path("projects/{project}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    public Attributes getAttributes(@PathParam("project") String project) throws VirtualFileSystemException {
        final Project myProject = getProject(project);
        final Map<String, List<String>> values = new HashMap<>();
        for (AttributeValueProviderFactory factory : AttributeValueProviderFactory.getInstances()) {
            final Attribute attribute = new Attribute(factory.getName(), factory.newInstance(myProject));
            values.put(attribute.getName(), attribute.getValues());
        }
        // analyze properties
        for (Property property : myProject.getProperties()) {
            final Attribute attribute = new Attribute(property);
            values.put(attribute.getName(), attribute.getValues());
        }
        final Attributes attributes = DtoFactory.getInstance().createDto(Attributes.class);
        attributes.setAttributes(values);
        return attributes;
    }

    @Path("vfs")
    @Produces(MediaType.APPLICATION_JSON)
    public VirtualFileSystem getVirtualFileSystem() throws VirtualFileSystemException {
        if (requestValidator != null) {
            requestValidator.validate(request);
        }
        RequestContext context = null;
        final ContextResolver<RequestContext> contextResolver = providers.getContextResolver(RequestContext.class, null);
        if (contextResolver != null) {
            context = contextResolver.getContext(RequestContext.class);
        }
        final String vfsId = (String)EnvironmentContext.getCurrent().getVariable(EnvironmentContext.WORKSPACE_ID);
        return registry.getProvider(vfsId).newInstance(context, listeners);
    }
}
