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

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.manager.dto.BuilderServiceLocation;
import com.codenvy.api.builder.manager.dto.BuilderServiceRegistration;
import com.codenvy.api.builder.manager.dto.BuilderState;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.dto.JsonDto;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@Path("api/{ws-name}/builder")
public final class BuilderService extends Service {
    @Inject
    private RequestQueue requestQueue;

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    public Response build(@PathParam("ws-name") String workspace,
                          @PathParam("project") String project) throws Exception {
        return Response.status(Response.Status.OK)
                       .entity(JsonDto.toJson(requestQueue.schedule(workspace, project).getDescriptor(getServiceContext()))).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dependencies(@PathParam("ws-name") String workspace,
                                 @PathParam("project") String project,
                                 @QueryParam("type") String analyzeType) throws Exception {
        return Response.status(Response.Status.OK)
                       .entity(JsonDto.toJson(requestQueue.schedule(workspace, project, analyzeType).getDescriptor(getServiceContext())))
                       .build();
    }

    @GET
    @Path("status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status(@PathParam("id") Long id) throws Exception {
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(requestQueue.get(id).getDescriptor(getServiceContext()))).build();
    }

    @POST
    @Path("cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancel(@PathParam("id") Long id) throws Exception {
        final WaitingBuildTask waiting = requestQueue.get(id);
        if (waiting.isWaiting()) {
            waiting.cancel();
        } else {
            waiting.getRemoteTask().cancel();
        }
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(waiting.getDescriptor(getServiceContext()))).build();
    }

    //

    @RolesAllowed("cloud/admin")
    @GenerateLink(rel = Constants.LINK_REL_QUEUE_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response state() {
        return Response.status(Response.Status.OK)
                       .entity(JsonDto.toJson(new BuilderState(requestQueue.getTotalNum(), requestQueue.getWaitingNum()))).build();
    }

    @RolesAllowed("cloud/admin")
    @GenerateLink(rel = Constants.LINK_REL_REGISTER_BUILDER_SERVICE)
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(String str) throws Exception {
        final BuilderServiceRegistration registration = JsonDto.fromJson(str).cast();
        requestQueue.registerBuilderService(registration);
        return Response.status(Response.Status.OK).build();
    }

    @RolesAllowed("cloud/admin")
    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_BUILDER_SERVICE)
    @POST
    @Path("unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(String str) throws Exception {
        final BuilderServiceLocation location = JsonDto.fromJson(str).cast();
        requestQueue.unregisterBuilderService(location);
        return Response.status(Response.Status.OK).build();
    }
}
