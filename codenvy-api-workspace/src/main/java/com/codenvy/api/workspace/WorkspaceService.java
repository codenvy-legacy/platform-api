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
import com.codenvy.api.workspace.dao.MemberDao;
import com.codenvy.api.workspace.shared.dto.Member;
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
        Workspace workspace = workspaceDao.getById(wsId);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("list of projects")
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                              .path(ProjectService.class, "getProjects").build(wsId).toString()));
        if (securityContext.isUserInRole("user")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("get all workspaces where current user is member")
                                .withHref(uriBuilder.clone().path(getClass(), "getAll").build().toString()));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withRel("get by id")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withRel("get by name")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(uriBuilder.clone().path(getClass(), "getByName").queryParam("name", workspace.getName()).build()
                                                    .toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove")
                                .withHref(uriBuilder.clone().path(getClass(), "removeById").build(wsId).toString()));
        }
        return Response.status(Response.Status.CREATED).entity(workspace).build();
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
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("get by email")
                            .withHref(uriBuilder.clone().path(getClass(), "getByEmail").queryParam("name", workspace.getName()).build()
                                                .toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("list of projects")
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                              .path(ProjectService.class, "getProjects").build(id).toString()));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("workspace/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("POST")
                                .withRel("update")
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(uriBuilder.clone().path(getClass(), "updateById").build(workspace.getId()).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove")
                                .withHref(uriBuilder.clone().path(getClass(), "remove").build(
                                        workspace.getId()).toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("members")
                                .withHref(uriBuilder.clone().path(getClass(), "getMembers").build(workspace.getId()).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withRel("add member")
                                .withHref(uriBuilder.clone().path(getClass(), "addMember").build(workspace.getId()).toString()));
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
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("get by id")
                            .withHref(uriBuilder.clone().path(getClass(), "getByEmail").build(workspace.getId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("list of projects")
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                              .path(ProjectService.class, "getProjects").build(workspace.getId()).toString()));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("workspace/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("POST")
                                .withRel("update")
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(uriBuilder.clone().path(getClass(), "updateById").build(workspace.getId()).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove")
                                .withHref(uriBuilder.clone().path(getClass(), "remove").build(workspace.getId()).toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("members")
                                .withHref(uriBuilder.clone().path(getClass(), "getMembers").build(workspace.getId()).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withRel("add member")
                                .withHref(uriBuilder.clone().path(getClass(), "addMember").build(workspace.getId()).toString()));
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
                                @Required @Description("new workspace") Workspace newWorkspace)
            throws ApiException {
        newWorkspace.setId(id);
        workspaceDao.update(newWorkspace);
        final ArrayList<Link> links = new ArrayList<>();
        UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("DELETE")
                            .withRel("remove")
                            .withHref(uriBuilder.clone().path(getClass(), "remove")
                                                .build(id).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("list of projects")
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                              .path(ProjectService.class, "getProjects").build(newWorkspace.getId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("get by name")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(uriBuilder.clone().path(getClass(), "getByName").queryParam("name", newWorkspace.getName()).build()
                                                .toString()));
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("members")
                                .withHref(uriBuilder.clone().path(getClass(), "getMembers").build(id).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withRel("add member")
                                .withHref(uriBuilder.clone().path(getClass(), "addMember").build(id).toString()));
        }
        newWorkspace.setLinks(links);
        return newWorkspace;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = "all workspaces of current user")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Workspace> getAll(@Context SecurityContext securityContext) throws ApiException {
        final User current = userDao.getByAlias(securityContext.getUserPrincipal().getName());
        final List<Workspace> workspaces = new ArrayList<>();
        final List<Link> links = new ArrayList<>(1);
        for (Member member : memberDao.getUserRelationships(current.getId())) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            Link projects = DtoFactory.getInstance().createDto(Link.class)
                                      .withProduces(MediaType.APPLICATION_JSON)
                                      .withMethod("GET")
                                      .withRel("workspace projects")
                                      .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                        .path(ProjectService.class, "getProjects").build(workspace.getId()).toString());
            workspace.setLinks(Arrays.asList(projects));
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
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withRel("user by id")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withHref(uriBuilder.clone().path(UserService.class).path(UserService.class, "getById").build(userid)
                                                    .toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("list of projects")
                                .withHref(getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
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
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("workspace")
                            .withHref(uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
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
    public Member addMember(@PathParam("id") String wsId, Member newMember) throws ApiException {
        newMember.setWorkspaceId(wsId);
        memberDao.create(newMember);
        final List<Link> links = new ArrayList<>(3);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("workspace")
                            .withHref(uriBuilder.clone().path(getClass(), "getById").build(wsId).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("members")
                            .withHref(uriBuilder.clone().path(getClass(), "getMembers").build(wsId).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("DELETE")
                            .withRel("remove member")
                            .withHref(uriBuilder.clone().path(getClass(), "removeMemberById").build(newMember.getUserId()).toString()));
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
}
