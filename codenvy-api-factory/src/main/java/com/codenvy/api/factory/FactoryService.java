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
package com.codenvy.api.factory;

import com.codenvy.api.core.rest.Service;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.api.factory.dto.AdvancedFactoryUrl;
import com.codenvy.api.factory.dto.Link;
import com.codenvy.commons.lang.NameGenerator;

import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.codenvy.commons.lang.Strings.nullToEmpty;
import static javax.ws.rs.core.Response.Status;

/** Service for factory rest api features */
@Path("/factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    @Inject
    private FactoryStore factoryStore;

    @Inject
    private FactoryUrlValidator validator;

    /**
     * Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information. Fields with images
     * should
     * be named 'image'. Acceptable image size 100x100 pixels. If vcs is not set in factory URL it will be set with "git" value.
     *
     * @param request
     *         - http request
     * @param uriInfo
     *         - url context
     * @return - stored data
     * @throws FactoryUrlException
     *         - with response code 400 if factory url json is not found
     *         - with response code 400 if vcs is unsupported
     *         - with response code 400 if image content can't be read
     *         - with response code 400 if image media type is unsupported
     *         - with response code 400 if image height or length isn't equal to 100 pixels
     *         - with response code 413 if image is too big
     *         - with response code 500 if internal server error occurs
     */
    @RolesAllowed("user")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public AdvancedFactoryUrl saveFactory(@Context HttpServletRequest request, @Context UriInfo uriInfo) throws FactoryUrlException {
        try {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            if (context.getUser() == null || context.getUser().getName() == null || context.getUser().getId() == null) {
                throw new FactoryUrlException(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to identify user from context");
            }

            Set<FactoryImage> images = new HashSet<>();
            AdvancedFactoryUrl factoryUrl = null;

            for (Part part : request.getParts()) {
                String fieldName = part.getName();
                if (fieldName.equals("factoryUrl")) {
                    JsonParser jsonParser = new JsonParser();
                    jsonParser.parse(part.getInputStream());
                    JsonValue jsonValue = jsonParser.getJsonObject();
                    factoryUrl = ObjectBuilder.createObject(AdvancedFactoryUrl.class, jsonValue);
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
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "No factory URL information found in 'factoryUrl' section of multipart/form-data.");
            }

            // check that vcs value is correct (only git is supported for now)
            if (factoryUrl.getVcs() == null) {
                factoryUrl.setVcs("git");
            }

            validator.validateUrl(factoryUrl);

            factoryUrl.setUserid(context.getUser().getId());

            factoryUrl.setCreated(Long.toString(System.currentTimeMillis()));
            String factoryId = factoryStore.saveFactory(factoryUrl, new HashSet<>(images));
            factoryUrl = factoryStore.getFactory(factoryId);
            factoryUrl = new AdvancedFactoryUrlImpl(factoryUrl, LinksHelper.createLinks(factoryUrl, images, uriInfo));

            String createProjectLink = "";
            Iterator<Link> createProjectLinksIterator = LinksHelper.getLinkByRelation(factoryUrl.getLinks(), "create-project").iterator();
            if (createProjectLinksIterator.hasNext()) {
                createProjectLink = createProjectLinksIterator.next().getHref();
            }
            LOG.info(
                    "EVENT#factory-created# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# REPO-URL#{}# FACTORY-URL#{}# AFFILIATE-ID#{}# ORG-ID#{}#",
                    "",
                    context.getUser().getName(),
                    "",
                    nullToEmpty(factoryUrl.getProjectattributes().get("ptype")),
                    factoryUrl.getVcsurl(),
                    createProjectLink,
                    nullToEmpty(factoryUrl.getAffiliateid()),
                    nullToEmpty(factoryUrl.getOrgid()));

            return factoryUrl;
        } catch (IOException | JsonException | ServletException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new FactoryUrlException(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get factory information from storage by its id.
     *
     * @param id
     *         - id of factory
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws FactoryUrlException
     *         - with response code 404 if factory with given id doesn't exist
     */
    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public AdvancedFactoryUrl getFactory(@PathParam("id") String id, @Context UriInfo uriInfo) throws FactoryUrlException {
        AdvancedFactoryUrl factoryUrl = factoryStore.getFactory(id);
        if (factoryUrl == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
        }

        try {
            factoryUrl = new AdvancedFactoryUrlImpl(factoryUrl, LinksHelper.createLinks(factoryUrl, factoryStore.getFactoryImages(id, null),
                                                                                    uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        }

        return factoryUrl;
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
     * @throws FactoryUrlException
     *         - with response code 404 if factory with given id doesn't exist
     *         - with response code 404 if imgId is not set in request and there is no default image for factory with given id
     *         - with response code 404 if image with given image id doesn't exist
     */
    @GET
    @Path("{factoryId}/image")
    @Produces("image/*")
    public Response getImage(@PathParam("factoryId") String factoryId, @DefaultValue("") @QueryParam("imgId") String imageId)
            throws FactoryUrlException {
        Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(factoryId, null);
        if (factoryImages == null) {
            LOG.warn("Factory URL with id {} is not found.", factoryId);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(),
                                          String.format("Factory URL with id %s is not found.", factoryId));
        }
        if (imageId.isEmpty()) {
            if (factoryImages.size() > 0) {
                FactoryImage image = factoryImages.iterator().next();
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            } else {
                LOG.warn("Default image for factory {} is not found.", factoryId);
                throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(),
                                              String.format("Default image for factory %s is not found.", factoryId));
            }
        } else {
            for (FactoryImage image : factoryImages) {
                if (image.getName().equals(imageId)) {
                    return Response.ok(image.getImageData(), image.getMediaType()).build();
                }
            }
        }
        LOG.warn("Image with id {} is not found.", imageId);
        throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Image with id %s is not found.", imageId));
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
     * @throws FactoryUrlException
     *         - with response code 404 if factory with given id doesn't exist
     *         - with response code 400 if snippet type is unsupported
     */
    @GET
    @Path("{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@PathParam("id") String id, @DefaultValue("url") @QueryParam("type") String type,
                                    @Context UriInfo uriInfo)
            throws FactoryUrlException {
        AdvancedFactoryUrl factory = factoryStore.getFactory(id);
        if (factory == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
        }


        switch (type) {
            case "url":
                return SnippetGenerator.generateUrlSnippet(id, uriInfo.getBaseUri());
            case "html":
                return SnippetGenerator.generateHtmlSnippet(id, uriInfo.getBaseUri());
            case "markdown":
                return SnippetGenerator
                        .generateMarkdownSnippet(id, factoryStore.getFactoryImages(id, null), factory.getStyle(),
                                                 uriInfo.getBaseUri());
            default:
                LOG.warn("Snippet type {} is unsupported", type);
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              String.format("Snippet type \"%s\" is unsupported.", type));
        }
    }
}
