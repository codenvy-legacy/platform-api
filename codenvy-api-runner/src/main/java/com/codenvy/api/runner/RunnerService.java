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
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * RESTful API for RunQueue.
 *
 * @author andrew00x
 */
@Path("runner/{ws-name}")
@Description("Runner API")
public class RunnerService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerService.class);

    @Inject
    private RunQueue runQueue;

    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("run")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@PathParam("ws-name") String workspace,
                                            @Required @Description("project name") @QueryParam("project") String project) throws Exception {
        final ServiceContext serviceContext = getServiceContext();
        return runQueue.run(workspace, project, serviceContext).getDescriptor(serviceContext);
    }

    @GET
    @Path("status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@PathParam("id") Long id) throws Exception {
        return runQueue.getTask(id).getDescriptor(getServiceContext());
    }

    @POST
    @Path("stop/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@PathParam("id") Long id) throws Exception {
        final RunQueueTask task = runQueue.getTask(id);
        task.stop();
        return task.getDescriptor(getServiceContext());
    }

    @GET
    @Path("logs/{id}")
    public void getLogs(@PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        runQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @POST
    @Path("webhook/{id}")
    public void webhook(@PathParam("id") Long id, @QueryParam("event") String event) {
        final ChannelBroadcastMessage message = new ChannelBroadcastMessage();
        try {
            final ApplicationProcessDescriptor processDescriptor = runQueue.getTask(id).getDescriptor(getServiceContext());
            message.setChannel("runner:status:" + id);
            message.setType(ChannelBroadcastMessage.Type.NONE);
            message.setBody(DtoFactory.getInstance().toJson(processDescriptor));
        } catch (Exception e) {
            message.setType(ChannelBroadcastMessage.Type.ERROR);
            message.setBody(e.getMessage());
        }
        try {
            WSConnectionContext.sendMessage(message);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
