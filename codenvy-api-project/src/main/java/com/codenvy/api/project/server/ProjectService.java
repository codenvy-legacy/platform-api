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
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.ProjectUpdate;
import com.codenvy.api.project.shared.dto.TreeElement;
import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemImpl;
import com.codenvy.api.vfs.server.search.QueryExpression;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Api(value = "/project/{ws-id}",
     description = "Project manager")
@Path("/project/{ws-id}")
public class ProjectService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectService.class);

    @Inject
    private ProjectManager           projectManager;
    @Inject
    private ProjectImporterRegistry  importers;
    @Inject
    private ProjectGeneratorRegistry generators;
    @Inject
    private SearcherProvider         searcherProvider;

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
                                              @PathParam("ws-id") String workspace) throws IOException, ServerException {
        final List<Project> projects = projectManager.getProjects(workspace);
        final List<ProjectReference> projectRefs = new ArrayList<>(projects.size());
        for (Project project : projects) {
            projectRefs.add(toReference(project));
        }
        return projectRefs;
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
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
        }
        return toDescriptor(project);
    }

    @ApiOperation(value = "Creates new project",
                  response = ProjectDescriptor.class,
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Operation is forbidden"),
            @ApiResponse(code = 409, message = "Project with specified name already exist in workspace"),
            @ApiResponse(code = 500, message = "Server error")})
    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @POST
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
        final Project project = projectManager.createProject(workspace, name, toDescription(newProject));
        final String visibility = newProject.getVisibility();
        if (visibility != null) {
            project.setVisibility(visibility);
        }
        final ProjectDescriptor descriptor = toDescriptor(project);
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", descriptor.getName(),
                 descriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return descriptor;
    }

    @GET
    @Path("modules/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectDescriptor> getModules(@PathParam("ws-id") String workspace, @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, workspace));
        }
        final List<Project> modules = project.getModules();
        final List<ProjectDescriptor> descriptors = new ArrayList<>(modules.size());
        for (Project module : modules) {
            descriptors.add(toDescriptor(module));
        }
        return descriptors;
    }

    @POST
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createModule(@PathParam("ws-id") String workspace,
                                          @PathParam("path") String parentProject,
                                          @QueryParam("name") String name,
                                          NewProject newProject)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, parentProject);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", parentProject, workspace));
        }
        final Project module = project.createModule(name, toDescription(newProject));
        final ProjectDescriptor descriptor = toDescriptor(module);
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", descriptor.getName(),
                 descriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return descriptor;
    }

    @ApiOperation(value = "Updates existed project",
                  response = ProjectDescriptor.class,
                  position = 4)
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
        project.updateDescription(toDescription(update));
        return toDescriptor(project);
    }

    @POST
    @Path("file/{parent:.*}")
    public Response createFile(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String parentPath,
                               @QueryParam("name") String fileName,
                               @HeaderParam("content-type") String contentType,
                               InputStream content) throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FileEntry newFile = asFolder(workspace, parentPath).createFile(fileName, content, contentType);
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getFile")
                                                   .build(workspace, newFile.getPath().substring(1))).build();
    }

    @POST
    @Path("/folder/{path:.*}")
    public Response createFolder(@PathParam("ws-id") String workspace, @PathParam("path") String path)
            throws ConflictException, ForbiddenException, ServerException {
        final FolderEntry newFolder = projectManager.getProjectsRoot(workspace).createFolder(path);
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, newFolder.getPath().substring(1))).build();
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.TEXT_HTML})
    @Path("/uploadFile/{parent:.*}")
    public Response uploadFile(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String parentPath,
                               Iterator<FileItem> formData)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, parentPath);
        return VirtualFileSystemImpl.uploadFile(parent.getVirtualFile(), formData);
    }

    @GET
    @Path("/file/{path:.*}")
    public Response getFile(@PathParam("ws-id") String workspace, @PathParam("path") String path)
            throws IOException, NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        return Response.ok().entity(file.getInputStream()).type(file.getMediaType()).build();
    }

    @PUT
    @Path("/file/{path:.*}")
    public Response updateFile(@PathParam("ws-id") String workspace,
                               @PathParam("path") String path,
                               @HeaderParam("content-type") String contentType,
                               InputStream content) throws NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        file.updateContent(content, contentType);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{path:.*}")
    public void delete(@PathParam("ws-id") String workspace, @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
            // In case of folder extract some information about project for logger before delete project.
            Project project = new Project(workspace, (FolderEntry)entry, projectManager);
            final String name = project.getName();
            final String projectType = project.getDescription().getProjectType().getId();
            entry.remove();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        } else {
            entry.remove();
        }
    }

    @POST
    @Path("/copy/{path:.*}")
    public Response copy(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        final VirtualFileEntry copy = entry.copyTo(newParent);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), copy.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, copy.getPath().substring(1));
        if (copy.isFolder() && ((FolderEntry)copy).isProjectFolder()) {
            Project project = new Project(workspace, (FolderEntry)copy, projectManager);
            final String name = project.getName();
            final String projectType = project.getDescription().getProjectType().getId();
            entry.remove();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(location).build();
    }

    @POST
    @Path("/move/{path:.*}")
    public Response move(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        entry.moveTo(newParent);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
            Project project = new Project(workspace, (FolderEntry)entry, projectManager);
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

    @POST
    @Path("/rename/{path:.*}")
    public Response rename(@PathParam("ws-id") String workspace,
                           @PathParam("path") String path,
                           @QueryParam("name") String newName,
                           @QueryParam("mediaType") String newMediaType)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (entry.isFile() && newMediaType != null) {
            ((FileEntry)entry).rename(newName, newMediaType);
        } else {
            entry.rename(newName);
        }
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        return Response.created(location).build();
    }

    @POST
    @Path("/import/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor importProject(@PathParam("ws-id") String workspace,
                                           @PathParam("path") String path,
                                           ImportSourceDescriptor importDescriptor)
            throws ConflictException, ForbiddenException, UnauthorizedException, IOException, ServerException {
        final ProjectImporter importer = importers.getImporter(importDescriptor.getType());
        if (importer == null) {
            throw new ServerException(String.format("Unable import sources project from '%s'. Sources type '%s' is not supported.",
                                                    importDescriptor.getLocation(), importDescriptor.getType()));
        }
        // create project descriptor based on query parameters
        ProjectUpdate descriptorToUpdate = DtoFactory.getInstance().createDto(ProjectUpdate.class);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        for (String key : queryParameters.keySet()) {
            final String value = queryParameters.getFirst(key);
            if ("project.name".equals(key)) {
                path = value;
            } else if ("project.type".equals(key)) {
                descriptorToUpdate.setProjectTypeId(value);
            } else if (key.startsWith("project.attribute.")) {
                final String name = key.substring("project.attribute.".length());
                descriptorToUpdate.getAttributes().put(name, queryParameters.get(key));
            }
        }

        Project project = projectManager.getProject(workspace, path);
        boolean newProject = false;
        if (project == null) {
            newProject = true;
            project = projectManager.createProject(workspace, path, new ProjectDescription());
        }
        importer.importSources(project.getBaseFolder(), importDescriptor.getLocation());

        // Some importers don't use virtual file system API and changes are not indexed.
        // Force searcher to reindex project to fix such issues.
        VirtualFile virtualFile = project.getBaseFolder().getVirtualFile();
        searcherProvider.getSearcher(virtualFile.getMountPoint(), true).add(virtualFile);

        if (descriptorToUpdate.getProjectTypeId() != null) {
            project.updateDescription(toDescription(descriptorToUpdate));
        }

        final ProjectDescriptor projectDescriptor = toDescriptor(project);
        if (newProject) {
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", projectDescriptor.getName(),
                     projectDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                     EnvironmentContext.getCurrent().getUser().getName());
        }
        return projectDescriptor;
    }

    @POST
    @Path("/generate/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor generateProject(@PathParam("ws-id") String workspace,
                                             @PathParam("path") String path,
                                             @QueryParam("generator") String generatorName,
                                             Map<String, String> options) throws ConflictException, ForbiddenException, ServerException {
        final ProjectGenerator generator = generators.getGenerator(generatorName);
        if (generator == null) {
            throw new ServerException(String.format("Unable generate project. Unknown generator '%s'.", generatorName));
        }
        Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            project = projectManager.createProject(workspace, path, new ProjectDescription());
        }
        generator.generateProject(project.getBaseFolder(), options);
        final ProjectDescriptor projectDescriptor = toDescriptor(project);
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", projectDescriptor.getName(),
                 projectDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return projectDescriptor;
    }

    @POST
    @Path("/import/{path:.*}")
    @Consumes("application/zip")
    public Response importZip(@PathParam("ws-id") String workspace,
                              @PathParam("path") String path,
                              InputStream zip) throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, path);
        VirtualFileSystemImpl.importZip(parent.getVirtualFile(), zip, true);
        if (parent.isProjectFolder()) {
            Project project = new Project(workspace, parent, projectManager);
            final String projectType = project.getDescription().getProjectType().getId();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#", path, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, parent.getPath().substring(1))).build();
    }

    @GET
    @Path("/export/{path:.*}")
    @Produces("application/zip")
    public ContentStream exportZip(@PathParam("ws-id") String workspace, @PathParam("path") String path)
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

    @GET
    @Path("/children/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> getChildren(@PathParam("ws-id") String workspace, @PathParam("parent") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        final List<VirtualFileEntry> children = folder.getChildren();
        final ArrayList<ItemReference> result = new ArrayList<>(children.size());
        for (VirtualFileEntry child : children) {
            final ItemReference itemReference = DtoFactory.getInstance().createDto(ItemReference.class)
                                                          .withName(child.getName())
                                                          .withPath(child.getPath());
            if (child.isFile()) {
                itemReference.withType("file")
                             .withMediaType(((FileEntry)child).getMediaType())
                             .withLinks(generateFileLinks(workspace, (FileEntry)child));
            } else {
                itemReference.withType("folder")
                             .withMediaType("text/directory")
                             .withHasChildFiles(!((FolderEntry)child).getChildFiles().isEmpty())
                             .withLinks(generateFolderLinks(workspace, (FolderEntry)child));
            }
            result.add(itemReference);
        }
        return result;
    }

    @GET
    @Path("/tree/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeElement getTree(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String path,
                               @DefaultValue("1") @QueryParam("depth") int depth)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return DtoFactory.getInstance().createDto(TreeElement.class)
                         .withNode(DtoFactory.getInstance().createDto(ItemReference.class)
                                             .withName(folder.getName())
                                             .withPath(folder.getPath())
                                             .withType("folder")
                                             .withMediaType("text/directory")
                                             .withHasChildFiles(!folder.getChildFiles().isEmpty())
                                             .withLinks(generateFolderLinks(workspace, folder)))
                         .withChildren(getTree(workspace, folder, depth));
    }

    private List<TreeElement> getTree(String workspace, FolderEntry folder, int depth) throws ServerException {
        if (depth == 0) {
            return null;
        }
        final List<FolderEntry> childFolders = folder.getChildFolders();
        final List<TreeElement> nodes = new ArrayList<>(childFolders.size());
        for (FolderEntry childFolder : childFolders) {
            nodes.add(DtoFactory.getInstance().createDto(TreeElement.class)
                                .withNode(DtoFactory.getInstance().createDto(ItemReference.class)
                                                    .withName(childFolder.getName())
                                                    .withPath(childFolder.getPath())
                                                    .withType("folder")
                                                    .withMediaType("text/directory")
                                                    .withHasChildFiles(!childFolder.getChildFiles().isEmpty())
                                                    .withLinks(generateFolderLinks(workspace, childFolder)))
                                .withChildren(getTree(workspace, childFolder, depth - 1)));
        }
        return nodes;
    }

    @GET
    @Path("/search/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> search(@PathParam("ws-id") String workspace,
                                      @PathParam("path") String path,
                                      @QueryParam("name") String name,
                                      @QueryParam("mediatype") String mediatype,
                                      @QueryParam("text") String text,
                                      @QueryParam("maxItems") @DefaultValue("-1") int maxItems,
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
            for (int i = skipCount; i < length; i++) {
                VirtualFileEntry child = null;
                try {
                    child = root.getChild(result[i]);
                } catch (ForbiddenException ignored) {
                    // Ignore item that user can't access
                }
                if (child != null && child.isFile()) {
                    items.add(DtoFactory.getInstance().createDto(ItemReference.class)
                                        .withName(child.getName())
                                        .withPath(child.getPath())
                                        .withType("file")
                                        .withMediaType(((FileEntry)child).getMediaType())
                                        .withLinks(generateFileLinks(workspace, (FileEntry)child)));
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    @GET
    @Path("/permissions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> getPermissions(@PathParam("ws-id") String wsId,
                                                   @PathParam("path") String path,
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

    @POST
    @Path("/switch_visibility/{path:.*}")
    @RolesAllowed("workspace/admin")
    public void switchVisibility(@PathParam("ws-id") String wsId,
                                 @PathParam("path") String path,
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

    @POST
    @Path("/permissions/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public void setPermissions(@PathParam("ws-id") String wsId,
                               @PathParam("path") String path,
                               List<AccessControlEntry> acl) throws ForbiddenException, ServerException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new ServerException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, wsId));
        }
        project.setPermissions(acl);
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

    private ProjectDescription toDescription(NewProject newProject) throws ServerException {
        final ProjectType projectType = projectManager.getTypeDescriptionRegistry().getProjectType(newProject.getProjectTypeId());
        if (projectType == null) {
            throw new ServerException(String.format("Invalid project type '%s'. ", newProject.getProjectTypeId()));
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        final Map<String, List<String>> projectAttributeValues = newProject.getAttributes();
        if (!(projectAttributeValues == null || projectAttributeValues.isEmpty())) {
            final List<Attribute> projectAttributes = new ArrayList<>(projectAttributeValues.size());
            for (Map.Entry<String, List<String>> e : projectAttributeValues.entrySet()) {
                projectAttributes.add(new Attribute(e.getKey(), e.getValue()));
            }
            projectDescription.setAttributes(projectAttributes);
        }
        projectDescription.setDescription(newProject.getDescription());
        return projectDescription;
    }

    private ProjectDescription toDescription(ProjectUpdate update) throws ServerException {
        final ProjectType projectType = projectManager.getTypeDescriptionRegistry().getProjectType(update.getProjectTypeId());
        if (projectType == null) {
            throw new ServerException(String.format("Invalid project type '%s'. ", update.getProjectTypeId()));
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        final Map<String, List<String>> projectAttributeValues = update.getAttributes();
        if (!(projectAttributeValues == null || projectAttributeValues.isEmpty())) {
            final List<Attribute> projectAttributes = new ArrayList<>(projectAttributeValues.size());
            for (Map.Entry<String, List<String>> e : projectAttributeValues.entrySet()) {
                projectAttributes.add(new Attribute(e.getKey(), e.getValue()));
            }
            projectDescription.setAttributes(projectAttributes);
        }
        projectDescription.setDescription(update.getDescription());
        return projectDescription;
    }

    private ProjectDescriptor toDescriptor(Project project) throws ServerException {
        final ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class);
        fillDescriptor(project, descriptor);
        return descriptor;
    }

    private void fillDescriptor(Project project, ProjectDescriptor descriptor) throws ServerException {
        final String workspace = project.getWorkspace();
        final ProjectDescription description = project.getDescription();
        final ProjectType type = description.getProjectType();
        final Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        for (Attribute attribute : description.getAttributes()) {
            attributeValues.put(attribute.getName(), attribute.getValues());
        }
        final User currentUser = EnvironmentContext.getCurrent().getUser();
        final List<AccessControlEntry> acl = project.getPermissions();
        final List<String> userPermissions = new LinkedList<>();
        if (acl.isEmpty()) {
            // there is no any restriction at all
            userPermissions.add("all");
        } else {
            for (AccessControlEntry accessControlEntry : acl) {
                final Principal principal = accessControlEntry.getPrincipal();
                if ((Principal.Type.USER == principal.getType() && currentUser.getName().equals(principal.getName()))
                    || (Principal.Type.USER == principal.getType() && "any".equals(principal.getName()))
                    || (Principal.Type.GROUP == principal.getType() && currentUser.isMemberOf(principal.getName()))) {

                    userPermissions.addAll(accessControlEntry.getPermissions());
                }
            }
        }
        descriptor.withName(project.getName())
                  .withPath(project.getBaseFolder().getPath())
                  .withBaseUrl(
                          getServiceContext().getServiceUriBuilder().path(project.getBaseFolder().getPath()).build(workspace).toString())
                  .withProjectTypeId(type.getId())
                  .withProjectTypeName(type.getName())
                  .withWorkspaceId(workspace)
                  .withDescription(description.getDescription())
                  .withVisibility(project.getVisibility())
                  .withCurrentUserPermissions(userPermissions)
                  .withAttributes(attributeValues)
                  .withCreationDate(project.getCreationDate())
                  .withModificationDate(project.getModificationDate())
                  .withLinks(generateProjectLinks(workspace, project));
    }

    private ProjectReference toReference(Project project) throws ServerException {
        final String workspaceId = project.getWorkspace();
        final String workspaceName = EnvironmentContext.getCurrent().getWorkspaceName();
        final ProjectDescription description = project.getDescription();
        final ProjectType type = description.getProjectType();
        final String name = project.getName();
        final String path = project.getPath();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        return DtoFactory.getInstance().createDto(ProjectReference.class)
                         .withName(name)
                         .withPath(path)
                         .withId(project.getBaseFolder().getVirtualFile().getId())
                         .withWorkspaceId(workspaceId)
                         .withWorkspaceName(workspaceName)
                         .withProjectTypeId(type.getId())
                         .withProjectTypeName(type.getName())
                         .withVisibility(project.getVisibility())
                         .withCreationDate(project.getCreationDate())
                         .withModificationDate(project.getModificationDate())
                         .withDescription(description.getDescription())
                         .withUrl(uriBuilder.clone().path(getClass(), "getProject").build(workspaceId, name).toString())
                         .withIdeUrl(workspaceName != null
                                     ? uriBuilder.clone().replacePath("ws").path(workspaceName).path(path).build().toString()
                                     : null);
    }

    private List<Link> generateProjectLinks(String workspace, Project project) {
        final UriBuilder ub = getServiceContext().getServiceUriBuilder();
        final DtoFactory dto = DtoFactory.getInstance();
        final List<Link> links = new ArrayList<>(5);
        final String relPath = project.getPath().substring(1);
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_UPDATE_PROJECT)
                     .withMethod("PUT")
                     .withProduces(MediaType.APPLICATION_JSON)
                     .withConsumes(MediaType.APPLICATION_JSON)
                     .withHref(ub.clone().path(getClass(), "updateProject").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_EXPORT_ZIP)
                     .withMethod("GET")
                     .withProduces("application/zip")
                     .withHref(ub.clone().path(getClass(), "exportZip").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_CHILDREN)
                     .withMethod("GET")
                     .withProduces(MediaType.APPLICATION_JSON)
                     .withHref(ub.clone().path(getClass(), "getChildren").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_TREE)
                     .withMethod("GET")
                     .withProduces(MediaType.APPLICATION_JSON)
                     .withHref(ub.clone().path(getClass(), "getTree").build(workspace, relPath).toString())
                     .withParameters(Arrays.asList(dto.createDto(LinkParameter.class).withName("depth").withType(ParameterType.Number))));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_DELETE)
                     .withMethod("DELETE")
                     .withHref(ub.clone().path(getClass(), "delete").build(workspace, relPath).toString()));
        return links;
    }

    private List<Link> generateFolderLinks(String workspace, FolderEntry folder) {
        final UriBuilder ub = getServiceContext().getServiceUriBuilder();
        final DtoFactory dto = DtoFactory.getInstance();
        final List<Link> links = new ArrayList<>(4);
        final String relPath = folder.getPath().substring(1);
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_EXPORT_ZIP)
                     .withMethod("GET")
                     .withProduces("application/zip")
                     .withHref(ub.clone().path(getClass(), "exportZip").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_CHILDREN)
                     .withMethod("GET")
                     .withProduces(MediaType.APPLICATION_JSON)
                     .withHref(ub.clone().path(getClass(), "getChildren").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_TREE)
                     .withMethod("GET")
                     .withProduces(MediaType.APPLICATION_JSON)
                     .withHref(ub.clone().path(getClass(), "getTree").build(workspace, relPath).toString())
                     .withParameters(Arrays.asList(dto.createDto(LinkParameter.class).withName("depth").withType(ParameterType.Number))));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_DELETE)
                     .withMethod("DELETE")
                     .withHref(ub.clone().path(getClass(), "delete").build(workspace, relPath).toString()));
        return links;
    }

    private List<Link> generateFileLinks(String workspace, FileEntry file) {
        final UriBuilder ub = getServiceContext().getServiceUriBuilder();
        final DtoFactory dto = DtoFactory.getInstance();
        final List<Link> links = new ArrayList<>(3);
        final String relPath = file.getPath().substring(1);
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_GET_CONTENT)
                     .withMethod("GET")
                     .withProduces(MediaType.WILDCARD)
                     .withHref(ub.clone().path(getClass(), "getFile").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_UPDATE_CONTENT)
                     .withMethod("PUT")
                     .withConsumes(MediaType.WILDCARD)
                     .withHref(ub.clone().path(getClass(), "updateFile").build(workspace, relPath).toString()));
        links.add(dto.createDto(Link.class)
                     .withRel(Constants.LINK_REL_DELETE)
                     .withMethod("DELETE")
                     .withHref(ub.clone().path(getClass(), "delete").build(workspace, relPath).toString()));
        return links;
    }
}
