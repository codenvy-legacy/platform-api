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

import com.codenvy.api.factory.store.FactoryStore;
import com.codenvy.api.factory.store.SavedFactoryData;
import com.codenvy.commons.lang.NameGenerator;

import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static javax.ws.rs.core.Response.Status;

/** Service for factory rest api features */
@Path("/factory")
public class FactoryService {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);
    @Inject
    private FactoryStore factoryStore;

    /**
     * Save factory to storage and return stored data.
     * If vcs is not set in factory URL it will be set with "git" value.
     *
     * @param request
     *         - http request
     * @param uriInfo
     * @return - stored data
     * @throws FactoryUrlException
     */
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public AdvancedFactoryUrl saveFactory(@Context HttpServletRequest request, @Context UriInfo uriInfo) throws FactoryUrlException {
        try {
            Set<Image> images = new HashSet<>();
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
                        BufferedImage bufferedImage = ImageIO.read(inputStream);
                        if (bufferedImage == null) {
                            LOG.error("Can't read image content.");
                            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), "Can't read image content.");
                        }
                        if (bufferedImage.getWidth() != 100 || bufferedImage.getHeight() != 100) {
                            LOG.error("Wrong size of image.");
                            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), "Wrong size of image.");
                        }
                        images.add(new Image(((DataBufferByte)bufferedImage.getRaster().getDataBuffer()).getData(), part.getContentType(),
                                             NameGenerator.generate(null, 16)));
                    }
                }
            }

            if (factoryUrl == null) {
                LOG.error("No factory URL information found in 'factoryUrl' section of multipart form-data.");
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "No factory URL information found in 'factoryUrl' section of multipart form-data.");
            }

            // check that vcs value is correct (only git is supported for now)
            if (factoryUrl.getVcs() == null) {
                factoryUrl.setVcs("git");
            } else if (!"git".equals(factoryUrl.getVcs())) {
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "Parameter vcs has illegal value. Only \"git\" is supported for now.");
            }

            SavedFactoryData savedFactoryData = factoryStore.saveFactory(factoryUrl, new HashSet<>(images));
            factoryUrl = new AdvancedFactoryUrl(savedFactoryData.getFactoryUrl(),
                                                LinksHelper.createLinks(factoryUrl, savedFactoryData.getImages(), uriInfo));

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
     * @return - stored data, if id is correct.
     * @throws FactoryUrlException
     */
    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public AdvancedFactoryUrl getFactory(@PathParam("id") String id, @Context UriInfo uriInfo) throws FactoryUrlException {
        SavedFactoryData savedFactoryData = factoryStore.getFactory(id);
        if (savedFactoryData == null) {
            LOG.error("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
        }

        AdvancedFactoryUrl factoryUrl = new AdvancedFactoryUrl(savedFactoryData.getFactoryUrl(), LinksHelper
                .createLinks(savedFactoryData.getFactoryUrl(), savedFactoryData.getImages(), uriInfo));

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
     *         no images, exception will be thrown.
     * @throws FactoryUrlException
     */
    @GET
    @Path("{factoryId}/image")
    @Produces("image/*")
    public Response getImage(@PathParam("factoryId") String factoryId, @DefaultValue("") @QueryParam("imgId") String imageId)
            throws FactoryUrlException {
        SavedFactoryData savedFactoryData = factoryStore.getFactory(factoryId);
        if (savedFactoryData == null) {
            LOG.error("Factory URL with id {} is not found.", factoryId);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(),
                                          String.format("Factory URL with id %s is not found.", factoryId));
        }
        if (imageId.isEmpty()) {
            Iterator<Image> it = savedFactoryData.getImages().iterator();
            if (it.hasNext()) {
                Image image = it.next();
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            } else {
                LOG.error("Default image for factory {} is not found.", factoryId);
                throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(),
                                              String.format("Default image for factory %s is not found.", factoryId));
            }
        } else {
            for (Image image : savedFactoryData.getImages()) {
                if (image.getName().equals(imageId)) {
                    return Response.ok(image.getImageData(), image.getMediaType()).build();
                }
            }
        }
        LOG.error("Image with id {} is not found.", imageId);
        throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Image with id %s is not found.", imageId));
    }

    /**
     * Get factory snippet by factory id and snippet type.
     *
     * @param id
     *         - factory id.
     * @param type
     *         - type of snippet.
     * @param uriInfo
     * @return - snippet content. If snippet type is not set, "url" type will be used as default.
     * @throws FactoryUrlException
     */
    @GET
    @Path("{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@PathParam("id") String id, @DefaultValue("url") @QueryParam("type") String type,
                                    @Context UriInfo uriInfo)
            throws FactoryUrlException {
        if (factoryStore.getFactory(id) == null) {
            LOG.error("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
        }

        switch (type) {
            case "url":
                return generateFactoryUrl(id, uriInfo);
            case "html":
                return "<script type=\"text/javascript\" language=\"javascript\" src=\"" +
                       UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("/factory/factory.js").build().toString() + "\" target=\"" +
                       generateFactoryUrl(id, uriInfo) + "\"></script>";
            case "markdown":
                return "[![alt](" + UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("/images/factory/factory.png").build().toString() +
                       ")](" + generateFactoryUrl(id, uriInfo) + ")";
            default:
                LOG.error("Snippet type {} is unsupported", type);
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              String.format("Snippet type \"%s\" is unsupported.", type));
        }
    }

    @GET
    @Path("ping")
    public Response ping() {
        return Response.ok().build();
    }


    private static String generateFactoryUrl(String id, UriInfo uriInfo) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory").queryParam("id", id).build().toString();
    }
}
