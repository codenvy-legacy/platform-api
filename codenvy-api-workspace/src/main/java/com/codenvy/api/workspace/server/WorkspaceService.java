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

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.project.server.ProjectDescriptionFactory;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
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
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@Path("{ws-name}/workspace")
public class WorkspaceService extends Service {
    @Inject
    private VirtualFileSystemRegistry registry;
    @Inject
    private EventListenerList         listeners;
    @Inject
    private RequestValidator          requestValidator;
    @Inject
    private ProjectDescriptionFactory projectDescriptionFactory;
    @Context
    private Providers                 providers;
    @Context
    private HttpServletRequest        request;

    // >>> "shortcuts" for some vfs methods related to the project

    @GenerateLink(rel = com.codenvy.api.workspace.Constants.LINK_REL_GET_PROJECT)
    @GET
    @Path("project")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@Required @Description("project name") @QueryParam("name") String name)
            throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem();
        final Folder root = fileSystem.getInfo().getRoot();
        final List<Item> projects = fileSystem.getChildren(root.getId(), -1, 0, "project", true, PropertyFilter.ALL_FILTER).getItems();
        if (!projects.isEmpty()) {
            for (Item project : projects) {
                if (name.equals(project.getName())) {
                    final ProjectDescription description = projectDescriptionFactory.getDescription((Project)project);
                    final ProjectType projectType = description.getProjectType();
                    final Map<String, List<String>> attributeValues = new HashMap<>();
                    for (Attribute attribute : description.getAttributes()) {
                        attributeValues.put(attribute.getName(), attribute.getValues());
                    }
                    return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                     .withProjectTypeId(projectType.getId())
                                     .withProjectTypeName(projectType.getName())
                                     .withAttributes(attributeValues);
                }
            }
        }
        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
    }

    @GenerateLink(rel = com.codenvy.api.workspace.Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Path("projects")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<ProjectReference> getProjects(@PathParam("ws-name") String workspace) throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem();
        final Folder root = fileSystem.getInfo().getRoot();
        final List<Item> projects = fileSystem.getChildren(root.getId(), -1, 0, "project", true, PropertyFilter.ALL_FILTER).getItems();
        final List<ProjectReference> result = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder().path(getClass(), "getProject");
        for (Item project : projects) {
            result.add(DtoFactory.getInstance().createDto(ProjectReference.class)
                                 .withName(project.getName())
                                 .withUrl(uriBuilder.clone().queryParam("name", project.getName()).build(workspace).toString()));
        }
        return result;
    }

    @GenerateLink(rel = com.codenvy.api.workspace.Constants.LINK_REL_CREATE_PROJECT)
    @POST
    @Path("project")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@Required @Description("project name") @QueryParam("name") String name,
                                           @Description("descriptor of project") ProjectDescriptor descriptor)
            throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem();
        final Folder root = fileSystem.getInfo().getRoot();
        final Project project = fileSystem.createProject(root.getId(), name, descriptor.getProjectTypeId(), null);
        final ProjectDescription description = projectDescriptionFactory.getDescription(project);
        final ProjectType projectType = description.getProjectType();
        final Map<String, List<String>> attributeValues = new HashMap<>();
        for (Attribute attribute : description.getAttributes()) {
            attributeValues.put(attribute.getName(), attribute.getValues());
        }
        return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                         .withProjectTypeId(projectType.getId())
                         .withProjectTypeName(projectType.getName())
                         .withAttributes(attributeValues);
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
