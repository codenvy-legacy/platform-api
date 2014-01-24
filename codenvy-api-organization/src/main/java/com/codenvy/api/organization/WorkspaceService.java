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
package com.codenvy.api.organization;


import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.organization.dao.MemberDao;
import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.WorkspaceDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Workspace;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.sun.corba.se.spi.orbutil.threadpool.Work;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 */
@Path("/workspace")
public class WorkspaceService extends Service {

    private static final int ID_LENGTH = 16;
    private final WorkspaceDao workspaceDao;
    private final UserDao      userDao;
    private final MemberDao    memberDao;

    @Inject
    public WorkspaceService(WorkspaceDao workspaceDao, UserDao userDao, MemberDao memberDao) {
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
    }

    @POST
    @GenerateLink(rel = "create")
    @RolesAllowed({"user", "system/admin"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @QueryParam("temporary") Boolean isTemporary, Workspace newWorkspace)
            throws OrganizationServiceException {

        String wsId = NameGenerator.generate(Workspace.class.getSimpleName(), ID_LENGTH);
        newWorkspace.setId(wsId);
        workspaceDao.create(newWorkspace);
        Workspace workspace = workspaceDao.getById(wsId);
        workspace.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                        .withName("temporary")
                                                        .withValue(String.valueOf(isTemporary))
                                                        .withDescription("Is workspace temporary")));
        final List<Link> links = new ArrayList<>();
        //todo add link to projects
        if (securityContext.isUserInRole("user")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("get all workspaces where current user is member")
                                .withHref(getServiceContext().getServiceUriBuilder().clone().path(WorkspaceService.class, "getAll").build()
                                                  .toString()));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withRel("get by id")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(getServiceContext().getServiceUriBuilder().clone().path(WorkspaceService.class, "getById")
                                                  .build(wsId).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withRel("get by name")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(getServiceContext().getServiceUriBuilder().clone().path(WorkspaceService.class, "getByName")
                                                  .queryParam("name", workspace.getName()).build().toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove")
                                .withHref(getServiceContext().getServiceUriBuilder().clone().path(WorkspaceService.class, "removeById")
                                                  .build(wsId).toString()));
        }
        return Response.status(Response.Status.CREATED).entity(workspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "workspace by id")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@PathParam("id") String id) throws OrganizationServiceException {
        Workspace workspace = workspaceDao.getById(id);
        //todo add links
        return workspace;
    }

    @GET
    @GenerateLink(rel = "workspace by name")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getByName(@QueryParam("name") String name) {
        //TODO
        Workspace workspace = null;
        return workspace;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = "update by id")
    @RolesAllowed({"system/admin", "workspace/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace updateById(@PathParam("id") String id, Workspace newWorkpsace) {
        //TODO
        Workspace workspace = null;
        return workspace;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = "all workspaces of current user")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Workspace> getAll(@Context SecurityContext securityContext) {
        //TODO
        List<Workspace> list = null;
        return list;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = "specific user workspaces")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getAllById(@QueryParam("userid") String userid) {
        //TODO
        List<Workspace> list = null;
        return list;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = "workspace members")
    @RolesAllowed("workspace/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String wsId) {
        //TODO
        List<Member> members = null;
        return members;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = "add member")
    @RolesAllowed("workspace/admin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Member addMember(@PathParam("id") String wsId, Member newMemeber) {
        //TODO
        Member member = null;
        return null;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = "remove member")
    @RolesAllowed("workspace/admin")
    public Response removeMemberById(@PathParam("id") String wsId, @PathParam("userid") String userId) {
        //TODO
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = "remove by id")
    @RolesAllowed({"system/admin", "workspace/admin"})
    public Response removeById(@PathParam("id") String wsId) {
        //TODO
        return Response.noContent().build();
    }
}
