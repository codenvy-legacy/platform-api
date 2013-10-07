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

import com.codenvy.api.analytics.server.MetricService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/** Helper class for creation links. */
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html"));

    public static Set<Link> createLinks(AdvancedFactoryUrl factoryUrl, Set<Image> images, UriInfo uriInfo) {
        Set<Link> links = new LinkedHashSet<>();

        final UriBuilder baseUriBuilder;
        if (uriInfo != null) {
            baseUriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            baseUriBuilder = UriBuilder.fromUri("/");
        }
        // add path to factory service
        baseUriBuilder.path(FactoryService.class);

        String fId = factoryUrl.getId();

        // uri to retrieve factory
        links.add(
                new Link(MediaType.APPLICATION_JSON, baseUriBuilder.clone().path(FactoryService.class, "getFactory").build(fId).toString(),
                         "self"));

        // uri's to retrieve images
        for (Image image : images) {
            links.add(new Link(image.getMediaType(),
                               baseUriBuilder.clone().path(FactoryService.class, "getImage").queryParam("imgId", image.getName())
                                             .build(fId, image.getName()).toString(), "image"));
        }

        // uri's of snippets
        for (String snippetType : snippetTypes) {
            links.add(new Link(MediaType.TEXT_PLAIN,
                               baseUriBuilder.clone().path(FactoryService.class, "getFactorySnippet").queryParam("type", snippetType)
                                             .build(fId).toString(), "snippet/" + snippetType));
        }

        // uri to accept factory
        links.add(new Link(MediaType.TEXT_HTML, baseUriBuilder.clone().replacePath("factory").queryParam("id", fId).build().toString(),
                           "create-project"));

        // links of analytics
        links.add(new Link(MediaType.TEXT_PLAIN,
                           baseUriBuilder.clone().replacePath(null).path(MetricService.class).build("FACTORY_URL_ACCEPTED_NUMBER/" + fId)
                                         .toString(), "accepted"));

        return links;
    }
}
