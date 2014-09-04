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
package com.codenvy.api.builder;

import com.codenvy.api.builder.dto.BaseBuilderRequest;
import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.rest.HttpServletProxyResponse;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.annotations.Valid;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.LinkedList;
import java.util.List;

/**
 * RESTful frontend for BuildQueue.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Api(value = "/builder/{ws-id}",
     description = "Builder manager")
@Path("/builder/{ws-id}")
@Description("Builder API")
public final class BuilderService extends Service {
    @Inject
    private BuildQueue buildQueue;

    @ApiOperation(value = "Build a project",
                  notes = "Build a project. Optional build options are passed in a JSON",
                  response = BuildTaskDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("/build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@PathParam("ws-id") String workspace,
                                     @ApiParam(value = "Project name", required = true)
                                     @Required @Description("project name") @QueryParam("project") String project,
                                     @ApiParam(value = "Build options. Here you specify optional build options like skip tests, build targets etc.")
                                     @Description("build options") BuildOptions options) throws Exception {
        return buildQueue.scheduleBuild(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @ApiOperation(value = "Analyze dependencies",
                  notes = "Analyze dependencies",
                  response = BuildTaskDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("/dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = true)
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @ApiParam(value = "Analysis type. If dropped, list is used by default", defaultValue = "list", allowableValues = "copy,list")
                                            @Valid({"copy", "list"}) @DefaultValue("list") @QueryParam("type") String analyzeType)
            throws Exception {
        return buildQueue.scheduleDependenciesAnalyze(workspace, project, analyzeType, getServiceContext()).getDescriptor();
    }

    @ApiOperation(value = "Get project build tasks",
                  notes = "Get build tasks that are related to a particular project. User can see only own processes related to own projects.",
                  response = BuildTaskDescriptor.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/builds")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BuildTaskDescriptor> builds(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = false)
                                            @Required @Description("project name")
                                            @QueryParam("project") String project) throws Exception {
        // handle project name
        if (project != null && !project.startsWith("/")) {
            project = '/' + project;
        }
        final List<BuildTaskDescriptor> builds = new LinkedList<>();
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            final String userName = user.getName();
            for (BuildQueueTask task : buildQueue.getTasks()) {
                final BaseBuilderRequest request = task.getRequest();
                if (request.getWorkspace().equals(workspace)
                    && request.getProject().equals(project)
                    && request.getUserName().equals(userName)) {

                    builds.add(task.getDescriptor());
                }
            }
        }
        return builds;
    }

    @ApiOperation(value = "Get build status",
                  notes = "Get status of a specified build",
                  response = BuildTaskDescriptor.class,
                  position = 4)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @GET
    @Path("/status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@ApiParam(value = "Workspace ID", required = true)
                                         @PathParam("ws-id") String workspace,
                                         @ApiParam(value = "Build ID", required = true)
                                         @PathParam("id")
                                         Long id) throws Exception {
        return buildQueue.getTask(id).getDescriptor();
    }

    @ApiOperation(value = "Cancel build",
                  notes = "Cancel build task",
                  response = BuildTaskDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @POST
    @Path("/cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@ApiParam(value = "Workspace ID", required = true)
                                      @PathParam("ws-id") String workspace,
                                      @ApiParam(value = "Build ID", required = true)
                                      @PathParam("id") Long id) throws Exception {
        final BuildQueueTask task = buildQueue.getTask(id);
        task.cancel();
        return task.getDescriptor();
    }

    @ApiOperation(value = "Get build logs",
                  notes = "Get build logs",
                  position = 5)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @GET
    @Path("/logs/{id}")
    public void getLogs(@ApiParam(value = "Workspace ID", required = true)
                        @PathParam("ws-id") String workspace,
                        @ApiParam(value = "Get build logs", required = true)
                        @PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }


    @ApiOperation(value = "Get build report",
                  notes = "Get build report by build ID",
                  position = 6)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @GET
    @Path("/report/{id}")
    public void getReport(@ApiParam(value = "Workspace ID", required = true)
                          @PathParam("ws-id") String workspace,
                          @ApiParam(value = "Build ID", required = true)
                          @PathParam("id") Long id,
                          @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readReport(new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Download build artifact",
                  notes = "Download build artifact",
                  position = 7)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @GET
    @Path("/download/{id}")
    public void download(@ApiParam(value = "Workspace ID", required = true)
                         @PathParam("ws-id") String workspace,
                         @ApiParam(value = "Build ID", required = true)
                         @PathParam("id") Long id,
                         @ApiParam(value = "Path to a project as /target/{BuildArtifactName}", required = true)
                         @Required @QueryParam("path") String path,
                         @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).download(path, new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Get all builders",
                  notes = "Get information on all registered builders",
                  response = BuilderDescriptor.class,
                  responseContainer = "List",
                  position = 8)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found")})
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/builders")
    public List<BuilderDescriptor> getRegisteredServers(@ApiParam(value = "Workspace ID", required = true)
                                                        @PathParam("ws-id") String workspace) throws Exception {
        final List<RemoteBuilderServer> runnerServers = buildQueue.getRegisterBuilderServers();
        final List<BuilderDescriptor> result = new LinkedList<>();
        for (RemoteBuilderServer builderServer : runnerServers) {
            final String assignedWorkspace = builderServer.getAssignedWorkspace();
            if (assignedWorkspace == null || assignedWorkspace.equals(workspace)) {
                result.addAll(builderServer.getAvailableBuilders());
            }
        }
        return result;
    }
}
