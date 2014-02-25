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

import com.codenvy.api.factory.dto.AdvancedFactoryUrl;
import com.codenvy.api.factory.dto.Link;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/** Helper class for creation links. */
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html"));

    public static List<Link> createLinks(AdvancedFactoryUrl factoryUrl, Set<FactoryImage> images, UriInfo uriInfo)
            throws UnsupportedEncodingException {
        List<Link> links = new ArrayList<>();

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
        Link self = DtoFactory.getInstance().createDto(Link.class);
        self.setType(MediaType.APPLICATION_JSON);
        self.setHref(factoryUriBuilder.clone().path(FactoryService.class, "getFactory").build(fId).toString());
        self.setRel("self");
        links.add(self);

        // uri's to retrieve images
        for (FactoryImage image : images) {
            Link imageLink = DtoFactory.getInstance().createDto(Link.class);
            imageLink.setType(image.getMediaType());
            imageLink.setHref(factoryUriBuilder.clone().path(FactoryService.class, "getImage").queryParam("imgId", image.getName())
                                               .build(fId, image.getName()).toString());
            imageLink.setRel("image");
            links.add(imageLink);
        }

        // uri's of snippets
        for (String snippetType : snippetTypes) {
            Link snippetLink = DtoFactory.getInstance().createDto(Link.class);
            snippetLink.setType(MediaType.TEXT_PLAIN);
            snippetLink.setHref(factoryUriBuilder.clone().path(FactoryService.class, "getFactorySnippet").queryParam("type", snippetType)
                                                 .build(fId).toString());
            snippetLink.setRel("snippet/" + snippetType);
            links.add(snippetLink);
        }

        // uri to accept factory
        Link createProject = DtoFactory.getInstance().createDto(Link.class);
        createProject.setType(MediaType.TEXT_HTML);
        createProject.setHref(baseUriBuilder.clone().replacePath("factory").queryParam("id", fId).build().toString());
        createProject.setRel("create-project");
        links.add(createProject);

        // links of analytics
        Link accept = DtoFactory.getInstance().createDto(Link.class);
        accept.setType(MediaType.TEXT_PLAIN);
        accept.setHref(baseUriBuilder.clone().path("analytics").path("public-metric/factory_used")
                                     .queryParam("factory", URLEncoder.encode(createProject.getHref(), "UTF-8")).build().toString());
        accept.setRel("accepted");
        links.add(accept);

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
    public static List<Link> getLinkByRelation(List<Link> links, String relation) {
        if (relation == null || links == null) {
            throw new IllegalArgumentException("Value of parameters can't be null.");
        }
        List<Link> result = new ArrayList<>();
        for (Link link : links) {
            if (relation.equals(link.getRel())) {
                result.add(link);
            }
        }

        return result;
    }
}
