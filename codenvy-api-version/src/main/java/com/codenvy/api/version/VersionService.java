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
package com.codenvy.api.version;


import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Service is responsible to return versions of the components.
 *
 * @author Anatoliy Bazko
 */
@Path("version")
public class VersionService extends Service {
    public static final String COMPONENT = "component";
    public static final String VERSION   = "version";

    private final Properties versions;

    @Inject
    public VersionService() throws IOException {
        versions = new Properties();
        try (InputStream in = VersionService.class.getClassLoader().getResourceAsStream("codenvy/BuildInfo.properties")) {
            versions.load(in);
        }
    }

    @GenerateLink(rel = "get component version")
    @GET
    @Path("{component}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion(@PathParam("component") String component) {
        if (versions.containsKey(component)) {
            Map<String, String> result = new HashMap<>(2);
            result.put(COMPONENT, component);
            result.put(VERSION, (String)versions.get(component));

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(result)).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Component '" + component + "' not found").build();
        }
    }
}
