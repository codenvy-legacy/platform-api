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

import com.codenvy.api.analytics.AnalyticsService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/** Helper class for creation links. */
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html"));

    public static Set<Link> createLinks(AdvancedFactoryUrl factoryUrl, Set<FactoryImage> images, UriInfo uriInfo)
            throws UnsupportedEncodingException {
        Set<Link> links = new LinkedHashSet<>();

        final UriBuilder baseUriBuilder;
        if (uriInfo != null) {
            baseUriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            baseUriBuilder = UriBuilder.fromUri("/");
        }
        // add path to factory service
        UriBuilder factoryUriBuilder = baseUriBuilder.clone().path(FactoryService.class);

        String fId = factoryUrl.getId();

        // uri to retrieve factory
        links.add(
                new Link(MediaType.APPLICATION_JSON,
                         factoryUriBuilder.clone().path(FactoryService.class, "getFactory").build(fId).toString(),
                         "self"));

        // uri's to retrieve images
        for (FactoryImage image : images) {
            links.add(new Link(image.getMediaType(),
                               factoryUriBuilder.clone().path(FactoryService.class, "getImage").queryParam("imgId", image.getName())
                                                .build(fId, image.getName()).toString(), "image"));
        }

        // uri's of snippets
        for (String snippetType : snippetTypes) {
            links.add(new Link(MediaType.TEXT_PLAIN,
                               factoryUriBuilder.clone().path(FactoryService.class, "getFactorySnippet").queryParam("type", snippetType)
                                                .build(fId).toString(), "snippet/" + snippetType));
        }

        // uri to accept factory
        Link createProject =
                new Link(MediaType.TEXT_HTML, baseUriBuilder.clone().replacePath("factory").queryParam("id", fId).build().toString(),
                         "create-project");
        links.add(createProject);

        // links of analytics
        links.add(new Link(MediaType.TEXT_PLAIN,
                           baseUriBuilder.clone().path(AnalyticsService.class).path(AnalyticsService.class, "getValue")
                                         .queryParam("factory_url", URLEncoder.encode(createProject.getHref(), "UTF-8")).build(
                                   "FACTORY_URL_ACCEPTED_NUMBER").toString(),
                           "accepted"));

        return links;
    }

    /**
     * Find links with given relation.
     *
     * @param links
     *         - links for searching
     * @param relation
     *         - searching relation
     * @return - set of links with relation equal to desired, empty set if there is no such links
     */
    public static Set<Link> getLinkByRelation(Set<Link> links, String relation) {
        if (relation == null || links == null) {
            throw new IllegalArgumentException("Value of parameters can't be null.");
        }
        Set<Link> result = new LinkedHashSet<>();
        for (Link link : links) {
            if (relation.equals(link.getRel())) {
                result.add(link);
            }
        }

        return result;
    }
}
