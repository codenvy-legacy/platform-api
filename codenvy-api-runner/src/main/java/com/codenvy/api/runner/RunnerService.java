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

import com.codenvy.api.core.rest.HttpServletProxyResponse;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.ResourcesDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.*;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * RESTful API for RunQueue.
 *
 * @author andrew00x
 */
@Api(value = "/runner/{ws-id}",
     description = "Runner manager")
@Path("/runner/{ws-id}")
@Description("Runner REST API")
public class RunnerService extends Service {
    @Inject
    private RunQueue runQueue;

    @ApiOperation(value = "Run project",
                  notes = "Run selected project",
                  response = ApplicationProcessDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("/run")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = true)
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @ApiParam(value = "Run options")
                                            @Description("run options") RunOptions options) throws Exception {
        return runQueue.run(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @ApiOperation(value = "Get run status",
                  notes = "Get status of a selected run process",
                  response = ApplicationProcessDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@ApiParam(value = "Workspace ID", required = true)
                                                  @PathParam("ws-id") String workspace,
                                                  @ApiParam(value = "Run ID", required = true)
                                                  @PathParam("id") Long id) throws Exception {
        return runQueue.getTask(id).getDescriptor();
    }

    @ApiOperation(value = "Get run processes",
                  notes = "Get info on all running processes",
                  response = ApplicationProcessDescriptor.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/processes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationProcessDescriptor> getRunningProcesses(@ApiParam(value = "Workspace ID", required = true)
                                                                  @PathParam("ws-id") String workspace,
                                                                  @ApiParam(value = "Project name", required = false)
                                                                  @Required @Description("project name")
                                                                  @QueryParam("project") String project) throws Exception {
        if (project != null && !project.startsWith("/")) {
            project = '/' + project;
        }
        final List<ApplicationProcessDescriptor> processes = new LinkedList<>();
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            final String userName = user.getName();
            for (RunQueueTask task : runQueue.getTasks()) {
                final RunRequest request = task.getRequest();
                if (request.getWorkspace().equals(workspace)
                    && request.getProject().equals(project)
                    && request.getUserName().equals(userName)) {

                    processes.add(task.getDescriptor());
                }
            }
        }
        return processes;
    }

    @ApiOperation(value = "Stop run process",
                  notes = "Stop running process",
                  response = ApplicationProcessDescriptor.class,
                  position = 4)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/stop/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@ApiParam(value = "Workspace ID", required = true)
                                             @PathParam("ws-id") String workspace,
                                             @ApiParam(value = "Run ID", required = true)
                                             @PathParam("id") Long id) throws Exception {
        final RunQueueTask task = runQueue.getTask(id);
        task.cancel();
        return task.getDescriptor();
    }

    @ApiOperation(value = "Get logs",
                  notes = "Get logs from a running application",
                  position = 5)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/logs/{id}")
    public void getLogs(@ApiParam(value = "Workspace ID", required = true)
                        @PathParam("ws-id") String workspace,
                        @ApiParam(value = "Run ID", required = true)
                        @PathParam("id") Long id, @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response is written directly to the servlet request stream
        runQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Get available RAM resources",
                  notes = "Get RAM resources of a workspace: used and free RAM",
                  response = ResourcesDescriptor.class,
                  position = 6)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/resources")
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcesDescriptor getResources(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace) throws Exception {
        return DtoFactory.getInstance().createDto(ResourcesDescriptor.class)
                         .withTotalMemory(String.valueOf(runQueue.getTotalMemory(workspace, getServiceContext())))
                         .withUsedMemory(String.valueOf(runQueue.getUsedMemory(workspace)));
    }

    @ApiOperation(value = "Get available runners",
                  notes = "Get available runners",
                  response = RunnerDescriptor.class,
                  responseContainer = "List",
                  position = 7)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_RUNNERS)
    @GET
    @Path("/available")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RunnerDescriptor> getRunners(@ApiParam(value = "Workspace ID", required = true)
                                             @PathParam("ws-id") String workspace,
                                             @ApiParam(value = "Project name")
                                             @Description("project name") @QueryParam("project") String project) throws Exception {
        final Map<String, RunnerDescriptor> all = new HashMap<>();
        for (RemoteRunnerServer runnerServer : runQueue.getRegisterRunnerServers()) {
            if ((!runnerServer.isDedicated() || runnerServer.getAssignedWorkspace().equals(workspace))
                && (project == null || project.equals(runnerServer.getAssignedProject()))) {
                for (RunnerDescriptor runnerDescriptor : runnerServer.getAvailableRunners()) {
                    if (all.containsKey(runnerDescriptor.getName())) {
                        all.get(runnerDescriptor.getName()).getEnvironments().putAll(runnerDescriptor.getEnvironments());
                    } else {
                        all.put(runnerDescriptor.getName(), runnerDescriptor);
                    }
                }
            }
        }
        return new ArrayList<>(all.values());
    }


    @ApiOperation(value = "Get runtime recipe",
                  notes = "Get content of a Dockerfile used to 'cook' runtime environment",
                  position = 8)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/recipe/{id}")
    public void getRecipeFile(@ApiParam(value = "Workspace ID", required = true)
                              @PathParam("ws-id") String workspace,
                              @ApiParam(value = "Run ID", required = true)
                              @PathParam("id") Long id,
                              @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        runQueue.getTask(id).readRecipeFile(new HttpServletProxyResponse(httpServletResponse));
    }
}
