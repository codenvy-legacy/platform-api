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
package com.codenvy.api.runner;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.RunnerServer;
import com.codenvy.api.runner.dto.RunnerServerLocation;
import com.codenvy.api.runner.dto.RunnerServerRegistration;
import com.codenvy.api.runner.internal.Constants;
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

    private static final String[] SERVER_LINK_RELS = new String[]{Constants.LINK_REL_AVAILABLE_RUNNERS,
                                                                  Constants.LINK_REL_SERVER_STATE,
                                                                  Constants.LINK_REL_RUNNER_STATE};

    @GenerateLink(rel = Constants.LINK_REL_REGISTERED_RUNNER_SERVER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("server")
    public List<RunnerServer> getRegisteredServers() throws Exception {
        final List<RemoteRunnerServer> runnerServers = runner.getRegisterRunnerServers();
        final List<RunnerServer> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (RemoteRunnerServer runnerServer : runnerServers) {
            final List<Link> adminLinks = new LinkedList<>();
            for (String linkRel : SERVER_LINK_RELS) {
                final Link link = runnerServer.getLink(linkRel);
                if (link != null) {
                    if (Constants.LINK_REL_RUNNER_STATE.equals(linkRel)) {
                        for (RunnerDescriptor runnerImpl : runnerServer.getAvailableRunners()) {
                            final String href = link.getHref();
                            final String hrefWithRunner = href + ((href.indexOf('?') > 0 ? '&' : '?') + "runner=" + runnerImpl.getName());
                            final Link linkCopy = dtoFactory.clone(link);
                            linkCopy.getParameters().clear();
                            linkCopy.setHref(hrefWithRunner);
                            adminLinks.add(linkCopy);
                        }
                    } else {
                        adminLinks.add(link);
                    }
                }
            }
            result.add(dtoFactory.createDto(RunnerServer.class)
                                 .withUrl(runnerServer.getBaseUrl())
                                 .withDescription(runnerServer.getServiceDescriptor().getDescription())
                                 .withDedicated(runnerServer.isDedicated())
                                 .withWorkspace(runnerServer.getAssignedWorkspace())
                                 .withProject(runnerServer.getAssignedProject())
                                 .withServerState(runnerServer.getServerState())
                                 .withLinks(adminLinks));
        }

        return result;
    }

    @GenerateLink(rel = Constants.LINK_REL_RUNNER_TASKS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("queue")
    public List<ApplicationProcessDescriptor> getTasks() throws Exception {
        final List<ApplicationProcessDescriptor> result = new LinkedList<>();
        for (RunQueueTask queueTask : runner.getTasks()) {
            final ApplicationProcessDescriptor descriptor = queueTask.getDescriptor();
            final RemoteRunnerProcess remoteProcess = queueTask.getRemoteProcess();
            if (remoteProcess != null) {
                descriptor.setServerUrl(remoteProcess.getServerUrl());
            }
            result.add(descriptor);
        }
        return result;
    }
}
