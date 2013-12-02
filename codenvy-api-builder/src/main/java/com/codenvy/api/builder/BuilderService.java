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

import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.WebHookPayload;
import com.codenvy.api.core.rest.HttpServletProxyResponse;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.annotations.Valid;

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
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@Path("{ws-name}/builder")
@Description("Builder API")
public final class BuilderService extends Service {
    @Inject
    private BuildQueue buildQueue;

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@PathParam("ws-name") String workspace,
                                     @Required @Description("project name") @QueryParam("project") String project) throws Exception {
        return buildQueue.scheduleBuild(workspace, project, getServiceContext()).getDescriptor(getServiceContext());
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@PathParam("ws-name") String workspace,
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @Valid({"copy", "list"}) @DefaultValue("list") @QueryParam("type") String analyzeType)
            throws Exception {
        return buildQueue.scheduleDependenciesAnalyze(workspace, project, analyzeType, getServiceContext()).getDescriptor(
                getServiceContext());
    }

    @GET
    @Path("status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@PathParam("id") Long id) throws Exception {
        return buildQueue.get(id).getDescriptor(getServiceContext());
    }

    @POST
    @Path("cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@PathParam("id") Long id) throws Exception {
        final BuildQueueTask task = buildQueue.get(id);
        task.cancel();
        return task.getDescriptor(getServiceContext());
    }

    @GET
    @Path("logs/{id}")
    public void getLogs(@PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.get(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @GET
    @Path("report/{id}")
    public void getReport(@PathParam("id") Long id,
                          @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.get(id).readReport(new HttpServletProxyResponse(httpServletResponse));
    }

    @GET
    @Path("download/{id}")
    public void download(@PathParam("id") Long id,
                         @Required @QueryParam("path") String path,
                         @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.get(id).download(path, new HttpServletProxyResponse(httpServletResponse));
    }

    @POST
    @Path("webhook")
    public void webhook(WebHookPayload callback) throws Exception {
        System.out.println("CALLBACK: " + callback); // TODO
    }
}
