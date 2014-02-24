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
import com.codenvy.dto.server.DtoFactory;
import com.google.common.io.ByteStreams;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
@Path("project/{ws-id}")
public class ProjectService extends Service {
    @Inject
    private ProjectManager           projectManager;
    @Inject
    private SourceImporterRegistry   importers;
    @Inject
    private ProjectGeneratorRegistry generators;

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
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", workspace, path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return toDescriptor(workspace, project);
    }

    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@PathParam("ws-id") String workspace,
                                           @Required @Description("project name") @QueryParam("name") String name,
                                           @Description("descriptor of project") ProjectDescriptor descriptor) throws Exception {
        final Project project = projectManager.createProject(workspace, name, toDescription(descriptor));
        return toDescriptor(workspace, project);
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
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", workspace, parentProject));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        final Project module = project.createModule(name, toDescription(descriptor));
        return toDescriptor(workspace, module);
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
                    String.format("Project '%s' doesn't exist in workspace '%s'. ", workspace, path));
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        project.updateDescription(toDescription(descriptor));
        return toDescriptor(workspace, project);
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
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        entry.remove();
    }

    @POST
    @Path("copy/{path:.*}")
    public Response copy(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        entry.copyTo(newParent);
        return Response.created(URI.create("")).build(); // TODO: Location
    }

    @POST
    @Path("move/{path:.*}")
    public Response move(@PathParam("ws-id") String workspace,
                         @PathParam("path") String path,
                         @QueryParam("to") String newParent) throws Exception {
        final AbstractVirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        entry.moveTo(newParent);
        return Response.created(URI.create("")).build(); // TODO: Location
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
        return Response.created(URI.create("")).build(); // TODO: Location
    }

    @POST
    @Path("import/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor importProject(@PathParam("ws-id") String workspace,
                                           @PathParam("path") String path,
                                           ImportSourceDescriptor importDescriptor) throws Exception {
        final SourceImporter importer = importers.getImporter(importDescriptor.getType());
        if (importer == null) {
            final ServiceError error = DtoFactory.getInstance().createDto(ServiceError.class).withMessage(
                    String.format("Unable import sources project from '%s'. Sources type '%s' is not supported. ",
                                  importDescriptor.getLocation(), importDescriptor.getType()));
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            project = projectManager.createProject(workspace, path, new ProjectDescription());
        }
        importer.importSources(project.getBaseFolder(), importDescriptor.getLocation());
        return toDescriptor(workspace, project);
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
        return toDescriptor(workspace, project);
    }

    @POST
    @Path("import/{path:.*}")
    @Consumes("application/zip")
    public Response importZip(@PathParam("ws-id") String workspace,
                              @PathParam("path") String name,
                              InputStream zip) throws Exception {
        final FolderEntry folder = asFolder(workspace, name);
        folder.unzip(zip);
        return Response.created(URI.create("")).build(); // TODO: Location
    }

    @GET
    @Path("export/{path:.*}")
    @Produces("application/zip")
    public Response exportZip(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        return Response.ok()
                       .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                       .header("Content-Disposition", String.format("attachment; filename=\"%s\"", folder.getName()))
                       .entity(folder.toZip())
                       .build();
    }

    @GET
    @Path("children/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> getChildren(@PathParam("ws-id") String workspace, @PathParam("parent") String path) throws Exception {
        final FolderEntry folder = asFolder(workspace, path);
        final List<AbstractVirtualFileEntry> children = folder.getChildren();
        final ArrayList<ItemReference> result = new ArrayList<>(children.size());
        for (AbstractVirtualFileEntry child : children) {
            final ItemReference itemReference = DtoFactory.getInstance().createDto(ItemReference.class).withName(child.getName());
            if (child.isFile()) {
                itemReference.withLinks(generateFileLinks(workspace, (FileEntry)child));
            } else {
                itemReference.withLinks(generateFolderLinks(workspace, (FolderEntry)child));
            }
            result.add(itemReference);
            // TODO
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
                         .withNode(DtoFactory.getInstance().createDto(ItemReference.class).withName(folder.getName()))
                         .withChildren(getTree(folder, depth));
    }


    private List<TreeElement> getTree(FolderEntry folder, int depth) {
        if (depth == 0) {
            return null;
        }
        final List<FolderEntry> childFolders = folder.getChildFolders();
        final List<TreeElement> nodes = new ArrayList<>(childFolders.size());
        for (FolderEntry childFolder : childFolders) {
            nodes.add(DtoFactory.getInstance().createDto(TreeElement.class)
                                .withNode(DtoFactory.getInstance().createDto(ItemReference.class).withName(childFolder.getName()))
                                .withChildren(getTree(childFolder, depth - 1)));
        }
        return nodes;
    }


/*
GET	    /search/{path:.*}
*/

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

    private ProjectDescriptor toDescriptor(String workspace, Project project) throws IOException {
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
                         .withLinks(generateProjectLinks(workspace, project));
        // TODO: check project visibility
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
