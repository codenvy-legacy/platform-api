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

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.project.server.exceptions.SourceImporterNotFoundException;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.ItemNotFoundException;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.EventListenerList;
import com.codenvy.api.vfs.shared.ItemType;
import com.codenvy.api.vfs.shared.dto.Folder;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.Nullable;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** @author andrew00x */
@Path("project/{ws-id}")
public class ProjectService extends Service {
    @Inject
    private VirtualFileSystemRegistry registry;

    @Inject
    private SourceImporterExtensionRegistry sourceImporters;
    @Inject
    @Nullable
    private EventListenerList               listeners;
    @Inject
    private ProjectDescriptionFactory       projectDescriptionFactory;
    @Context
    private UriInfo                         uriInfo;

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECT)
    @GET
    @Path("description")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@PathParam("ws-id") String workspace,
                                        @Required @Description("project name") @QueryParam("name") String name)
            throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
        final Item item = fileSystem.getItemByPath(name, null, false);
        if (ItemType.PROJECT == item.getItemType()) {
            return projectDescriptionFactory.getDescription((Project)item).getDescriptor();
        }
        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
    }

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<ProjectReference> getProjects(@PathParam("ws-id") String workspace) throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
        final Folder root = fileSystem.getInfo().getRoot();
        final List<Item> projects = fileSystem.getChildren(root.getId(), -1, 0, "project", true).getItems();
        final List<ProjectReference> result = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder().path(getClass(), "getProject");
        for (Item project : projects) {
            result.add(DtoFactory.getInstance().createDto(ProjectReference.class)
                                 .withName(project.getName())
                                 .withUrl(uriBuilder.clone().queryParam("name", project.getName()).build(workspace).toString()));
        }
        return result;
    }

    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@PathParam("ws-id") String workspace,
                                           @Required @Description("project name") @QueryParam("name") String name,
                                           @Description("descriptor of project") ProjectDescriptor descriptor)
            throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
        final Folder root = fileSystem.getInfo().getRoot();
        final Project project = fileSystem.createProject(root.getId(), name, descriptor.getProjectTypeId(), null);
        final PersistentProjectDescription description = projectDescriptionFactory.getDescription(project);
        description.update(descriptor);
        description.store(project, fileSystem);
        return description.getDescriptor();
    }

    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PROJECT)
    @POST
    @Path("update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor updateProject(@PathParam("ws-id") String workspace,
                                           @Required @Description("project name") @QueryParam("name") String name,
                                           @Description("descriptor of project") ProjectDescriptor descriptor)
            throws VirtualFileSystemException {
        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
        final Item item = fileSystem.getItemByPath(name, null, false);
        if (ItemType.PROJECT == item.getItemType()) {
            final Project project = (Project)item;
            final PersistentProjectDescription description = projectDescriptionFactory.getDescription(project);
            description.update(descriptor);
            description.store(project, fileSystem);
            return description.getDescriptor();
        }
        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
    }

    @Path("vfs")
    @Produces(MediaType.APPLICATION_JSON)
    public VirtualFileSystem getVirtualFileSystem(@PathParam("ws-id") String workspace) throws VirtualFileSystemException {
        return registry.getProvider(workspace).newInstance(uriInfo.getBaseUri(), listeners);
    }

    @Path("import")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor importSource(@PathParam("ws-id") String workspace,
                                          @Required @Description("project name") @QueryParam("projectName") String projectName,
                                          ImportSourceDescriptor importSourceDescriptor)
            throws VirtualFileSystemException, IOException, SourceImporterNotFoundException {
        final String type = importSourceDescriptor.getType();
        SourceImporterExtension importExtension = sourceImporters.getImporter(type);
        importExtension.importSource(workspace, projectName, importSourceDescriptor);

        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
        final Item item = fileSystem.getItemByPath(projectName, null, false);
        if (ItemType.PROJECT == item.getItemType()) {
            final Project project = (Project)item;
            final PersistentProjectDescription description = projectDescriptionFactory.getDescription(project);
            description.store(project, fileSystem);
            return description.getDescriptor();
        }
        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", projectName));
    }

    @Path("importers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getImporters(){
        return sourceImporters.getImporterTypes();
    }

}
