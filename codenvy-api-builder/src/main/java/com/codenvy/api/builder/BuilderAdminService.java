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

import com.codenvy.api.builder.dto.BuilderServer;
import com.codenvy.api.builder.dto.BuilderServerLocation;
import com.codenvy.api.builder.dto.BuilderServerRegistration;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.dto.BuilderDescriptor;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.shared.dto.Link;
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
 * Builder admin API.
 *
 * @author andrew00x
 */
@Path("admin/builder")
@Description("Builder API")
@RolesAllowed("system/admin")
public class BuilderAdminService extends Service {
    @Inject
    private BuildQueue buildQueue;

    @GenerateLink(rel = Constants.LINK_REL_REGISTER_BUILDER_SERVICE)
    @POST
    @Path("server/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(BuilderServerRegistration registration) throws Exception {
        buildQueue.registerBuilderServer(registration);
        return Response.status(Response.Status.OK).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_BUILDER_SERVICE)
    @POST
    @Path("server/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(BuilderServerLocation location) throws Exception {
        buildQueue.unregisterBuilderServer(location);
        return Response.status(Response.Status.OK).build();
    }

    private static String[] SERVER_LINK_RELS = new String[]{Constants.LINK_REL_AVAILABLE_BUILDERS,
                                                            Constants.LINK_REL_SERVER_STATE,
                                                            Constants.LINK_REL_BUILDER_STATE};

    @GenerateLink(rel = Constants.LINK_REL_REGISTERED_BUILDER_SERVER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("server")
    public List<BuilderServer> getRegisteredServers() throws Exception {
        final List<RemoteBuilderServer> runnerServers = buildQueue.getRegisterBuilderServers();
        final List<BuilderServer> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (RemoteBuilderServer builderServer : runnerServers) {
            final List<Link> adminLinks = new LinkedList<>();
            for (String linkRel : SERVER_LINK_RELS) {
                final Link link = builderServer.getLink(linkRel);
                if (link != null) {
                    if (Constants.LINK_REL_BUILDER_STATE.equals(linkRel)) {
                        for (BuilderDescriptor builderImpl : builderServer.getAvailableBuilders()) {
                            final String href = link.getHref();
                            final String hrefWithRunner = href + ((href.indexOf('?') > 0 ? '&' : '?') + "builder=" + builderImpl.getName());
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
            result.add(dtoFactory.createDto(BuilderServer.class)
                                 .withUrl(builderServer.getBaseUrl())
                                 .withDescription(builderServer.getServiceDescriptor().getDescription())
                                 .withDedicated(builderServer.isDedicated())
                                 .withWorkspace(builderServer.getAssignedWorkspace())
                                 .withProject(builderServer.getAssignedProject())
                                 .withServerState(builderServer.getServerState())
                                 .withLinks(adminLinks));
        }

        return result;
    }
}
