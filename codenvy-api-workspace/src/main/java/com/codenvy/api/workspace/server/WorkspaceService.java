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
package com.codenvy.api.workspace.server;


import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.MembershipException;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.server.exception.WorkspaceNotFoundException;
import com.codenvy.api.workspace.shared.dto.Membership;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.security.Principal;
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
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

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
    @GenerateLink(rel = Constants.LINK_REL_CREATE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @Required @Description("new workspace") Workspace newWorkspace)
            throws WorkspaceException {
        if (newWorkspace == null) {
            throw new WorkspaceException("Missed workspace to create");
        }
//        String wsId = NameGenerator.generate(Workspace.class.getSimpleName(), Constants.ID_LENGTH);
//        newWorkspace.setId(wsId);
        workspaceDao.create(newWorkspace);
        injectLinks(newWorkspace, securityContext);
        return Response.status(Response.Status.CREATED).entity(newWorkspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_ID)
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@Context SecurityContext securityContext, @PathParam("id") String id) throws WorkspaceException {
        Workspace workspace = workspaceDao.getById(id);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(id);
        }
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getByName(@Context SecurityContext securityContext,
                               @Required @Description("workspace name") @QueryParam("name") String name) throws WorkspaceException {
        if (name == null) {
            throw new WorkspaceException("Missed parameter name");
        }
        Workspace workspace = workspaceDao.getByName(name);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(name);
        }
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID)
    @RolesAllowed({"system/admin", "workspace/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace update(@Context SecurityContext securityContext, @PathParam("id") String id,
                            @Required @Description("workspace to update") Workspace workspaceToUpdate) throws WorkspaceException {
        if (workspaceToUpdate == null) {
            throw new WorkspaceException("Missed workspace to update");
        }
        if (workspaceDao.getById(id) == null) {
            throw new WorkspaceNotFoundException(id);
        }
        workspaceToUpdate.setId(id);
        workspaceDao.update(workspaceToUpdate);
        injectLinks(workspaceToUpdate, securityContext);
        return workspaceToUpdate;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getWorkspacesOfCurrentUser(@Context SecurityContext securityContext)
            throws WorkspaceException, UserException, MembershipException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        if (user == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final List<Workspace> workspaces = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(user.getId())) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            if (workspace == null) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(),
                                        principal.getName()));
                continue;
            }
            injectLinks(workspace, securityContext);
            workspaces.add(workspace);
        }
        return workspaces;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getWorkspacesOfConcreteUser(@Context SecurityContext securityContext,
                                                       @Required @Description("user id to find workspaces")
                                                       @QueryParam("userid") String userId)
            throws WorkspaceException, UserException, MembershipException {
        if (userId == null) {
            throw new WorkspaceException("Missed parameter userid");
        }
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        final List<Workspace> workspaces = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(userId)) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            if (workspace == null) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(), userId));
                continue;
            }
            injectLinks(workspace, securityContext);
            workspaces.add(workspace);
        }
        return workspaces;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_MEMBERS)
    @RolesAllowed("workspace/admin")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String wsId) throws WorkspaceException, MembershipException {
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        Link self = createLink("GET", Constants.LINK_REL_GET_WORKSPACE_MEMBERS, null, MediaType.APPLICATION_JSON,
                               uriBuilder.clone().path(getClass(), "getMembers").build(wsId).toString());
        for (Member member : members) {
            Link remove = createLink("DELETE", Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, null, null,
                                     uriBuilder.clone().path(getClass(), "removeMember").build(wsId, member.getUserId()).toString());
            member.setLinks(Arrays.asList(self, remove));
        }
        return members;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_WORKSPACE_MEMBER)
    @RolesAllowed("workspace/admin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Member addMember(@PathParam("id") String wsId,
                            @Description("describes new workspace member") @Required Membership membership) throws MembershipException {
        if (membership == null) {
            throw new MembershipException("Missed membership");
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class);
        newMember.setWorkspaceId(wsId);
        newMember.setRoles(membership.getRoles());
        newMember.setUserId(membership.getUserId());
        memberDao.create(newMember);
        final List<Link> links = new ArrayList<>(2);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_MEMBERS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMembers").build(wsId).toString()));
        links.add(createLink("DELETE", Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, null, null,
                             uriBuilder.clone().path(getClass(), "removeMember").build(wsId, newMember.getUserId()).toString()));
        newMember.setLinks(links);
        return newMember;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER)
    @RolesAllowed("workspace/admin")
    public void removeMember(@PathParam("id") String wsId, @PathParam("userid") String userId) throws MembershipException {
        Member member = DtoFactory.getInstance().createDto(Member.class);
        member.setUserId(userId);
        member.setWorkspaceId(wsId);
        memberDao.remove(member);
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE)
    @RolesAllowed({"system/admin", "workspace/admin"})
    public void remove(@PathParam("id") String wsId) throws WorkspaceException, MembershipException {
        if (workspaceDao.getById(wsId) == null) {
            throw new WorkspaceNotFoundException(wsId);
        }
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        for (Member member : members) {
            memberDao.remove(member);
        }
        workspaceDao.remove(wsId);
    }

    private void injectLinks(Workspace workspace, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET", com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS, null, MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().clone().path(ProjectService.class)
                                                    .path(ProjectService.class, "getProjects")
                                                    .build(workspace.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getWorkspacesOfCurrentUser").build().toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembers").build(workspace.getId()).toString()));
            links.add(createLink("POST", Constants.LINK_REL_ADD_WORKSPACE_MEMBER, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "addMember").build(workspace.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withName("member")
                                                                      .withDescription("new member")
                                                                      .withRequired(true)
                                                                      .withType(ParameterType.Object))));
        }
        if (securityContext.isUserInRole("workspace/admin") || securityContext.isUserInRole("workspace/developer") ||
            securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", workspace.getName()).build()
                                           .toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(workspace.getId()).toString()));
        }
        if (securityContext.isUserInRole("workspace/admin") || securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE", Constants.LINK_REL_REMOVE_WORKSPACE, null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(workspace.getId()).toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "update").build(workspace.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withName("workspaceToUpdate")
                                                                      .withDescription("workspace to update")
                                                                      .withRequired(true)
                                                                      .withType(ParameterType.Object))));
        }
        workspace.setLinks(links);
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