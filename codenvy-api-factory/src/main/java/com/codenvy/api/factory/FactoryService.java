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
package com.codenvy.api.factory;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.factory.dto.Author;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.FactoryV2_0;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.api.project.server.Builders;
import com.codenvy.api.project.server.Project;
import com.codenvy.api.project.server.ProjectDescription;
import com.codenvy.api.project.server.ProjectJson2;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.api.project.server.Runners;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.Pair;
import com.codenvy.commons.lang.URLEncodedUtils;
import com.codenvy.dto.server.DtoFactory;
import com.google.gson.JsonSyntaxException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.commons.lang.Strings.nullToEmpty;

/** Service for factory rest api features */
@Api(value = "/factory",
     description = "Factory manager")
@Path("/factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    private String                    baseApiUrl;
    private FactoryStore              factoryStore;
    private FactoryUrlCreateValidator createValidator;
    private FactoryUrlAcceptValidator acceptValidator;
    private LinksHelper               linksHelper;
    private FactoryBuilder            factoryBuilder;
    private ProjectManager            projectManager;

    @Inject
    public FactoryService(@Named("api.endpoint") String baseApiUrl,
                          FactoryStore factoryStore,
                          FactoryUrlCreateValidator createValidator,
                          FactoryUrlAcceptValidator acceptValidator,
                          LinksHelper linksHelper,
                          FactoryBuilder factoryBuilder,
                          ProjectManager projectManager) {
        this.baseApiUrl = baseApiUrl;
        this.factoryStore = factoryStore;
        this.createValidator = createValidator;
        this.acceptValidator = acceptValidator;
        this.linksHelper = linksHelper;
        this.factoryBuilder = factoryBuilder;
        this.projectManager = projectManager;
    }

    /**
     * Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information.
     * Fields with images should be named 'image'. Acceptable image size 100x100 pixels.
     * If vcs is not set in factory URL it will be set with "git" value.
     *
     * @param request
     *         - http request
     * @param uriInfo
     *         - url context
     * @return - stored data
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.ConflictException} when factory url json is not found
     *         - {@link com.codenvy.api.core.ConflictException} when vcs is unsupported
     *         - {@link com.codenvy.api.core.ConflictException} when image content can't be read
     *         - {@link com.codenvy.api.core.ConflictException} when image media type is unsupported
     *         - {@link com.codenvy.api.core.ConflictException} when image height or length isn't equal to 100 pixels
     *         - {@link com.codenvy.api.core.ConflictException} when if image is too big
     *         - {@link com.codenvy.api.core.ServerException} when internal server error occurs
     */
    @ApiOperation(value = "Create a Factory and return data",
                  notes = "Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information.",
                  response = Factory.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 409, message = "Conflict error. Some parameter is missing"),
            @ApiResponse(code = 500, message = "Unable to identify user from context")})
    @RolesAllowed("user")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public Factory saveFactory(@ApiParam(value = "Factory parameters", required = true)
                               @Context HttpServletRequest request,
                               @Context UriInfo uriInfo)
            throws ApiException {
        try {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            if (context.getUser() == null || context.getUser().getName() == null || context.getUser().getId() == null) {
                throw new ServerException("Unable to identify user from context");
            }

            Set<FactoryImage> images = new HashSet<>();
            Factory factoryUrl = null;

            for (Part part : request.getParts()) {
                String fieldName = part.getName();
                if (fieldName.equals("factoryUrl")) {
                    try {
                        factoryUrl = factoryBuilder.buildEncoded(part.getInputStream());
                    } catch (JsonSyntaxException e) {
                        throw new ConflictException(
                                "You have provided an invalid JSON.  For more information, " +
                                "please visit http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/");
                    }
                } else if (fieldName.equals("image")) {
                    try (InputStream inputStream = part.getInputStream()) {
                        FactoryImage factoryImage =
                                FactoryImage.createImage(inputStream, part.getContentType(), NameGenerator.generate(null, 16));
                        if (factoryImage.hasContent()) {
                            images.add(factoryImage);
                        }
                    }
                }
            }

            if (factoryUrl == null) {
                LOG.warn("No factory URL information found in 'factoryUrl' section of multipart form-data.");
                throw new ConflictException("No factory URL information found in 'factoryUrl' section of multipart/form-data.");
            }

            if (factoryUrl.getV().equals("1.0")) {
                throw new ConflictException("Storing of Factory 1.0 is unsupported.");
            }

            String orgid;
            String repoUrl;
            String ptype;
            if (factoryUrl.getV().startsWith("1.")) {
                factoryUrl.setUserid(context.getUser().getId());
                factoryUrl.setCreated(System.currentTimeMillis());

                // for logging purposes
                orgid = factoryUrl.getOrgid();
                repoUrl = factoryUrl.getVcsurl();
                ptype = factoryUrl.getProjectattributes() != null ? factoryUrl.getProjectattributes().getPtype() : null;
            } else {
                if (null == factoryUrl.getCreator()) {
                    factoryUrl.setCreator(DtoFactory.getInstance().createDto(Author.class));
                }
                factoryUrl.getCreator().withUserId(context.getUser().getId()).withCreated(System.currentTimeMillis());

                // for logging purposes
                orgid = factoryUrl.getCreator().getAccountId();
                repoUrl = factoryUrl.getSource().getProject().getLocation();
                ptype = factoryUrl.getProject() != null ? factoryUrl.getProject().getType() : null;
            }

            createValidator.validateOnCreate(factoryUrl);
            String factoryId = factoryStore.saveFactory(factoryUrl, images);
            factoryUrl = factoryStore.getFactory(factoryId);
            factoryUrl = factoryUrl.withLinks(linksHelper.createLinks(factoryUrl, images, uriInfo));

            String createProjectLink = "";
            Iterator<Link> createProjectLinksIterator = linksHelper.getLinkByRelation(factoryUrl.getLinks(), "create-project").iterator();
            if (createProjectLinksIterator.hasNext()) {
                createProjectLink = createProjectLinksIterator.next().getHref();
            }

            LOG.info(
                    "EVENT#factory-created# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# REPO-URL#{}# FACTORY-URL#{}# AFFILIATE-ID#{}# ORG-ID#{}#",
                    "",
                    context.getUser().getName(),
                    "",
                    nullToEmpty(ptype),
                    repoUrl,
                    createProjectLink,
                    nullToEmpty(factoryUrl.getAffiliateid()),
                    nullToEmpty(orgid));

            return factoryUrl;
        } catch (IOException | ServletException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get  factory json from non encoded version of factory.
     *
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.NotFoundException} when factory with given id doesn't exist
     */
    @GET
    @Path("/nonencoded")
    @Produces({MediaType.APPLICATION_JSON})
    public Factory getFactoryFromNonEncoded(@DefaultValue("false") @QueryParam("legacy") Boolean legacy,
                                            @DefaultValue("false") @QueryParam("validate") Boolean validate,
                                            @QueryParam("maxVersion") String maxVersion,
                                            @Context UriInfo uriInfo) throws ApiException {
        final URI uri = UriBuilder.fromUri(uriInfo.getRequestUri())
                                  .replaceQueryParam("legacy", null)
                                  .replaceQueryParam("token", null)
                                  .replaceQueryParam("validate", null)
                                  .replaceQueryParam("maxVersion", null)
                                  .build();
        Factory factory = factoryBuilder.buildEncoded(uri);
        if (legacy) {
            if (maxVersion != null) {
                if ("1.2".equals(maxVersion)) {
                    factory = factoryBuilder.convertToV1_2(factory);
                } else {
                    factory = factoryBuilder.convertToLatest(factory);
                }
            } else {
                factory = factoryBuilder.convertToLatest(factory);
            }
        }
        if (validate) {
            acceptValidator.validateOnAccept(factory, false);
        }
        return factory;
    }

    /**
     * Get factory information from storage by its id.
     *
     * @param id
     *         - id of factory
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.NotFoundException} when factory with given id doesn't exist
     */

    @ApiOperation(value = "Get Factory information by its ID",
                  notes = "Get JSON with Factory information. Factory ID is passed in a path parameter",
                  response = Factory.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory not found"),
            @ApiResponse(code = 409, message = "Failed to validate Factory URL"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Factory getFactory(@ApiParam(value = "Factory ID", required = true)
                              @PathParam("id") String id,
                              @ApiParam(value = "Legacy. Whether or not to transform Factory into the most recent format",
                                        allowableValues = "true,false", defaultValue = "false")
                              @DefaultValue("false") @QueryParam("legacy") Boolean legacy,
                              @ApiParam(value = "Whether or not to validate values like it is done when accepting a Factory",
                                        allowableValues = "true,false", defaultValue = "false")
                              @DefaultValue("false") @QueryParam("validate") Boolean validate,
                              @QueryParam("maxVersion") String maxVersion,
                              @Context UriInfo uriInfo) throws ApiException {
        Factory factoryUrl = factoryStore.getFactory(id);
        if (factoryUrl == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new NotFoundException("Factory URL with id " + id + " is not found.");
        }

        if (legacy) {
            if (maxVersion != null) {
                if ("1.2".equals(maxVersion)) {
                    factoryUrl = factoryBuilder.convertToV1_2(factoryUrl);
                } else {
                    factoryUrl = factoryBuilder.convertToLatest(factoryUrl);
                }
            } else {
                factoryUrl = factoryBuilder.convertToLatest(factoryUrl);
            }
        }

        try {
            factoryUrl = factoryUrl.withLinks(linksHelper.createLinks(factoryUrl, factoryStore.getFactoryImages(id, null), uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new ConflictException(e.getLocalizedMessage());
        }
        if (validate) {
            acceptValidator.validateOnAccept(factoryUrl, true);
        }
        return factoryUrl;
    }

    /**
     * Get list of factory links which conform specified attributes.
     *
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.NotFoundException} when factory with given id doesn't exist
     */
    @RolesAllowed("user")
    @GET
    @Path("/find")
    @Produces({MediaType.APPLICATION_JSON})
    @SuppressWarnings("unchecked")
    public List<Link> getFactoryByAttribute(@Context UriInfo uriInfo) throws ApiException {
        List<Link> result = new ArrayList<>();
        URI uri = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQueryParam("token", null).build();
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");
        if (queryParams.isEmpty()) {
            throw new IllegalArgumentException("Query must contain at least one attribute.");
        }
        if (queryParams.containsKey("accountid")) {
            queryParams.put("orgid", queryParams.remove("accountid"));
        }
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : queryParams.entrySet()) {
            if (!entry.getValue().isEmpty())
                pairs.add(Pair.of(entry.getKey(), entry.getValue().iterator().next()));
        }
        List<Factory> factories = factoryStore.findByAttribute(pairs.toArray(new Pair[pairs.size()]));
        for (Factory factory : factories) {
            result.add(DtoFactory.getInstance().createDto(Link.class)
                                 .withMethod("GET")
                                 .withRel("self")
                                 .withProduces(MediaType.APPLICATION_JSON)
                                 .withConsumes(null)
                                 .withHref(UriBuilder.fromUri(uriInfo.getBaseUri())
                                                     .path(FactoryService.class)
                                                     .path(FactoryService.class, "getFactory").build(factory.getId())
                                                     .toString())
                                 .withParameters(null));
        }
        return result;
    }

    /**
     * Get image information by its id.
     *
     * @param factoryId
     *         - id of factory
     * @param imageId
     *         - image id.
     * @return - image information if ids are correct. If imageId is not set, random image of factory will be returned. But if factory has
     * no images, exception will be thrown.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.NotFoundException} when factory with given id doesn't exist
     *         - {@link com.codenvy.api.core.NotFoundException} when imgId is not set in request and there is no default image for factory
     *         with given id
     *         - {@link com.codenvy.api.core.NotFoundException} when image with given image id doesn't exist
     */
    @ApiOperation(value = "Get Factory image information",
                  notes = "Get Factory image information by Factory and image ID",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory or Image ID Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{factoryId}/image")
    @Produces("image/*")
    public Response getImage(@ApiParam(value = "Factory ID", required = true)
                             @PathParam("factoryId") String factoryId,
                             @ApiParam(value = "Image ID", required = true)
                             @DefaultValue("") @QueryParam("imgId") String imageId)
            throws ApiException {
        Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(factoryId, null);
        if (factoryImages == null) {
            LOG.warn("Factory URL with id {} is not found.", factoryId);
            throw new NotFoundException("Factory URL with id " + factoryId + " is not found.");
        }
        if (imageId.isEmpty()) {
            if (factoryImages.size() > 0) {
                FactoryImage image = factoryImages.iterator().next();
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            } else {
                LOG.warn("Default image for factory {} is not found.", factoryId);
                throw new NotFoundException("Default image for factory " + factoryId + " is not found.");
            }
        } else {
            for (FactoryImage image : factoryImages) {
                if (image.getName().equals(imageId)) {
                    return Response.ok(image.getImageData(), image.getMediaType()).build();
                }
            }
        }
        LOG.warn("Image with id {} is not found.", imageId);
        throw new NotFoundException("Image with id " + imageId + " is not found.");
    }

    /**
     * Get factory snippet by factory id and snippet type. If snippet type is not set, "url" type will be used as default.
     *
     * @param id
     *         - factory id.
     * @param type
     *         - type of snippet.
     * @param uriInfo
     *         - url context
     * @return - snippet content.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.NotFoundException} when factory with given id doesn't exist - with response code 400 if
     *         snippet
     *         type
     *         is unsupported
     */
    @ApiOperation(value = "Get Factory snippet by ID",
                  notes = "Get Factory snippet by ID",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory not Found"),
            @ApiResponse(code = 409, message = "Unknown snippet type"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@ApiParam(value = "Factory ID", required = true)
                                    @PathParam("id") String id,
                                    @ApiParam(value = "Snippet type", required = true, allowableValues = "url,html,iframe,markdown",
                                              defaultValue = "url")
                                    @DefaultValue("url") @QueryParam("type") String type,
                                    @Context UriInfo uriInfo)
            throws ApiException {
        Factory factory = factoryStore.getFactory(id);
        if (factory == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new NotFoundException("Factory URL with id " + id + " is not found.");
        }

        final String baseUrl = UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("").build().toString();

        switch (type) {
            case "url":
                return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory").queryParam("id", id).build().toString();
            case "html":
                return SnippetGenerator.generateHtmlSnippet(baseUrl, id);
            case "iframe":
                return SnippetGenerator.generateiFrameSnippet(baseUrl, id);
            case "markdown":
                Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(id, null);
                String imageId = (factoryImages.size() > 0) ? factoryImages.iterator().next().getName() : null;

                try {
                    return SnippetGenerator.generateMarkdownSnippet(baseUrl, factory, imageId);
                } catch (IllegalArgumentException e) {
                    throw new ConflictException(e.getMessage());
                }
            default:
                LOG.warn("Snippet type {} is unsupported", type);
                throw new ConflictException("Snippet type \"" + type + "\" is unsupported.");
        }
    }

    /**
     * Generate project configuration.
     *
     * @param workspace
     *         - workspace id.
     * @param path
     *         - project path.
     * @throws com.codenvy.api.core.ApiException
     *         - {@link com.codenvy.api.core.ConflictException} when project is not under source control.
     */
    @GET
    @Path("/{ws-id}/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactoryJson(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws ApiException {
        final Project project = projectManager.getProject(workspace, path);
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        ImportSourceDescriptor source;
        NewProject newProject;
        try {
            final ProjectDescription projectDescription = project.getDescription();
            if ("git".equals(projectDescription.getAttributeValue("vcs.provider.name"))) {
                final Link importSourceLink = dtoFactory.createDto(Link.class)
                                                        .withMethod("GET")
                                                        .withHref(UriBuilder.fromUri(baseApiUrl)
                                                                            .path("git")
                                                                            .path(workspace)
                                                                            .path("import-source-descriptor")
                                                                            .build().toString());
                source = HttpJsonHelper.request(ImportSourceDescriptor.class, importSourceLink, new Pair<>("projectPath", path));
            } else {
                throw new ConflictException("Not able to generate project configuration, project has to be under version control system");
            }
            // Read again project.json file even we already have all information about project in 'projectDescription' variable.
            // We do so because 'projectDescription' variable contains all attributes of project including 'calculated' attributes but we
            // don't need 'calculated' attributes in this case. Such attributes exists only in runtime and may be restored from the project.
            // TODO: improve this once we will be able to detect different type of attributes. In this case just need get attributes from
            // 'projectDescription' variable and skip all attributes that aren't defined in project.json file.
            final ProjectJson2 projectJson = ProjectJson2.load(project);
            newProject = dtoFactory.createDto(NewProject.class)
                                   .withName(project.getName())
                                   .withType(projectJson.getType())
                                   .withAttributes(projectJson.getAttributes())
                                   .withVisibility(project.getVisibility())
                                   .withDescription(projectJson.getDescription());

            final Builders builders = projectJson.getBuilders();
            if (builders != null) {
                newProject.withBuilders(com.codenvy.api.project.server.DtoConverter.toDto(builders));
            }
            final Runners runners = projectJson.getRunners();
            if (runners != null) {
                newProject.withRunners(com.codenvy.api.project.server.DtoConverter.toDto(runners));
            }
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage());
        }
        return Response.ok(dtoFactory.createDto(FactoryV2_0.class)
                                     .withProject(newProject)
                                     .withSource(dtoFactory.createDto(Source.class).withProject(source))
                                     .withV("2.0"), MediaType.APPLICATION_JSON)
                       .header("Content-Disposition", "attachment; filename=" + path + ".json")
                       .build();
    }
}
