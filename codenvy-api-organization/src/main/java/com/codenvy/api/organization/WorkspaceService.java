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


import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.organization.dao.MemberDao;
import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.WorkspaceDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Workspace;

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
import java.util.List;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 */
@Path("/workspace")
public class WorkspaceService {

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
    public Response create() throws OrganizationServiceException {
        //TODO
        Workspace workspace = null;
        return Response.status(Response.Status.CREATED).entity(workspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "workspace by id")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@PathParam("id") String id) {
        //TODO
        Workspace workspace = null;
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
    public List<Workspace> getCurrentUserWorkspaces(@Context SecurityContext securityContext) {
        //TODO
        List<Workspace> list = null;
        return list;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = "specific user workspaces")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getSpecificUserWorkspaces(@QueryParam("userid") String userid) {
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
