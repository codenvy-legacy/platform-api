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

import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.core.rest.HttpServletProxyResponse;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.annotations.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * RESTful frontend for BuildQueue.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Path("builder/{ws-id}")
@Description("Builder API")
public final class BuilderService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(BuilderService.class);

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
}
