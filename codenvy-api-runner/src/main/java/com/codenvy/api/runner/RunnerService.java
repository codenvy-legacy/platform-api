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

import com.codenvy.api.core.rest.HttpServletProxyResponse;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.Constants;

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
import java.util.List;
import java.util.Map;

/**
 * RESTful API for RunQueue.
 *
 * @author andrew00x
 */
@Path("runner/{ws-id}")
@Description("Runner REST API")
public class RunnerService extends Service {
    @Inject
    private RunQueue runQueue;

    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("run")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@PathParam("ws-id") String workspace,
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @Description("run options") RunOptions options) throws Exception {
        return runQueue.run(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @GET
    @Path("status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@PathParam("id") Long id) throws Exception {
        return runQueue.getTask(id).getDescriptor();
    }

    @POST
    @Path("stop/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@PathParam("id") Long id) throws Exception {
        final RunQueueTask task = runQueue.getTask(id);
        task.cancel();
        return task.getDescriptor();
    }

    @GET
    @Path("logs/{id}")
    public void getLogs(@PathParam("id") Long id, @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response is written directly to the servlet request stream
        runQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_RUNNERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RunnerDescriptor> getRunners(@PathParam("ws-id") String workspace,
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
}
