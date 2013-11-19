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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.SystemInfo;
import com.codenvy.api.runner.ApplicationStatus;
import com.codenvy.api.runner.NoSuchRunnerException;
import com.codenvy.api.runner.RunnerException;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.dto.RunnerList;
import com.codenvy.api.runner.internal.dto.RunnerState;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RESTful API for slave-runners.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@Path("internal/runner")
public class SlaveRunnerService extends Service {
    @Inject
    private RunnerRegistry runners;

    /** Get list of available Runners which can be accessible over this SlaveRunnerService. */
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_RUNNERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerList availableRunners() {
        final Set<Runner> all = runners.getAll();
        final List<RunnerDescriptor> list = new ArrayList<>(all.size());
        for (Runner runner : all) {
            list.add(DtoFactory.getInstance().createDto(RunnerDescriptor.class)
                               .withName(runner.getName())
                               .withDescription(runner.getDescription()));
        }
        return DtoFactory.getInstance().createDto(RunnerList.class).withRunners(list);
    }

    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("run")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@Description("Parameters for run task in JSON format") RunRequest request)
            throws RunnerException, IOException, RemoteException {
        final Runner runner = getRunner(request.getRunner());
        final RunnerProcess process = runner.execute(request);
        return getDescriptor(process, getServiceContext());
    }

    @GET
    @Path("status/{runner}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@PathParam("runner") String runner, @PathParam("id") Long id) throws Exception {
        final RunnerProcess process = getRunner(runner).getProcess(id);
        return getDescriptor(process, getServiceContext());
    }

    @POST
    @Path("stop/{runner}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@PathParam("runner") String runner, @PathParam("id") Long id) throws Exception {
        final RunnerProcess process = getRunner(runner).getProcess(id);
        process.cancel();
        return getDescriptor(process, getServiceContext());
    }

    @GET
    @Path("logs/{runner}/{id}")
    public void getLogs(@PathParam("runner") String runner,
                        @PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        final ApplicationLogger logger = getRunner(runner).getProcess(id).getLogger();
        httpServletResponse.setContentType(logger.getContentType());
        final PrintWriter output = httpServletResponse.getWriter();
        logger.getLogs(output);
        output.flush();
    }

    @GenerateLink(rel = Constants.LINK_REL_RUNNER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerState getBuilderState(@Required
                                       @Description("Name of the runner")
                                       @QueryParam("runner") String builder) throws Exception {
        final Runner myRunner = getRunner(builder);
        final ResourceAllocators allocators = ResourceAllocators.getInstance();
        return DtoFactory.getInstance().createDto(RunnerState.class)
                         .withName(myRunner.getName())
                         .withRunningAppsNum(myRunner.getRunningAppsNum())
                         .withTotalAppsNum(myRunner.getTotalAppsNum())
                         .withCpuPercentUsage(SystemInfo.cpu())
                         .withTotalMemory(allocators.totalMemory())
                         .withFreeMemory(allocators.freeMemory());
    }

    private Runner getRunner(String name) throws NoSuchRunnerException {
        final Runner myRunner = runners.get(name);
        if (myRunner == null) {
            throw new NoSuchRunnerException(name);
        }
        return myRunner;
    }

    public ApplicationProcessDescriptor getDescriptor(RunnerProcess process, ServiceContext restfulRequestContext) throws RunnerException {
        final ApplicationStatus status = process.isStopped() ? ApplicationStatus.STOPPED
                                                             : process.isRunning() ? ApplicationStatus.RUNNING : ApplicationStatus.NEW;
        final List<Link> links = new ArrayList<>(3);
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel(Constants.LINK_REL_GET_STATUS)
                            .withHref(servicePathBuilder.clone().path(getClass(), "getStatus")
                                                        .build(process.getRunner(), process.getId()).toString())
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel(Constants.LINK_REL_VIEW_LOG)
                            .withHref(servicePathBuilder.clone().path(getClass(), "getLogs")
                                                        .build(process.getRunner(), process.getId()).toString())
                            .withMethod("GET")
                            .withProduces(process.getLogger().getContentType()));
        if (ApplicationStatus.RUNNING == status) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_STOP)
                                .withHref(servicePathBuilder.clone().path(getClass(), "stop")
                                                            .build(process.getRunner(), process.getId()).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
        }
        return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                         .withProcessId(process.getId())
                         .withStatus(status)
                         .withLinks(links);
    }
}
