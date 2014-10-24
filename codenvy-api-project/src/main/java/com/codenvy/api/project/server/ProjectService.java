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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.core.util.LineConsumerFactory;
import com.codenvy.api.project.shared.EnvironmentId;
import com.codenvy.api.project.shared.dto.GenerateDescriptor;
import com.codenvy.api.project.shared.dto.ImportProject;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectProblem;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.ProjectUpdate;
import com.codenvy.api.project.shared.dto.RunnerEnvironment;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentLeaf;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentTree;
import com.codenvy.api.project.shared.dto.RunnerSource;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.project.shared.dto.TreeElement;
import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemImpl;
import com.codenvy.api.vfs.server.search.QueryExpression;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 * @author Artem Zatsarynnyy
 */
@Api(value = "/project",
     description = "Project manager")
@Path("/project/{ws-id}")
public class ProjectService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    @Inject
    private ProjectManager              projectManager;
    @Inject
    private ProjectImporterRegistry     importers;
    @Inject
    private ProjectGeneratorRegistry    generators;
    @Inject
    private SearcherProvider            searcherProvider;
    @Inject
    private ProjectTypeResolverRegistry resolverRegistry;
    @Inject
    private EventService                eventService;

    @ApiOperation(value = "Gets list of projects in root folder",
                  response = ProjectReference.class,
                  responseContainer = "List",
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server error")})
    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectReference> getProjects(@ApiParam(value = "ID of workspace to get projects", required = true)
                                              @PathParam("ws-id") String workspace) throws IOException, ServerException, ConflictException {
        final List<Project> projects = projectManager.getProjects(workspace);
        final List<ProjectReference> projectReferences = new ArrayList<>(projects.size());
        for (Project project : projects) {
            try {
                projectReferences.add(DtoConverter.toReferenceDto(project, getServiceContext().getServiceUriBuilder()));
            } catch (RuntimeException e) {
                // Ignore known error for single project.
                // In result we won't have them in explorer tree but at least 'bad' projects won't prevent to show 'good' projects.
                LOG.error(e.getMessage(), e);
            }
        }
        return projectReferences;
    }

    @ApiOperation(value = "Gets project by ID of workspace and project's path",
                  response = ProjectDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Project with specified path doesn't exist in workspace"),
            @ApiResponse(code = 403, message = "Access to requested project is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@ApiParam(value = "ID of workspace to get projects", required = true)
                                        @PathParam("ws-id") String workspace,
                                        @ApiParam(value = "Path to requested project", required = true)
                                        @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
        }
        return DtoConverter.toDescriptorDto(project, getServiceContext().getServiceUriBuilder());
    }

    @ApiOperation(value = "Creates new project",
                  response = ProjectDescriptor.class,
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Operation is forbidden"),
            @ApiResponse(code = 409, message = "Project with specified name already exist in workspace"),
            @ApiResponse(code = 500, message = "Server error")})

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@ApiParam(value = "ID of workspace to create project", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Name for new project", required = true)
                                           @Required
                                           @Description("project name")
                                           @QueryParam("name") String name,
                                           @Description("descriptor of project") NewProject newProject)
            throws ConflictException, ForbiddenException, ServerException {
        final Project project = projectManager.createProject(workspace, name,
                                                             DtoConverter.fromDto(newProject, projectManager.getTypeDescriptionRegistry()));
        final String visibility = newProject.getVisibility();
        final ProjectMisc misc = project.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!

        if (visibility != null) {
            project.setVisibility(visibility);
        }
        final ProjectDescriptor descriptor = DtoConverter.toDescriptorDto(project, getServiceContext().getServiceUriBuilder());
        eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", descriptor.getName(),
                 descriptor.getType(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return descriptor;
    }

    @ApiOperation(value = "Get project modules",
                  notes = "Get project modules. Roles allowed: system/admin, system/manager.",
                  response = ProjectDescriptor.class,
                  responseContainer = "List",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/modules/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectDescriptor> getModules(@ApiParam(value = "Workspace ID", required = true)
                                              @PathParam("ws-id") String workspace,
                                              @ApiParam(value = "Path to a project", required = true)
                                              @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {
        final FolderEntry folder = asFolder(workspace, path);
        final List<ProjectDescriptor> modules = new LinkedList<>();
        for (FolderEntry childFolder : folder.getChildFolders()) {
            if (childFolder.isProjectFolder()) {
                try {
                    modules.add(DtoConverter.toDescriptorDto(new Project(childFolder, projectManager),
                                                             getServiceContext().getServiceUriBuilder()));
                } catch (RuntimeException e) {
                    // Ignore known error for single module.
                    // In result we won't have them in project tree but at least 'bad' modules won't prevent to show 'good' modules.
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return modules;
    }

    @ApiOperation(value = "Create a new module",
                  notes = "Create a new module in a specified project",
                  response = ProjectDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Module already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createModule(@ApiParam(value = "Workspace ID", required = true)
                                          @PathParam("ws-id") String workspace,
                                          @ApiParam(value = "Path to a target directory", required = true)
                                          @PathParam("path") String parentPath,
                                          @ApiParam(value = "New module name", required = true)
                                          @QueryParam("name") String name,
                                          NewProject newProject)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, parentPath);
        final FolderEntry moduleFolder = folder.createFolder(name);
        final Project module = new Project(moduleFolder, projectManager);
        module.updateDescription(DtoConverter.fromDto(newProject, projectManager.getTypeDescriptionRegistry()));
        final ProjectDescriptor descriptor = DtoConverter.toDescriptorDto(module, getServiceContext().getServiceUriBuilder());
        eventService.publish(new ProjectCreatedEvent(module.getWorkspace(), module.getPath()));
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", descriptor.getName(),
                 descriptor.getType(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return descriptor;
    }

    @ApiOperation(value = "Updates existing project",
                  response = ProjectDescriptor.class,
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Project with specified path doesn't exist in workspace"),
            @ApiResponse(code = 403, message = "Operation is forbidden"),
            @ApiResponse(code = 409, message = "Update operation causes conflicts"),
            @ApiResponse(code = 500, message = "Server error")})
    @PUT
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor updateProject(@ApiParam(value = "ID of workspace", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Path to updated project", required = true)
                                           @PathParam("path") String path,
                                           ProjectUpdate update)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
        }
        project.updateDescription(DtoConverter.fromDto(update, projectManager.getTypeDescriptionRegistry()));
        return DtoConverter.toDescriptorDto(project, getServiceContext().getServiceUriBuilder());
    }

    @ApiOperation(value = "Create file",
                  notes = "Create a new file in a project. If file type isn't specified the server will resolve its type.",
                  position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/file/{parent:.*}")
    public Response createFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Path to a target directory", required = true)
                               @PathParam("parent") String parentPath,
                               @ApiParam(value = "New file name", required = true)
                               @QueryParam("name") String fileName,
                               @ApiParam(value = "New file content type")
                               @HeaderParam("content-type") MediaType contentType,
                               InputStream content) throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, parentPath);
        // Have issue with client side. Always have Content-type header is set even if client doesn't set it.
        // In this case have Content-type is set with "text/plain; charset=UTF-8" which isn't acceptable.
        // Have agreement with client to send Content-type header with "application/unknown" value if client doesn't want to specify media
        // type of new file. In this case server takes care about resolving media type of file.
        final FileEntry newFile;
        if (contentType == null || ("application".equals(contentType.getType()) && "unknown".equals(contentType.getSubtype()))) {
            newFile = parent.createFile(fileName, content, null);
        } else {
            newFile = parent.createFile(fileName, content, (contentType.getType() + '/' + contentType.getSubtype()));
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final ItemReference fileReference = DtoConverter.toItemReferenceDto(newFile, uriBuilder.clone());
        final URI location = uriBuilder.clone().path(getClass(), "getFile").build(workspace, newFile.getPath().substring(1));
        return Response.created(location).entity(fileReference).build();
    }

    @ApiOperation(value = "Create a folder",
                  notes = "Create a folder is a specified project",
                  position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/folder/{path:.*}")
    public Response createFolder(@ApiParam(value = "Workspace ID", required = true)
                                 @PathParam("ws-id") String workspace,
                                 @ApiParam(value = "Path to a new folder destination", required = true)
                                 @PathParam("path") String path)
            throws ConflictException, ForbiddenException, ServerException {
        final FolderEntry newFolder = projectManager.getProjectsRoot(workspace).createFolder(path);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final ItemReference folderReference = DtoConverter.toItemReferenceDto(newFolder, uriBuilder.clone());
        final URI location = uriBuilder.clone().path(getClass(), "getChildren").build(workspace, newFolder.getPath().substring(1));
        return Response.created(location).entity(folderReference).build();
    }

    @ApiOperation(value = "Upload a file",
                  notes = "Upload a new file",
                  position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.TEXT_HTML})
    @Path("/uploadfile/{parent:.*}")
    public Response uploadFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Destination path", required = true)
                               @PathParam("parent") String parentPath,
                               Iterator<FileItem> formData)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, parentPath);
        return VirtualFileSystemImpl.uploadFile(parent.getVirtualFile(), formData);
    }

    @ApiOperation(value = "Get file content",
                  notes = "Get file content by its name",
                  position = 10)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/file/{path:.*}")
    public Response getFile(@ApiParam(value = "Workspace ID", required = true)
                            @PathParam("ws-id") String workspace,
                            @ApiParam(value = "Path to a file", required = true)
                            @PathParam("path") String path)
            throws IOException, NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        return Response.ok().entity(file.getInputStream()).type(file.getMediaType()).build();
    }

    @ApiOperation(value = "Update file",
                  notes = "Update an existing file with new content",
                  position = 11)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @PUT
    @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
    @Path("/file/{path:.*}")
    public Response updateFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Full path to a file", required = true)
                               @PathParam("path") String path,
                               @ApiParam(value = "Media Type")
                               @HeaderParam("content-type") MediaType contentType,
                               InputStream content) throws NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        // Have issue with client side. Always have Content-type header is set even if client doesn't set it.
        // In this case have Content-type is set with "text/plain; charset=UTF-8" which isn't acceptable.
        // Have agreement with client to send Content-type header with "application/unknown" value if client doesn't want to specify media
        // type of new file. In this case server takes care about resolving media type of file.
        if (contentType == null || ("application".equals(contentType.getType()) && "unknown".equals(contentType.getSubtype()))) {
            file.updateContent(content);
        } else {
            file.updateContent(content, contentType.getType() + '/' + contentType.getSubtype());
        }
        return Response.ok().build();
    }

    @ApiOperation(value = "Delete a resource",
                  notes = "Delete resources. If you want to delete a single project, specify project name. If a folder or file needs to " +
                          "be deleted a path to the requested resource needs to be specified",
                  position = 12)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{path:.*}")
    public void delete(@ApiParam(value = "Workspace ID", required = true)
                       @PathParam("ws-id") String workspace,
                       @ApiParam(value = "Path to a resource to be deleted", required = true)
                       @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
            // In case of folder extract some information about project for logger before delete project.
            Project project = new Project((FolderEntry)entry, projectManager);
            final String name = project.getName();
            String projectType = null;
            try {
                projectType = project.getDescription().getProjectType().getId();
            } catch (ServerException | ValueStorageException e) {
                // Let delete even project in invalid state.
                LOG.error(e.getMessage(), e);
            }
            entry.remove();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        } else {
            entry.remove();
        }
    }

    @ApiOperation(value = "Copy resource",
                  notes = "Copy resource to a new location which is specified in a query parameter",
                  position = 13)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/copy/{path:.*}")
    public Response copy(@ApiParam(value = "Workspace ID", required = true)
                         @PathParam("ws-id") String workspace,
                         @ApiParam(value = "Path to a resource", required = true)
                         @PathParam("path") String path,
                         @ApiParam(value = "Path to a new location", required = true)
                         @QueryParam("to") String newParent)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        final VirtualFileEntry copy = entry.copyTo(newParent);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), copy.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, copy.getPath().substring(1));
        if (copy.isFolder() && ((FolderEntry)copy).isProjectFolder()) {
            Project project = new Project((FolderEntry)copy, projectManager);
            final String name = project.getName();
            final String projectType = project.getDescription().getProjectType().getId();
            entry.remove();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(location).build();
    }

    @ApiOperation(value = "Move resource",
                  notes = "Move resource to a new location which is specified in a query parameter",
                  position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/move/{path:.*}")
    public Response move(@ApiParam(value = "Workspace ID", required = true)
                         @PathParam("ws-id") String workspace,
                         @ApiParam(value = "Path to a resource to be moved", required = true)
                         @PathParam("path") String path,
                         @ApiParam(value = "Path to a new location", required = true)
                         @QueryParam("to") String newParent)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        entry.moveTo(newParent);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
            Project project = new Project((FolderEntry)entry, projectManager);
            final String name = project.getName();
            final String projectType = project.getDescription().getProjectType().getId();
            entry.remove();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(location).build();
    }

    @ApiOperation(value = "Rename resource",
                  notes = "Rename resources. It can be project, module, folder or file",
                  position = 15)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/rename/{path:.*}")
    public Response rename(@ApiParam(value = "Workspace ID", required = true)
                           @PathParam("ws-id") String workspace,
                           @ApiParam(value = "Path to resource to be renamed", required = true)
                           @PathParam("path") String path,
                           @ApiParam(value = "New name", required = true)
                           @QueryParam("name") String newName,
                           @ApiParam(value = "New media type")
                           @QueryParam("mediaType") String newMediaType)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (entry.isFile() && newMediaType != null) {
            // Use the same rules as in method createFile to make client side simpler.
            ((FileEntry)entry).rename(newName, newMediaType);
        } else {
            entry.rename(newName);
        }
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        return Response.created(location).build();
    }

    @ApiOperation(value = "Import resource",
                  notes = "Import resource. JSON with a designated importer and project location is sent. It is possible to import from " +
                          "VCS or ZIP",
                  response = ProjectDescriptor.class,
                  position = 16)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 401, message = "User not authorized to call this operation"),
            @ApiResponse(code = 403, message = "Forbidden operation"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Unsupported source type")})
    @POST
    @Path("/import/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor importProject(@ApiParam(value = "Workspace ID", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Path in the project", required = true)
                                           @PathParam("path") String path,
                                           @ApiParam(value = "Force rewrite existing project", allowableValues = "true,false")
                                           @QueryParam("force") boolean force,
                                           ImportProject importProject)
            throws ConflictException, ForbiddenException, UnauthorizedException, IOException, ServerException {
        final ImportSourceDescriptor projectSource = importProject.getSource().getProject();
        final ProjectImporter importer = importers.getImporter(projectSource.getType());
        if (importer == null) {
            throw new ServerException(String.format("Unable import sources project from '%s'. Sources type '%s' is not supported.",
                                                    projectSource.getLocation(), projectSource.getType()));
        }
        // Preparing websocket output publisher to broadcast output of import process to the ide clients while importing
        final String fWorkspace = workspace;
        final String fPath = path;
        final LineConsumerFactory outputOutputConsumerFactory = new LineConsumerFactory() {
            @Override
            public LineConsumer newLineConsumer() {
                return new ProjectImportOutputWSLineConsumer(fPath, fWorkspace, 300);
            }
        };

        // Not all importers uses virtual file system API. In this case virtual file system API doesn't get events and isn't able to set
        // correct creation time. Need do it manually.
        long creationDate = -1;
        VirtualFileEntry virtualFile = projectManager.getProjectsRoot(workspace).getChild(path);
        if (virtualFile != null && virtualFile.isFile()) {
            // File with same name exist already exists.
            throw new ConflictException(String.format("File with the name '%s' already exists.", path));
        } else {
            if (virtualFile == null) {
                creationDate = System.currentTimeMillis();
                virtualFile = projectManager.getProjectsRoot(workspace).createFolder(path);
            } else if (!force) {
                // Project already exists.
                throw new ConflictException(String.format("Project with the name '%s' already exists.", path));
            }
        }

        final FolderEntry baseProjectFolder = (FolderEntry)virtualFile;
        importer.importSources(baseProjectFolder, projectSource.getLocation(), projectSource.getParameters(), outputOutputConsumerFactory);

        // Use resolver only if project type not set
        ProjectProblem resolved = null;

        String visibility = null;

        Project project = projectManager.getProject(workspace, path);

        if (importProject.getProject() != null) {  //project configuration set in Source we will use it
            visibility = importProject.getProject().getVisibility();
            if (project == null) {
                project = new Project(baseProjectFolder, projectManager);
                project.updateDescription(DtoConverter.fromDto(importProject.getProject(), projectManager.getTypeDescriptionRegistry()));
            } else {
                project.updateDescription(DtoConverter.fromDto(importProject.getProject(), projectManager.getTypeDescriptionRegistry()));
            }
        } else { //project not configure so we try resolve it
            if (project == null) {
                Set<ProjectTypeResolver> resolvers = resolverRegistry.getResolvers();
                for (ProjectTypeResolver resolver : resolvers) {
                    if (resolver.resolve((FolderEntry)virtualFile)) {
                        resolved = DtoFactory.getInstance().createDto(ProjectProblem.class).withCode(300)
                                             .withMessage("Project type detect via ProjectResolver");
                        break;
                    }
                }
                // Try get project again after trying resolve it
                project = projectManager.getProject(workspace, path);
                if (project == null) { //resolver can't resolve project type
                    project = new Project(baseProjectFolder, projectManager); //create BLANK project type
                    project.updateDescription(new ProjectDescription());
                    resolved = DtoFactory.getInstance().createDto(ProjectProblem.class).withCode(301)
                                         .withMessage("Project type not detect so we set it as blank");
                }
            }
        }
        // Some importers don't use virtual file system API and changes are not indexed.
        // Force searcher to reindex project to fix such issues.
        VirtualFile file = project.getBaseFolder().getVirtualFile();
        searcherProvider.getSearcher(file.getMountPoint(), true).add(file);
        if (creationDate > 0) {
            final ProjectMisc misc = project.getMisc();
            misc.setCreationDate(creationDate);
            misc.save(); // Important to save misc!!
        }

        VirtualFileEntry environmentsFolder = baseProjectFolder.getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        if (environmentsFolder != null && environmentsFolder.isFile()) {
            throw new ConflictException(String.format("Unable import runner environments. File with the name '%s' already exists.",
                                                      Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR));
        } else if (environmentsFolder == null) {
            environmentsFolder = baseProjectFolder.createFolder(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        }

        for (Map.Entry<String, RunnerSource> runnerSource : importProject.getSource().getRunners().entrySet()) {
            final String runnerSourceKey = runnerSource.getKey();
            if (runnerSourceKey.startsWith("/docker/")) {
                final RunnerSource runnerSourceValue = runnerSource.getValue();
                if (runnerSourceValue != null) {
                    String name = runnerSourceKey.substring(8);
                    String runnerSourceLocation = runnerSourceValue.getLocation();
                    if (runnerSourceLocation.startsWith("https") || runnerSourceLocation.startsWith("http")) {
                        try (InputStream in = new java.net.URL(runnerSourceLocation).openStream()) {
                            // Add file without mediatype to avoid creation useless metadata files on virtual file system level.
                            // Dockerfile add in list of known files, see com.codenvy.api.core.util.ContentTypeGuesser
                            // and content-types.properties file.
                            ((FolderEntry)environmentsFolder).createFolder(name).createFile("Dockerfile", in, null);
                        }
                    } else {
                        LOG.warn(
                                "ProjectService.importProject :: not valid runner source location availabel only http or https scheme but" +
                                " we get :" +
                                runnerSourceLocation);
                    }

                }
            }
        }

        //set project visibility if needed
        if (visibility != null) {
            project.setVisibility(visibility);
        }

        eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
        final ProjectDescriptor projectDescriptor = DtoConverter.toDescriptorDto(project, getServiceContext().getServiceUriBuilder());
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", projectDescriptor.getName(),
                 projectDescriptor.getType(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        if (resolved != null) {
            List<ProjectProblem> projectProblems = projectDescriptor.getProblems();
            projectProblems.add(resolved);
            projectDescriptor.setProblems(projectProblems);
        }
        return projectDescriptor;
    }

    @ApiOperation(value = "Generate a project",
                  notes = "Generate a project of a particular type",
                  response = ProjectDescriptor.class,
                  position = 17)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 403, message = "Forbidden operation"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Unable to generate project. Unknown generator is used")})
    @POST
    @Path("/generate/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor generateProject(@ApiParam(value = "Workspace ID", required = true)
                                             @PathParam("ws-id") String workspace,
                                             @ApiParam(value = "Path to a new project", required = true)
                                             @PathParam("path") String path,
                                             @Description("descriptor of project generator") GenerateDescriptor generateDescriptor)
            throws ConflictException, ForbiddenException, ServerException {
        final ProjectGenerator generator = generators.getGenerator(generateDescriptor.getGeneratorName());
        if (generator == null) {
            throw new ServerException(
                    String.format("Unable generate project. Unknown generator '%s'.", generateDescriptor.getGeneratorName()));
        }
        Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            project = projectManager.createProject(workspace, path, new ProjectDescription());
        }
        generator.generateProject(project.getBaseFolder(), generateDescriptor.getOptions());
        final String visibility = generateDescriptor.getProjectVisibility();
        if (visibility != null) {
            project.setVisibility(visibility);
        }
        final ProjectDescriptor projectDescriptor = DtoConverter.toDescriptorDto(project, getServiceContext().getServiceUriBuilder());
        eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", projectDescriptor.getName(),
                 projectDescriptor.getType(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return projectDescriptor;
    }

    @ApiOperation(value = "Import zip",
                  notes = "Import resources as zip",
                  position = 18)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/import/{path:.*}")
    @Consumes("application/zip")
    public Response importZip(@ApiParam(value = "Workspace ID", required = true)
                              @PathParam("ws-id") String workspace,
                              @ApiParam(value = "Path to a location (where import to?)")
                              @PathParam("path") String path,
                              InputStream zip) throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, path);
        VirtualFileSystemImpl.importZip(parent.getVirtualFile(), zip, true);
        if (parent.isProjectFolder()) {
            Project project = new Project(parent, projectManager);
            eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
            final String projectType = project.getDescription().getProjectType().getId();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", path, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, parent.getPath().substring(1))).build();
    }

    @ApiOperation(value = "Download ZIP",
                  notes = "Export resource as zip. It can be an entire project or folder",
                  position = 19)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/export/{path:.*}")
    @Produces("application/zip")
    public ContentStream exportZip(@ApiParam(value = "Workspace ID", required = true)
                                   @PathParam("ws-id") String workspace,
                                   @ApiParam(value = "Path to resource to be imported")
                                   @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile());
    }

    @POST
    @Path("/export/{path:.*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("application/zip")
    public Response exportDiffZip(@PathParam("ws-id") String workspace, @PathParam("path") String path, InputStream in)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile(), in);
    }

    @POST
    @Path("/export/{path:.*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public Response exportDiffZipMultipart(@PathParam("ws-id") String workspace, @PathParam("path") String path, InputStream in)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZipMultipart(folder.getVirtualFile(), in);
    }

    @ApiOperation(value = "Get project children items",
                  notes = "Request all children items for a project, such as files and folders",
                  response = ItemReference.class,
                  responseContainer = "List",
                  position = 20)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/children/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> getChildren(@ApiParam(value = "Workspace ID", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Path to a project", required = true)
                                           @PathParam("parent") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        final List<VirtualFileEntry> children = folder.getChildren();
        final ArrayList<ItemReference> result = new ArrayList<>(children.size());
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        for (VirtualFileEntry child : children) {
            if (child.isFile()) {
                result.add(DtoConverter.toItemReferenceDto((FileEntry)child, uriBuilder.clone()));
            } else {
                result.add(DtoConverter.toItemReferenceDto((FolderEntry)child, uriBuilder.clone()));
            }
        }
        return result;
    }

    @ApiOperation(value = "Get project tree",
                  notes = "Get project tree. Depth is specified in a query parameter",
                  response = TreeElement.class,
                  responseContainer = "List",
                  position = 21)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/tree/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeElement getTree(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Path to resource. Can be project or its folders", required = true)
                               @PathParam("parent") String path,
                               @ApiParam(value = "Tree depth. This parameter can be dropped. If not specified ?depth=1 is used by default")
                               @DefaultValue("1") @QueryParam("depth") int depth)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        return dtoFactory.createDto(TreeElement.class)
                         .withNode(DtoConverter.toItemReferenceDto(folder, uriBuilder.clone()))
                         .withChildren(getTree(folder, depth, uriBuilder, dtoFactory));
    }

    private List<TreeElement> getTree(FolderEntry folder, int depth, UriBuilder uriBuilder, DtoFactory dtoFactory) throws ServerException {
        if (depth == 0) {
            return null;
        }
        final List<FolderEntry> childFolders = folder.getChildFolders();
        final List<TreeElement> nodes = new ArrayList<>(childFolders.size());
        for (FolderEntry childFolder : childFolders) {
            nodes.add(dtoFactory.createDto(TreeElement.class)
                                .withNode(DtoConverter.toItemReferenceDto(childFolder, uriBuilder.clone()))
                                .withChildren(getTree(childFolder, depth - 1, uriBuilder, dtoFactory)));
        }
        return nodes;
    }

    @ApiOperation(value = "Search for resources",
                  notes = "Search for resources applying a number of search filters as query parameters",
                  response = ItemReference.class,
                  responseContainer = "List",
                  position = 22)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Conflict error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/search/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> search(@ApiParam(value = "Workspace ID", required = true)
                                      @PathParam("ws-id") String workspace,
                                      @ApiParam(value = "Path to resource, i.e. where to search?", required = true)
                                      @PathParam("path") String path,
                                      @ApiParam(value = "Resource name")
                                      @QueryParam("name") String name,
                                      @ApiParam(value = "Media type")
                                      @QueryParam("mediatype") String mediatype,
                                      @ApiParam(value = "Search keywords")
                                      @QueryParam("text") String text,
                                      @ApiParam(value = "Maximum items to display. If this parameter is dropped, there are no limits")
                                      @QueryParam("maxItems") @DefaultValue("-1") int maxItems,
                                      @ApiParam(value = "Skip count")
                                      @QueryParam("skipCount") int skipCount)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        if (searcherProvider != null) {
            if (skipCount < 0) {
                throw new ConflictException(String.format("Invalid 'skipCount' parameter: %d.", skipCount));
            }
            final QueryExpression expr = new QueryExpression()
                    .setPath(path.startsWith("/") ? path : ('/' + path))
                    .setName(name)
                    .setMediaType(mediatype)
                    .setText(text);

            final String[] result = searcherProvider.getSearcher(folder.getVirtualFile().getMountPoint(), true).search(expr);
            if (skipCount > 0) {
                if (skipCount > result.length) {
                    throw new ConflictException(
                            String.format("'skipCount' parameter: %d is greater then total number of items in result: %d.",
                                          skipCount, result.length));
                }
            }
            final int length = maxItems > 0 ? Math.min(result.length, maxItems) : result.length;
            final List<ItemReference> items = new ArrayList<>(length);
            final FolderEntry root = projectManager.getProjectsRoot(workspace);
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            for (int i = skipCount; i < length; i++) {
                VirtualFileEntry child = null;
                try {
                    child = root.getChild(result[i]);
                } catch (ForbiddenException ignored) {
                    // Ignore item that user can't access
                }
                if (child != null && child.isFile()) {
                    items.add(DtoConverter.toItemReferenceDto((FileEntry)child, uriBuilder.clone()));
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    @ApiOperation(value = "Get user permissions in a project",
                  notes = "Get permissions for a user in a specified project, such as read, write, build, " +
                          "run etc. ID of a user is set in a query parameter of a request URL.",
                  response = AccessControlEntry.class,
                  responseContainer = "List",
                  position = 23)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/permissions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> getPermissions(@ApiParam(value = "Workspace ID", required = true)
                                                   @PathParam("ws-id") String wsId,
                                                   @ApiParam(value = "Path to a project", required = true)
                                                   @PathParam("path") String path,
                                                   @ApiParam(value = "User ID", required = true)
                                                   @QueryParam("userid") Set<String> users)
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, wsId));
        }
        final List<AccessControlEntry> acl = project.getPermissions();
        if (!(users == null || users.isEmpty())) {
            for (Iterator<AccessControlEntry> itr = acl.iterator(); itr.hasNext(); ) {
                final AccessControlEntry ace = itr.next();
                final Principal principal = ace.getPrincipal();
                if (principal.getType() != Principal.Type.USER || !users.contains(principal.getName())) {
                    itr.remove();
                }
            }
        }
        return acl;
    }

    @ApiOperation(value = "Set project visibility",
                  notes = "Set project visibility. Projects can be private or public",
                  position = 24)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/switch_visibility/{path:.*}")
    @RolesAllowed("workspace/admin")
    public void switchVisibility(@ApiParam(value = "Workspace ID", required = true)
                                 @PathParam("ws-id") String wsId,
                                 @ApiParam(value = "Path to a project", required = true)
                                 @PathParam("path") String path,
                                 @ApiParam(value = "Visibility type", required = true, allowableValues = "public,private")
                                 @QueryParam("visibility") String visibility)
            throws NotFoundException, ForbiddenException, ServerException {
        if (visibility == null || visibility.isEmpty()) {
            throw new ServerException(String.format("Invalid visibility '%s'", visibility));
        }
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, wsId));
        }
        project.setVisibility(visibility);
    }

    @ApiOperation(value = "Set permissions for a user in a project",
                  notes = "Set permissions for a user in a specified project, such as read, write, build, " +
                          "run etc. ID of a user is set in a query parameter of a request URL.",
                  response = AccessControlEntry.class,
                  responseContainer = "List",
                  position = 25)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/permissions/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> setPermissions(@ApiParam(value = "Workspace ID", required = true)
                                                   @PathParam("ws-id") String wsId,
                                                   @ApiParam(value = "Path to a project", required = true)
                                                   @PathParam("path") String path,
                                                   @ApiParam(value = "Permissions", required = true)
                                                   List<AccessControlEntry> acl) throws ForbiddenException, ServerException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new ServerException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, wsId));
        }
        project.setPermissions(acl);
        return project.getPermissions();
    }

    @ApiOperation(value = "Get available project-scoped runner environments",
                  notes = "Get available project-scoped runner environments.",
                  response = RunnerEnvironmentTree.class,
                  position = 26)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/runner_environments/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerEnvironmentTree getRunnerEnvironments(@ApiParam(value = "Workspace ID", required = true)
                                                       @PathParam("ws-id") String workspace,
                                                       @ApiParam(value = "Path to a project", required = true)
                                                       @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, path);
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final RunnerEnvironmentTree root = dtoFactory.createDto(RunnerEnvironmentTree.class).withDisplayName("project");
        final List<RunnerEnvironmentLeaf> environments = new LinkedList<>();
        final VirtualFileEntry environmentsFolder = project.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        if (environmentsFolder != null && environmentsFolder.isFolder()) {
            for (FolderEntry childFolder : ((FolderEntry)environmentsFolder).getChildFolders()) {
                final String id = new EnvironmentId(EnvironmentId.Scope.project, childFolder.getName()).toString();
                environments.add(dtoFactory.createDto(RunnerEnvironmentLeaf.class)
                                           .withEnvironment(dtoFactory.createDto(RunnerEnvironment.class).withId(id))
                                           .withDisplayName(childFolder.getName()));
            }
        }
        return root.withLeaves(environments);
    }

    private FileEntry asFile(String workspace, String path) throws ForbiddenException, NotFoundException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFile()) {
            throw new ForbiddenException(String.format("Item '%s' isn't a file. ", path));
        }
        return (FileEntry)entry;
    }

    private FolderEntry asFolder(String workspace, String path) throws ForbiddenException, NotFoundException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFolder()) {
            throw new ForbiddenException(String.format("Item '%s' isn't a file. ", path));
        }
        return (FolderEntry)entry;
    }

    private VirtualFileEntry getVirtualFileEntry(String workspace, String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry root = projectManager.getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        return entry;
    }
}
