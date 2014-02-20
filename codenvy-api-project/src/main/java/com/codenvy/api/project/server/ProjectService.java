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
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/** @author andrew00x */
@Path("project/{ws-id}")
public class ProjectService extends Service {
    @Inject
    private ProjectManager         projectManager;
    @Inject
    private SourceImporterRegistry sourceImporters;
    @Context
    private UriInfo                uriInfo;

    /*

<DONE> GET	 	/
GET	    /{path:.*}
POST	/{path:.*}?name={name}
PUT	    /{path:.*}
POST	/import/{path:.*}?name={name}
POST	/export/{path:.*}
GET	    /children/{parent:.*}
POST	/copy/{path:.*}?to={path}
POST	/move/{path:.*}?to={path}
POST	/rename/{path:.*}?name={name}
POST	/file/{parent:.*}/{name}
GET	    /file/{path:.*}
PUT	    /file/{path:.*}
POST	/folder/{path:.*}/{name}
DELETE	/{path:.*}
GET	    /search/{path:.*}

     */

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public List<ProjectReference> getProjects(@PathParam("ws-id") String workspace) throws Exception {
        final List<Project> projects = projectManager.getProjects(workspace);
        final List<ProjectReference> projectRefs = new ArrayList<>();
        final ServiceContext thisServiceContext = getServiceContext();
        final UriBuilder thisServiceUriBuilder = thisServiceContext.getServiceUriBuilder();
        for (Project project : projects) {
            final ProjectDescription projectDescription = project.getDescription();
            final ProjectType projectType = projectDescription.getProjectType();
            final String projectRelPath = project.getBaseFolder().getPath().substring(1);
            projectRefs.add(DtoFactory.getInstance().createDto(ProjectReference.class)
                                      .withName(project.getName())
                                      .withWorkspace(workspace)
                                      .withProjectTypeId(projectType.getId())
                                      .withProjectTypeName(projectType.getName())
                                      .withDescription(projectDescription.getDescription())
                                      .withVisibility("public")
                                      .withUrl(thisServiceUriBuilder.clone().path(getClass(), "getProject").build(workspace, projectRelPath)
                                                                    .toString()));
        }
        return projectRefs;
    }

    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@PathParam("ws-id") String workspace, @PathParam("path") String path) {
//        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
//        final Item item = fileSystem.getItemByPath(name, null, false);
//        if (ItemType.PROJECT == item.getItemType()) {
//            return projectDescriptionFactory.getDescription((Project)item).getDescriptor();
//        }
//        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
        return null;
    }

//
//    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
//    @POST
//    @Path("create")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public ProjectDescriptor createProject(@PathParam("ws-id") String workspace,
//                                           @Required @Description("project name") @QueryParam("name") String name,
//                                           @Description("descriptor of project") ProjectDescriptor descriptor)
//            throws VirtualFileSystemException {
//        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
//        final Folder root = fileSystem.getInfo().getRoot();
//        final Project project = fileSystem.createProject(root.getId(), name, descriptor.getProjectTypeId(), null);
//        final ProjectProperties description = projectDescriptionFactory.getDescription(project);
//        description.update(descriptor);
//        description.store(project, fileSystem);
//        return description.getDescriptor();
//    }
//
//    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PROJECT)
//    @POST
//    @Path("update")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public ProjectDescriptor updateProject(@PathParam("ws-id") String workspace,
//                                           @Required @Description("project name") @QueryParam("name") String name,
//                                           @Description("descriptor of project") ProjectDescriptor descriptor)
//            throws VirtualFileSystemException {
//        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
//        final Item item = fileSystem.getItemByPath(name, null, false);
//        if (ItemType.PROJECT == item.getItemType()) {
//            final Project project = (Project)item;
//            final ProjectProperties description = projectDescriptionFactory.getDescription(project);
//            description.update(descriptor);
//            description.store(project, fileSystem);
//            return description.getDescriptor();
//        }
//        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", name));
//    }
//
//    @Path("vfs")
//    @Produces(MediaType.APPLICATION_JSON)
//    public VirtualFileSystem getVirtualFileSystem(@PathParam("ws-id") String workspace) throws VirtualFileSystemException {
//        return registry.getProvider(workspace).newInstance(uriInfo.getBaseUri(), listeners);
//    }
//
//    @GenerateLink(rel = Constants.LINK_REL_IMPORT_PROJECT)
//    @Path("import")
//    @POST
//    @Produces(MediaType.APPLICATION_JSON)
//    public ProjectDescriptor importSource(@PathParam("ws-id") String workspace,
//                                          @Required @Description("project name") @QueryParam("projectName") String projectName,
//                                          ImportSourceDescriptor importSourceDescriptor)
//            throws VirtualFileSystemException, IOException {
//        sourceImporters.getImporter(importSourceDescriptor.getType())
//                       .importSource(workspace, projectName, importSourceDescriptor.getLocation());
//        final VirtualFileSystem fileSystem = getVirtualFileSystem(workspace);
//        final Item item = fileSystem.getItemByPath(projectName, null, false);
//        if (ItemType.PROJECT == item.getItemType()) {
//            return projectDescriptionFactory.getDescription((Project)item).getDescriptor();
//        }
//        throw new ItemNotFoundException(String.format("Project '%s' does not exists in workspace. ", projectName));
//    }
//
//    @Path("importers")
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    public List<String> getImporters(){
//        return sourceImporters.getImporterTypes();
//    }
}
