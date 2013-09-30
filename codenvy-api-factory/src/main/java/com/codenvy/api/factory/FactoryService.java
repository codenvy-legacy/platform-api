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

import org.apache.commons.fileupload.FileItem;
import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.impl.provider.json.JsonParser;
import org.everrest.core.impl.provider.json.JsonValue;
import org.everrest.core.impl.provider.json.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.Iterator;

import static javax.ws.rs.core.Response.Status;

/** Service for factory rest api features */
@Path("/factory")
public class FactoryService {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);
    @Inject
    private FactoryStore factoryStore;

    /**
     * Save factory to storage and return stored data.
     *
     * @param factoryData
     *         - data to store
     * @param uriInfo
     * @return - stored data
     * @throws FactoryUrlException
     */
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public AdvancedFactoryUrl saveFactory(Iterator<FileItem> factoryData, @Context UriInfo uriInfo) throws FactoryUrlException {
        try {
            Image image = null;
            AdvancedFactoryUrl factoryUrl = null;

            while (factoryData.hasNext()) {
                FileItem fileItem = factoryData.next();
                String fieldName = fileItem.getFieldName();
                if (fieldName.equals("factoryUrl")) {
                    JsonParser jsonParser = new JsonParser();
                    jsonParser.parse(fileItem.getInputStream());
                    JsonValue jsonValue = jsonParser.getJsonObject();
                    factoryUrl = ObjectBuilder.createObject(AdvancedFactoryUrl.class, jsonValue);
                } else if (fieldName.equals("image")) {
                    image = new Image(fileItem.get(), fileItem.getContentType(), fileItem.getName());
                }
            }

            if (factoryUrl == null) {
                LOG.error("No factory URL information found in 'factoryUrl' section of multipart form-data");
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "No factory URL information found in 'factoryUrl' section of multipart form-data");
            }

            // check that vcs value is correct (only git is supported for now)
            if (!"git".equals(factoryUrl.getVcs())) {
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "Parameter vcs has illegal value. Only \"git\" is supported for now.");
            }

            SavedFactoryData savedFactoryData = factoryStore.saveFactory(factoryUrl, image);
            factoryUrl = new AdvancedFactoryUrl(savedFactoryData.getFactoryUrl(),
                                                LinksHelper.createLinks(factoryUrl, savedFactoryData.getImages(), uriInfo));

            return factoryUrl;
        } catch (IOException | JsonException e) {
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
            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
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
     * @return - image information if ids are correct.
     * @throws FactoryUrlException
     */
    @GET
    @Path("{factoryId}/{imageId}")
    @Produces("image/*")
    public Response getImage(@PathParam("factoryId") String factoryId, @PathParam("imageId") String imageId) throws FactoryUrlException {
        SavedFactoryData savedFactoryData = factoryStore.getFactory(factoryId);
        if (savedFactoryData == null) {
            LOG.error("Factory URL with id {} is not found.", factoryId);
            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                          String.format("Factory URL with id %s is not found.", factoryId));
        }
        for (Image image : savedFactoryData.getImages()) {
            if (image.getName().equals(imageId)) {
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            }
        }
        LOG.error("Image with id {} is not found.", imageId);
        throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), String.format("Image with id %s is not found.", imageId));
    }

    /**
     * Get factory snippet by factory id and snippet type.
     *
     * @param id
     *         - factory id.
     * @param type
     *         - type of snippet.
     * @param uriInfo
     * @return - snippet content
     * @throws FactoryUrlException
     */
    @GET
    @Path("{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@PathParam("id") String id, @QueryParam("type") String type, @Context UriInfo uriInfo)
            throws FactoryUrlException {
        if (factoryStore.getFactory(id) == null) {
            LOG.error("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), String.format("Factory URL with id %s is not found.", id));
        }

        if (type == null || type.isEmpty()) {
            LOG.error("Snippet type is not found");
            throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), String.format("Snippet type \"%s\" is unsupported", type));
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
                                              String.format("Snippet type \"%s\" is unsupported", type));
        }
    }

    @GET
    @Path("ping")
    public Response ping() {
        return Response.ok().build();
    }


    private static String generateFactoryUrl(String id, UriInfo uriInfo) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory-" + id).build().toString();
    }
}
