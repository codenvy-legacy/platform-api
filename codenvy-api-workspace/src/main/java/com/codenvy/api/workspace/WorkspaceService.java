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
package com.codenvy.api.workspace;


import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.user.UserService;
import com.codenvy.api.user.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.user.dao.MemberDao;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.workspace.shared.dto.Membership;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.api.workspace.dao.WorkspaceDao;

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
import javax.ws.rs.core.UriBuilder;
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
    public Response create(@Context SecurityContext securityContext, @Required @Description("new workspace") Workspace newWorkspace)
            throws ApiException {
        String wsId = NameGenerator.generate(Workspace.class.getSimpleName(), ID_LENGTH);
        newWorkspace.setId(wsId);
        workspaceDao.create(newWorkspace);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                             getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                .path(ProjectService.class, "getProjects").build(wsId).toString()));
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET", "current user workspaces", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getAll").build().toString()));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink("GET", "get by id", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
            links.add(createLink("GET", "get by name", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", newWorkspace.getName()).build()
                                           .toString()));
            links.add(createLink("DELETE", "remove", null, null, uriBuilder.clone().path(getClass(), "removeById").build(wsId).toString()));
        }
        newWorkspace.setLinks(links);
        return Response.status(Response.Status.CREATED).entity(newWorkspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "workspace by id")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@Context SecurityContext securityContext, @PathParam("id") String id) throws ApiException {
        Workspace workspace = workspaceDao.getById(id);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", "get by name", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getByName").queryParam("name", workspace.getName()).build().toString()));
        links.add(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                             getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                .path(ProjectService.class, "getProjects").build(id).toString()));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("POST", "update", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updateById").build(workspace.getId()).toString()));
            links.add(createLink("DELETE", "remove", null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(workspace.getId()).toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("GET", "members", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembers").build(workspace.getId()).toString()));
            links.add(createLink("POST", "add member", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "addMember").build(workspace.getId()).toString()));
        }
        workspace.setLinks(links);
        return workspace;
    }

    @GET
    @GenerateLink(rel = "workspace by name")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getByName(@Context SecurityContext securityContext,
                               @Required @Description("workspace name") @QueryParam("name") String name)
            throws ApiException {
        Workspace workspace = workspaceDao.getByName(name);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", "get by id", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getById").build(workspace.getId()).toString()));
        links.add(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                             getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                .path(ProjectService.class, "getProjects").build(workspace.getId()).toString()));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("POST", "update", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updateById").build(workspace.getId()).toString()));
            links.add(createLink("DELETE", "remove", null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(workspace.getId()).toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("GET", "members", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembers").build(workspace.getId()).toString()));
            links.add(createLink("POST", "add member", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "addMember").build(workspace.getId()).toString()));
        }
        workspace.setLinks(links);
        return workspace;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = "update by id")
    @RolesAllowed({"system/admin", "workspace/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace updateById(@Context SecurityContext securityContext, @PathParam("id") String id,
                                @Required @Description("workspace to update") Workspace workspaceToUpdate)
            throws ApiException {
        workspaceToUpdate.setId(id);
        workspaceDao.update(workspaceToUpdate);
        final ArrayList<Link> links = new ArrayList<>();
        UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("DELETE", "remove", null, null,
                             uriBuilder.clone().path(getClass(), "remove").build(id).toString()));
        links.add(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                             getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                .path(ProjectService.class, "getProjects").build(id).toString()));
        links.add(createLink("GET", "get by name", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getByName").queryParam("name", workspaceToUpdate.getName()).build()
                                       .toString()));
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("GET", "members", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembers").build(workspaceToUpdate.getId()).toString()));
            links.add(createLink("POST", "add member", MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "addMember").build(workspaceToUpdate.getId()).toString()));
        }
        workspaceToUpdate.setLinks(links);
        return workspaceToUpdate;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = "all workspaces")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Workspace> getAll(@Context SecurityContext securityContext) throws ApiException {
        final User current = userDao.getByAlias(securityContext.getUserPrincipal().getName());
        final List<Workspace> workspaces = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(current.getId())) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            workspace.setLinks(Arrays.asList(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                                                        getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                                           .path(ProjectService.class, "getProjects")
                                                                           .build(member.getWorkspaceId()).toString())));
            workspaces.add(workspace);
        }
        return workspaces;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = "specific user workspaces")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getAllById(@Required @Description("user id to find workspaces") @QueryParam("userid") String userid)
            throws ApiException {
        final List<Workspace> workspaces = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        for (Member member : memberDao.getUserRelationships(userid)) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            final List<Link> links = new ArrayList<>(2);
            links.add(createLink("GET", "user by id", null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(UserService.class).path(UserService.class, "getById").build(userid).toString()));
            links.add(createLink("GET", "list of projects", null, MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                    .path(ProjectService.class, "getProjects").build(workspace.getId()).toString()));
            workspace.setLinks(links);
            workspaces.add(workspace);
        }
        return workspaces;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = "workspace members")
    @RolesAllowed("workspace/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String wsId) throws ApiException {
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final List<Link> links = new ArrayList<>(1);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", "get by id", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
        for (Member member : members) {
            member.setLinks(links);
        }
        return members;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = "add member")
    @RolesAllowed("workspace/admin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Member addMember(@PathParam("id") String wsId, Membership membership) throws ApiException {
        Member newMember = DtoFactory.getInstance().createDto(Member.class);
        newMember.setWorkspaceId(wsId);
        newMember.setRoles(membership.getRoles());
        newMember.setUserId(membership.getUserId());
        memberDao.create(newMember);
        final List<Link> links = new ArrayList<>(3);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", "get by id", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
        links.add(createLink("GET", "members", null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMembers").build(wsId).toString()));
        links.add(createLink("DELETE", "remove member", null, null,
                             uriBuilder.clone().path(getClass(), "removeMemberById").build(newMember.getUserId()).toString()));
        newMember.setLinks(links);
        return newMember;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = "remove member")
    @RolesAllowed("workspace/admin")
    public Response removeMemberById(@PathParam("id") String wsId, @PathParam("userid") String userId) {
        memberDao.removeWorkspaceMember(wsId, userId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = "remove by id")
    @RolesAllowed({"system/admin", "workspace/admin"})
    public Response removeById(@PathParam("id") String wsId) throws ApiException {
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        for (Member member : members) {
            memberDao.removeWorkspaceMember(wsId, member.getUserId());
        }
        workspaceDao.remove(wsId);
        return Response.noContent().build();
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }
}
