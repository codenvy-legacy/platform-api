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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/** Helper class for creation links. */
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html"));

    public static Set<Link> createLinks(AdvancedFactoryUrl factoryUrl, Set<Image> images, UriInfo uriInfo) {
        Set<Link> links = new LinkedHashSet<>();

        links.add(generateFactoryUrlLink(factoryUrl.getId(), uriInfo));
        links.add(generateCreationlLink(factoryUrl.getId(), uriInfo));
        for (Image image : images) {
            links.add(generateFactoryImageLink(image.getName(), uriInfo));
        }
        links.addAll(generateSnippetLinks(factoryUrl.getId(), uriInfo));

        links.addAll(generateSnippetLinks(factoryUrl.getId(), uriInfo));

        return links;
    }

    private static Link generateFactoryImageLink(String imageId, UriInfo uriInfo) {
        return new Link("image/" + imageId.substring(imageId.lastIndexOf('.') + 1),
                        generatePath(uriInfo, imageId, "factory/image"), "image");
    }

    private static Link generateFactoryUrlLink(String id, UriInfo uriInfo) {
        return new Link(MediaType.APPLICATION_JSON, generatePath(uriInfo, id, "factory"), "self");
    }

    private static Link generateCreationlLink(String id, UriInfo uriInfo) {
        UriBuilder ub;
        if (uriInfo != null) {
            ub = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            ub = UriBuilder.fromUri("/");
        }
        ub.replacePath("factory");
        ub.queryParam("id", id);
        return new Link(MediaType.TEXT_HTML, ub.build().toString(), "create-project");
    }

    private static Set<Link> generateSnippetLinks(String id, UriInfo uriInfo) {
        Set<Link> result = new LinkedHashSet<>();
        for (String snippetType : snippetTypes) {
            result.add(new Link(MediaType.TEXT_PLAIN, generatePath(uriInfo, id, "factory", "snippet", "type", snippetType),
                                "snippet/" + snippetType));
        }
        return result;
    }

    private static String generatePath(UriInfo uriInfo, String id, String rel) {
        UriBuilder ub;
        if (uriInfo != null) {
            ub = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            ub = UriBuilder.fromUri("/");
        }
        ub.path(rel);
        ub.path(id);

        return ub.build().toString();
    }

    private static String generatePath(UriInfo uriInfo, String id, String rel, String servletPath, String... query) {
        UriBuilder ub;
        if (uriInfo != null) {
            ub = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            ub = UriBuilder.fromUri("/");
        }
        ub.path(rel);
        ub.path(id);
        ub.path(servletPath);

        if (query != null && query.length > 0) {
            for (int i = 0; i < query.length; i++) {
                String name = query[i];
                String value = i < query.length ? query[++i] : "";
                ub.queryParam(name, value);
            }
        }
        return ub.build().toString();
    }
}
