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

import com.codenvy.api.builder.dto.BuilderServiceLocation;
import com.codenvy.api.builder.dto.BuilderServiceRegistration;
import com.codenvy.api.builder.dto.BuilderState;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

    @GenerateLink(rel = Constants.LINK_REL_QUEUE_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public BuilderState state() {
        return DtoFactory.getInstance().createDto(BuilderState.class)
                         .withTotalNum(buildQueue.getTotalNum())
                         .withWaitingNum(buildQueue.getWaitingNum());
    }

    @GenerateLink(rel = Constants.LINK_REL_REGISTER_BUILDER_SERVICE)
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(BuilderServiceRegistration registration) throws Exception {
        buildQueue.registerBuilderService(registration);
        return Response.status(Response.Status.OK).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_BUILDER_SERVICE)
    @POST
    @Path("unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(BuilderServiceLocation location) throws Exception {
        buildQueue.unregisterBuilderService(location);
        return Response.status(Response.Status.OK).build();
    }
}
