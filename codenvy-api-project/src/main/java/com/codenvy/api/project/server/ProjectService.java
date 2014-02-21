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
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.io.ByteStreams;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
@Path("project/{ws-id}")
public class ProjectService extends Service {
    @Inject
    private ProjectManager      projectManager;
    @Inject
    private ProjectTypeRegistry projectTypeRegistry;
    @Inject
    private SourceImporterRegistry sourceImporters;

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectReference> getProjects(@PathParam("ws-id") String workspace) throws Exception {
        final List<Project> projects = projectManager.getProjects(workspace);
        final List<ProjectReference> projectRefs = new ArrayList<>();
        final UriBuilder thisServiceUriBuilder = getServiceContext().getServiceUriBuilder();
        for (Project project : projects) {
            final ProjectDescription description = project.getDescription();
            final ProjectType type = description.getProjectType();
            final String name = project.getName();
            // TODO: check project visibility
            projectRefs.add(DtoFactory.getInstance().createDto(ProjectReference.class)
                                      .withName(name)
                                      .withWorkspace(workspace)
                                      .withProjectTypeId(type.getId())
                                      .withProjectTypeName(type.getName())
                                      .withDescription(description.getDescription())
                                      .withVisibility("public")
                                      .withUrl(thisServiceUriBuilder.clone().path(getClass(), "getProject").build(workspace, name)
                                                                    .toString()));
        }
        return projectRefs;
    }

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            throw new ProjectNotFoundException(workspace, path);
        }
        return toDescriptor(project);
    }

    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@PathParam("ws-id") String workspace,
                                           @Required @Description("project name") @QueryParam("name") String name,
                                           @Description("descriptor of project") ProjectDescriptor descriptor) throws Exception {
        final Project project = projectManager.createProject(workspace, name, toDescription(descriptor));
        return toDescriptor(project);
    }

    @POST
    @Path("{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createModule(@PathParam("ws-id") String workspace,
                                          @PathParam("path") String parentProject,
                                          @QueryParam("name") String name,
                                          ProjectDescriptor descriptor) throws Exception {
        final Project project = projectManager.getProject(workspace, parentProject);
        if (project == null) {
            throw new ProjectNotFoundException(workspace, parentProject);
        }
        final Project module = project.createModule(name, toDescription(descriptor));
        return toDescriptor(module);
    }

    @PUT
    @Path("{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor updateProject(@PathParam("ws-id") String workspace,
                                           @PathParam("path") String projectPath,
                                           ProjectDescriptor descriptor) throws Exception {
        final Project project = projectManager.getProject(workspace, projectPath);
        if (project == null) {
            throw new ProjectNotFoundException(workspace, projectPath);
        }
        project.updateDescription(toDescription(descriptor));
        return toDescriptor(project);
    }

    @POST
    @Path("file/{parent:.*}")
    public Response createFile(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String parentPath,
                               @QueryParam("name") String fileName,
                               @HeaderParam("content-type") String contentType,
                               InputStream content) throws Exception {
        asFolder(workspace, parentPath).createFile(fileName, content, contentType);
        return Response.created(URI.create("")).build(); // TODO: Location
    }

    @GET
    @Path("file/{path:.*}")
    public Response getFile(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final FileEntry file = asFile(workspace, path);
        return Response.ok().entity(file.getInputStream()).type(file.getMediaType()).build();
    }

    @PUT
    @Path("file/{path:.*}")
    public Response updateFile(@PathParam("ws-id") String workspace,
                               @PathParam("path") String path,
                               @HeaderParam("content-type") String contentType,
                               InputStream content) throws Exception {
        final FileEntry file = asFile(workspace, path);
        try (OutputStream outputStream = file.openOutputStream()) {
            ByteStreams.copy(content, outputStream);
        }
        file.setMediaType(contentType);
        return Response.ok().build();
    }

    @POST
    @Path("folder/{path:.*}")
    public Response createFolder(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        projectManager.getProjectsRoot(workspace).createFolder(path);
        return Response.created(URI.create("")).build(); // TODO: Location
    }

    @DELETE
    @Path("{path:.*}")
    public void delete(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final VirtualFileEntry entry = projectManager.getProjectsRoot(workspace).getChild(path);
        if (entry == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid path %s. Path doesn't exists. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        entry.remove();
    }

/*
GET	    /children/{parent:.*}
POST	/copy/{path:.*}?to={path}
POST	/move/{path:.*}?to={path}
POST	/rename/{path:.*}?name={name}

POST	/import/{path:.*}?name={name}
POST	/export/{path:.*}
GET	    /search/{path:.*}
*/


    private FileEntry asFile(String workspace, String path) {
        final FolderEntry root = projectManager.getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid path %s. Path doesn't exists. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        if (!entry.isFile()) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid path %s. Item isn't a file. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return (FileEntry)entry;
    }

    private FolderEntry asFolder(String workspace, String path) {
        final FolderEntry root = projectManager.getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid parent path %s. Path doesn't exists. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        if (!entry.isFolder()) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid parent path %s. Item isn't a folder. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return (FolderEntry)entry;
    }

    private ProjectDescription toDescription(ProjectDescriptor descriptor) throws InvalidProjectTypeException {
        final ProjectType projectType = projectTypeRegistry.getProjectType(descriptor.getProjectTypeId());
        if (projectType == null) {
            throw new InvalidProjectTypeException(descriptor.getProjectTypeId());
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        final Map<String, List<String>> projectAttributeValues = descriptor.getAttributes();
        if (!(projectAttributeValues == null || projectAttributeValues.isEmpty())) {
            final List<Attribute> projectAttributes = new ArrayList<>(projectAttributeValues.size());
            for (Map.Entry<String, List<String>> e : projectAttributeValues.entrySet()) {
                projectAttributes.add(new Attribute(e.getKey(), e.getValue()));
            }
            projectDescription.setAttributes(projectAttributes);
        }
        projectDescription.setDescription(descriptor.getDescription());
        return projectDescription;
    }

    private ProjectDescriptor toDescriptor(Project project) throws IOException {
        final ProjectDescription description = project.getDescription();
        final ProjectType type = description.getProjectType();
        final Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        for (Attribute attribute : description.getAttributes()) {
            attributeValues.put(attribute.getName(), attribute.getValues());
        }
        return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                         .withProjectTypeId(type.getId())
                         .withProjectTypeName(type.getName())
                         .withDescription(description.getDescription())
                         .withVisibility("public")
                         .withAttributes(attributeValues)
                         .withModificationDate(project.getBaseFolder().getLastModificationDate())
                         .withLinks(generateProjectLinks(project));
        // TODO: check project visibility
    }

    private List<Link> generateProjectLinks(Project project) {
        final UriBuilder thisServiceUriBuilder = getServiceContext().getServiceUriBuilder();
        return Collections.emptyList(); // TODO
    }
}
