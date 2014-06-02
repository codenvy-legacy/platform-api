/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.factory;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;
import javax.ws.rs.core.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/** Helper class for creation links. */
@Singleton
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html", "iframe"));

    public List<Link> createLinks(Factory factoryUrl, Set<FactoryImage> images, UriInfo uriInfo)
            throws UnsupportedEncodingException {
        List<Link> links = new LinkedList<>();

        final UriBuilder baseUriBuilder;
        if (uriInfo != null) {
            baseUriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            baseUriBuilder = UriBuilder.fromUri("/");
        }
        // add path to factory service
        UriBuilder factoryUriBuilder = baseUriBuilder.clone().path(FactoryService.class);
        String factoryId = factoryUrl.getId();
        Link createProject;

        // uri to retrieve factory
        links.add(createLink("GET", "self", null, MediaType.APPLICATION_JSON,
                             factoryUriBuilder.clone().path(FactoryService.class, "getFactory").build(factoryId).toString(), null));

        // uri's to retrieve images
        for (FactoryImage image : images) {
            links.add(createLink("GET", "image", null, image.getMediaType(),
                                 factoryUriBuilder.clone().path(FactoryService.class, "getImage").queryParam("imgId", image.getName())
                                                  .build(factoryId).toString(), null));
        }

        // uri's of snippets
        for (String snippetType : snippetTypes) {
            links.add(createLink("GET", "snippet/" + snippetType, null, MediaType.TEXT_PLAIN,
                                 factoryUriBuilder.clone().path(FactoryService.class, "getFactorySnippet").queryParam("type", snippetType)
                                                  .build(factoryId).toString(), null));
        }

        // uri to accept factory
        createProject = createLink("GET", "create-project", null, MediaType.TEXT_HTML,
                                   baseUriBuilder.clone().replacePath("factory").queryParam("id", factoryId).build().toString(), null);
        links.add(createProject);

        // links of analytics
        links.add(createLink("GET", "accepted", null, MediaType.TEXT_PLAIN,
                             baseUriBuilder.clone().path("analytics").path("public-metric/factory_used")
                                           .queryParam("factory", URLEncoder.encode(createProject.getHref(), "UTF-8")).build().toString(),
                             null));
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
    public List<Link> getLinkByRelation(List<Link> links, String relation) {
        if (relation == null || links == null) {
            throw new IllegalArgumentException("Value of parameters can't be null.");
        }
        List<Link> result = new LinkedList<>();
        for (Link link : links) {
            if (relation.equals(link.getRel())) {
                result.add(link);
            }
        }

        return result;
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href, List<LinkParameter> params) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href)
                         .withParameters(params);
    }
}
