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
package com.codenvy.api.builder;

import com.codenvy.api.builder.dto.BuilderServerLocation;
import com.codenvy.api.builder.dto.BuilderServerRegistration;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Builder admin API.
 *
 * @author andrew00x
 */
@Path("admin/builder")
@Description("Builder API")
@RolesAllowed("system/admin")
public class BuilderAdminService extends Service {
    @Inject
    private BuildQueue buildQueue;

    @GenerateLink(rel = Constants.LINK_REL_REGISTER_BUILDER_SERVICE)
    @POST
    @Path("server/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(BuilderServerRegistration registration) throws Exception {
        buildQueue.registerBuilderServer(registration);
        return Response.status(Response.Status.OK).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_BUILDER_SERVICE)
    @POST
    @Path("server/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(BuilderServerLocation location) throws Exception {
        buildQueue.unregisterBuilderServer(location);
        return Response.status(Response.Status.OK).build();
    }
}
