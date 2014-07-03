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

import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.ProjectReference;
import com.codenvy.api.project.shared.dto.TreeElement;
import com.codenvy.api.vfs.server.ContentStream;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileFilter;
import com.codenvy.api.vfs.server.VirtualFileSystemImpl;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.search.QueryExpression;
import com.codenvy.api.vfs.server.search.SearcherProvider;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

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
import javax.ws.rs.WebApplicationException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Path("project/{ws-id}")
public class ProjectService extends Service {
    private static final Logger            LOG          = LoggerFactory.getLogger(ProjectService.class);
    private static final VirtualFileFilter FILES_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) throws VirtualFileSystemException {
            return file.isFile();
        }
    };

    @Inject
    private ProjectManager           projectManager;
    @Inject
    private ProjectImporterRegistry  importers;
    @Inject
    private ProjectGeneratorRegistry generators;
    @Inject
    private SearcherProvider         searcherProvider;

    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectReference> getProjects(@PathParam("ws-id") String workspace) throws Exception {
        final List<Project> projects = projectManager.getProjects(workspace);
        final List<ProjectReference> projectRefs = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final String wsName = EnvironmentContext.getCurrent().getWorkspaceName();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (Project project : projects) {
            final ProjectDescription description = project.getDescription();
            final ProjectType type = description.getProjectType();
            final String name = project.getName();
            final String id = project.getId();
            final String path = project.getBaseFolder().getPath();

            projectRefs.add(dtoFactory.createDto(ProjectReference.class)
                                      .withName(name)
                                      .withId(id)
                                      .withWorkspaceId(workspace)
                                      .withWorkspaceName(wsName)
                                      .withProjectTypeId(type.getId())
                                      .withProjectTypeName(type.getName())
                                      .withVisibility(project.getVisibility())
                                      .withCreationDate(project.getCreationDate())
                                      .withModificationDate(project.getModificationDate())
                                      .withDescription(description.getDescription())
                                      .withUrl(uriBuilder.clone().path(getClass(), "getProject")
                                                         .build(workspace, name).toString())
                                      .withIdeUrl(wsName != null
                                                  ? uriBuilder.clone().replacePath("ide").path(wsName).path(path)
                                                              .build().toString()
                                                  : null));
        }
        return projectRefs;
    }

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", path, workspace));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
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
        final ProjectDescriptor projectDescriptor = toDescriptor(project);

        VirtualFile projectVirtualFile = project.getBaseFolder().getVirtualFile();
        searcherProvider.getSearcher(projectVirtualFile.getMountPoint(), true).add(projectVirtualFile);

        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", projectDescriptor.getName(),
                 projectDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return projectDescriptor;
    }

    @GET
    @Path("modules/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectDescriptor> getModules(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", path, workspace));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }

        List<Project> modules = project.getModules();
        List<ProjectDescriptor> result = new ArrayList<>(modules.size());
        for (Project module : modules) {
            result.add(toDescriptor(module));
        }
        return result;
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
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", parentProject, workspace));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        final Project module = project.createModule(name, toDescription(descriptor));
        final ProjectDescriptor moduleDescriptor = toDescriptor(module);
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", moduleDescriptor.getName(),
                 moduleDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return moduleDescriptor;
    }

    @PUT
    @Path("{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor updateProject(@PathParam("ws-id") String workspace,
                                           @PathParam("path") String path,
                                           ProjectDescriptor descriptor) throws Exception {
        final Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", path, workspace));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        final String newVisibility = descriptor.getVisibility();
        project.updateDescription(toDescription(descriptor));
        if (!(newVisibility == null || newVisibility.equals(project.getVisibility()))) {
            project.setVisibility(newVisibility);
        }
        return toDescriptor(project);
    }

    @POST
    @Path("file/{parent:.*}")
    public Response createFile(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String parentPath,
                               @QueryParam("name") String fileName,
                               @HeaderParam("content-type") String contentType,
                               InputStream content) throws Exception {
        final FileEntry file = asFolder(workspace, parentPath).createFile(fileName, content, contentType);
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getFile")
                                                   .build(workspace, file.getPath().substring(1))).build();
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.TEXT_HTML})
    @Path("uploadfile/{parent:.*}")
    public Response uploadFile(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String parentPath,
                               Iterator<FileItem> formData) throws Exception {
        final FolderEntry folder = asFolder(workspace, parentPath);
        return VirtualFileSystemImpl.uploadFile(folder.getVirtualFile(), formData);
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
        file.updateContent(content, contentType);
        return Response.ok().build();
    }

    @POST
    @Path("folder/{path:.*}")
    public Response createFolder(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final FolderEntry folder = projectManager.getProjectsRoot(workspace).createFolder(path);
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, folder.getPath().substring(1))).build();
    }

    @DELETE
    @Path("{path:.*}")
    public void delete(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
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
    @Path("copy/{path:.*}")
    public Response copy(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        final AbstractVirtualFileEntry copy = entry.copyTo(newParent);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), copy.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, copy.getPath().substring(1));
        if (copy.isFolder() && ((FolderEntry)copy).isProjectFolder()) {
            Project project = new Project(workspace, (FolderEntry)copy, projectManager);
            final String name = project.getName();
            final String projectType = project.getDescription().getProjectType().getId();
            entry.remove();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(location).build();
    }

    @POST
    @Path("move/{path:.*}")
    public Response move(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
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
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(location).build();
    }

    @POST
    @Path("rename/{path:.*}")
    public Response rename(@PathParam("ws-id") String workspace,
                           @PathParam("path") String path,
                           @QueryParam("name") String newName,
                           @QueryParam("mediaType") String newMediaType) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
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
    @Path("import/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor importProject(@PathParam("ws-id") String workspace,
                                           @PathParam("path") String path,
                                           ImportSourceDescriptor importDescriptor) throws Exception {
        final ProjectImporter importer = importers.getImporter(importDescriptor.getType());
        if (importer == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Unable import sources project from '%s'. Sources type '%s' is not supported. ",
                                  importDescriptor.getLocation(), importDescriptor.getType())
                                                                                                         );
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }

        // create project descriptor based on query parameters
        ProjectDescriptor descriptorToUpdate = DtoFactory.getInstance().createDto(ProjectDescriptor.class);
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

        VirtualFile projectVirtualFile = project.getBaseFolder().getVirtualFile();
        searcherProvider.getSearcher(projectVirtualFile.getMountPoint(), true).add(projectVirtualFile);

        if (descriptorToUpdate.getProjectTypeId() != null) {
            project.updateDescription(toDescription(descriptorToUpdate));
        }

        final ProjectDescriptor projectDescriptor = toDescriptor(project);
        if (newProject) {
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", projectDescriptor.getName(),
                     projectDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                     EnvironmentContext.getCurrent().getUser().getName());
        }
        return projectDescriptor;
    }

    @POST
    @Path("generate/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor generateProject(@PathParam("ws-id") String workspace,
                                             @PathParam("path") String path,
                                             @QueryParam("generator") String generatorName,
                                             Map<String, String> options) throws Exception {
        final ProjectGenerator generator = generators.getGenerator(generatorName);
        if (generator == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Unable generate project. Unknown generator '%s'. ", generatorName));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            project = projectManager.createProject(workspace, path, new ProjectDescription());
        }
        generator.generateProject(project.getBaseFolder(), options);
        final ProjectDescriptor projectDescriptor = toDescriptor(project);
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", projectDescriptor.getName(),
                 projectDescriptor.getProjectTypeId(), EnvironmentContext.getCurrent().getWorkspaceName(),
                 EnvironmentContext.getCurrent().getUser().getName());
        return projectDescriptor;
    }

    @POST
    @Path("import/{path:.*}")
    @Consumes("application/zip")
    public Response importZip(@PathParam("ws-id") String workspace,
                              @PathParam("path") String name,
                              InputStream zip) throws Exception {
        final FolderEntry folder = asFolder(workspace, name);
        VirtualFileSystemImpl.importZip(folder.getVirtualFile(), zip, true);
        if (folder.isProjectFolder()) {
            Project project = new Project(workspace, folder, projectManager);
            final String projectType = project.getDescription().getProjectType().getId();
            LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                     EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());
        }
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, folder.getPath().substring(1))).build();
    }

    /** See {@link com.codenvy.api.vfs.server.VirtualFileSystem#exportZip(String)}. */
    @GET
    @Path("export/{path:.*}")
    @Produces("application/zip")
    public ContentStream exportZip(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile());
    }

    /** See {@link com.codenvy.api.vfs.server.VirtualFileSystem#exportZip(String, java.io.InputStream)}. */
    @POST
    @Path("export/{path:.*}")
    @Consumes("text/plain")
    @Produces("application/zip")
    public Response exportDiffZip(@PathParam("ws-id") String workspace, @PathParam("path") String path, InputStream in) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile(), in);
    }

    @GET
    @Path("children/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> getChildren(@PathParam("ws-id") String workspace, @PathParam("parent") String path) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        final List<AbstractVirtualFileEntry> children = folder.getChildren();
        final ArrayList<ItemReference> result = new ArrayList<>(children.size());
        for (AbstractVirtualFileEntry child : children) {
            final ItemReference itemReference = DtoFactory.getInstance().createDto(ItemReference.class)
                                                          .withId(child.getVirtualFile().getId())
                                                          .withName(child.getName())
                                                          .withPath(child.getPath());
            if (child.isFile()) {
                itemReference.withType("file")
                             .withMediaType(((FileEntry)child).getMediaType())
                             .withLinks(generateFileLinks(workspace, (FileEntry)child));
            } else {
                itemReference.withType("folder")
                             .withMediaType("text/directory")
                             .withHasChildFiles(!((FolderEntry)child).getChildren(FILES_FILTER).isEmpty())
                             .withLinks(generateFolderLinks(workspace, (FolderEntry)child));
            }
            result.add(itemReference);
        }
        return result;
    }

    @GET
    @Path("tree/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeElement getTree(@PathParam("ws-id") String workspace,
                               @PathParam("parent") String path,
                               @DefaultValue("1") @QueryParam("depth") int depth) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        return DtoFactory.getInstance().createDto(TreeElement.class)
                         .withNode(DtoFactory.getInstance().createDto(ItemReference.class)
                                             .withId(folder.getVirtualFile().getId())
                                             .withName(folder.getName())
                                             .withPath(folder.getPath())
                                             .withType("folder")
                                             .withMediaType("text/directory")
                                             .withHasChildFiles(!folder.getChildren(FILES_FILTER).isEmpty())
                                             .withLinks(generateFolderLinks(workspace, folder)))
                         .withChildren(getTree(workspace, folder, depth));
    }

    private List<TreeElement> getTree(String workspace, FolderEntry folder, int depth) throws VirtualFileSystemException {
        if (depth == 0) {
            return null;
        }
        final List<FolderEntry> childFolders = folder.getChildFolders();
        final List<TreeElement> nodes = new ArrayList<>(childFolders.size());
        for (FolderEntry childFolder : childFolders) {
            nodes.add(DtoFactory.getInstance().createDto(TreeElement.class)
                                .withNode(DtoFactory.getInstance().createDto(ItemReference.class)
                                                    .withId(childFolder.getVirtualFile().getId())
                                                    .withName(childFolder.getName())
                                                    .withPath(childFolder.getPath())
                                                    .withType("folder")
                                                    .withMediaType("text/directory")
                                                    .withHasChildFiles(!childFolder.getChildren(FILES_FILTER).isEmpty())
                                                    .withLinks(generateFolderLinks(workspace, childFolder)))
                                .withChildren(getTree(workspace, childFolder, depth - 1)));
        }
        return nodes;
    }

    @GET
    @Path("search/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> search(@PathParam("ws-id") String workspace,
                                      @PathParam("path") String path,
                                      @QueryParam("name") String name,
                                      @QueryParam("mediatype") String mediatype,
                                      @QueryParam("text") String text,
                                      @QueryParam("maxItems") @DefaultValue("-1") int maxItems,
                                      @QueryParam("skipCount") int skipCount) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        if (searcherProvider != null) {
            if (skipCount < 0) {
                final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                        String.format("Invalid 'skipCount' parameter: %d. ", skipCount));
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
            }
            final QueryExpression expr = new QueryExpression()
                    .setPath(path.startsWith("/") ? path : ('/' + path))
                    .setName(name)
                    .setMediaType(mediatype)
                    .setText(text);

            final String[] result = searcherProvider.getSearcher(folder.getVirtualFile().getMountPoint(), true).search(expr);
            if (skipCount > 0) {
                if (skipCount > result.length) {
                    final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                            String.format("'skipCount' parameter: %d is greater then total number of items in result: %d. ",
                                          skipCount, result.length)
                                                                                                                 );
                    throw new WebApplicationException(
                            Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
                }
            }
            final int length = maxItems > 0 ? Math.min(result.length, maxItems) : result.length;
            final List<ItemReference> items = new ArrayList<>(length);
            final FolderEntry root = projectManager.getProjectsRoot(workspace);
            for (int i = skipCount; i < length; i++) {
                final AbstractVirtualFileEntry child = root.getChild(result[i]);
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
    @Path("permissions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> getPermissions(@PathParam("ws-id") String wsId,
                                                   @PathParam("path") String path,
                                                   @QueryParam("userid") String userId) throws Exception {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new ServerException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, wsId));
        }
        final List<AccessControlEntry> acl = project.getPermissions();
        final Map<Principal, AccessControlEntry> aclMap = new HashMap<>(acl.size());
        for (AccessControlEntry ace : acl) {
            //replace "all" with "read" & "write" & "update_acl"
            final Set<String> permissions = new HashSet<>(ace.getPermissions());
            if (permissions.contains("all")) {
                permissions.remove("all");
                permissions.add("read");
                permissions.add("write");
                permissions.add("update_acl");
                ace.setPermissions(new ArrayList<>(permissions));
            }
            aclMap.put(ace.getPrincipal(), ace);
        }
        if (userId != null) {
            final Principal principal = DtoFactory.getInstance().createDto(Principal.class).withName(userId).withType(Principal.Type.USER);
            AccessControlEntry ace = aclMap.get(principal);
            if (ace == null) {
                ace = DtoFactory.getInstance().createDto(AccessControlEntry.class).withPrincipal(principal);
            }
            return Collections.singletonList(ace);
        } else {
            return project.getPermissions();
        }
    }

    @POST
    @Path("permissions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public void setPermissions(@PathParam("ws-id") String wsId,
                               @PathParam("path") String path,
                               List<AccessControlEntry> acl) throws ServerException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new ServerException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, wsId));
        }
        try {
            project.setPermissions(acl);
        } catch (IOException ioEx) {
            LOG.error(ioEx.getMessage(), ioEx);
            throw new ServerException("Error while saving permissions");
        }
    }

    private FileEntry asFile(String workspace, String path) {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFile()) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Item '%s' isn't a file. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return (FileEntry)entry;
    }

    private FolderEntry asFolder(String workspace, String path) {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFolder()) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Item '%s' isn't a folder. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return (FolderEntry)entry;
    }

    private AbstractVirtualFileEntry getVirtualFileEntry(String workspace, String path) {
        final FolderEntry root = projectManager.getProjectsRoot(workspace);
        final AbstractVirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Path '%s' doesn't exists. ", path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return entry;
    }

    private ProjectDescription toDescription(ProjectDescriptor descriptor) {
        final ProjectType projectType = projectManager.getProjectTypeRegistry().getProjectType(descriptor.getProjectTypeId());
        if (projectType == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Invalid project type '%s'. ", descriptor.getProjectTypeId()));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
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

    private ProjectDescriptor toDescriptor(Project project) throws IOException, VirtualFileSystemException {
        final String workspace = project.getWorkspace();
        final ProjectDescription description = project.getDescription();
        final ProjectType type = description.getProjectType();
        final Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        for (Attribute attribute : description.getAttributes()) {
            attributeValues.put(attribute.getName(), attribute.getValues());
        }
        return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                         .withName(project.getName())
                         .withPath(project.getBaseFolder().getPath())
                .withBaseUrl(getServiceContext().getServiceUriBuilder().path(project.getBaseFolder().getPath()).build(workspace)
                                                .toString())
                        // Temporary add virtualFile ID, since need to rework client side.
                .withId(project.getBaseFolder().getVirtualFile().getId())
                .withProjectTypeId(type.getId())
                .withProjectTypeName(type.getName())
                .withDescription(description.getDescription())
                .withVisibility(project.getVisibility())
                .withAttributes(attributeValues)
                .withCreationDate(project.getCreationDate())
                .withModificationDate(project.getModificationDate())
                .withLinks(generateProjectLinks(workspace, project));
    }

    private List<Link> generateProjectLinks(String workspace, Project project) {
        final UriBuilder ub = getServiceContext().getServiceUriBuilder();
        final DtoFactory dto = DtoFactory.getInstance();
        final List<Link> links = new ArrayList<>(5);
        final String relPath = project.getBaseFolder().getPath().substring(1);
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
