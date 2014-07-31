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
@Path("builder/{ws-id}")
@Description("Builder API")
public final class BuilderService extends Service {
    @Inject
    private BuildQueue buildQueue;

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@PathParam("ws-id") String workspace,
                                     @Required @Description("project name") @QueryParam("project") String project,
                                     @Description("build options") BuildOptions options) throws Exception {
        return buildQueue.scheduleBuild(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@PathParam("ws-id") String workspace,
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @Valid({"copy", "list"}) @DefaultValue("list") @QueryParam("type") String analyzeType)
            throws Exception {
        return buildQueue.scheduleDependenciesAnalyze(workspace, project, analyzeType, getServiceContext()).getDescriptor();
    }

    @GET
    @Path("builds")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BuildTaskDescriptor> builds(@PathParam("ws-id") String workspace,
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

    @GET
    @Path("status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@PathParam("id") Long id) throws Exception {
        return buildQueue.getTask(id).getDescriptor();
    }

    @POST
    @Path("cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@PathParam("id") Long id) throws Exception {
        final BuildQueueTask task = buildQueue.getTask(id);
        task.cancel();
        return task.getDescriptor();
    }

    @GET
    @Path("logs/{id}")
    public void getLogs(@PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @GET
    @Path("report/{id}")
    public void getReport(@PathParam("id") Long id,
                          @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readReport(new HttpServletProxyResponse(httpServletResponse));
    }

    @GET
    @Path("download/{id}")
    public void download(@PathParam("id") Long id,
                         @Required @QueryParam("path") String path,
                         @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).download(path, new HttpServletProxyResponse(httpServletResponse));
    }

    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("builders")
    public List<BuilderDescriptor> getRegisteredServers(@PathParam("ws-id") String workspace) throws Exception {
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
