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
package com.codenvy.api.runner;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.RunnerServer;
import com.codenvy.api.runner.dto.RunnerServerLocation;
import com.codenvy.api.runner.dto.RunnerServerRegistration;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.runner.internal.dto.ServerState;
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
import java.util.LinkedList;
import java.util.List;

/**
 * RESTful API for administration.
 *
 * @author andrew00x
 */
@Path("admin/runner")
@Description("Runner administration REST API")
@RolesAllowed("system/admin")
public class RunnerAdminService extends Service {
    @Inject
    private RunQueue runner;

    @GenerateLink(rel = Constants.LINK_REL_REGISTER_RUNNER_SERVER)
    @POST
    @Path("server/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerRunnerServer(RunnerServerRegistration registration) throws Exception {
        runner.registerRunnerServer(registration);
        return Response.status(Response.Status.OK).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_RUNNER_SERVER)
    @POST
    @Path("server/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregisterRunnerServer(RunnerServerLocation location) throws Exception {
        runner.unregisterRunnerServer(location);
        return Response.status(Response.Status.OK).build();
    }

    private static String[] SERVER_LINK_RELS = new String[]{Constants.LINK_REL_AVAILABLE_RUNNERS,
                                                            Constants.LINK_REL_SERVER_STATE,
                                                            Constants.LINK_REL_RUNNER_STATE};

    @GenerateLink(rel = Constants.LINK_REL_REGISTERED_RUNNER_SERVER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("server")
    public List<RunnerServer> getRegisterRunnerService() throws Exception {
        final List<RemoteRunnerServer> runnerServers = runner.getRegisterRunnerServices();
        final List<RunnerServer> result = new LinkedList<>();
        for (RemoteRunnerServer runnerServer : runnerServers) {
            final ServerState serverState = runnerServer.getServerState();
            final List<Link> adminLinks = new LinkedList<>();
            for (String linkRel : SERVER_LINK_RELS) {
                final Link link = runnerServer.getLink(linkRel);
                if (link != null) {
                    adminLinks.add(link);
                }
            }
            result.add(DtoFactory.getInstance().createDto(RunnerServer.class)
                                 .withUrl(runnerServer.getBaseUrl())
                                 .withDescription(runnerServer.getServiceDescriptor().getDescription())
                                 .withDedicated(runnerServer.isDedicated())
                                 .withWorkspace(runnerServer.getAssignedWorkspace())
                                 .withProject(runnerServer.getAssignedProject())
                                 .withCpuPercentUsage(serverState.getCpuPercentUsage())
                                 .withTotalMemory(serverState.getTotalMemory())
                                 .withFreeMemory(serverState.getFreeMemory())
                                 .withLinks(adminLinks));
        }

        return result;
    }
}
