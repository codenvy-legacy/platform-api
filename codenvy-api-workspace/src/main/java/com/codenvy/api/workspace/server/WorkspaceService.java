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


import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.user.server.UserProfileService;
import com.codenvy.api.user.server.UserService;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.MembershipException;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.server.exception.WorkspaceNotFoundException;
import com.codenvy.api.workspace.shared.dto.Membership;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.NewMembership;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.api.workspace.shared.dto.WorkspaceRef;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Path("workspace")
public class WorkspaceService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceDao    workspaceDao;
    private final UserDao         userDao;
    private final MemberDao       memberDao;
    private final UserProfileDao  userProfileDao;
    private final OrganizationDao organizationDao;

    @Inject
    public WorkspaceService(WorkspaceDao workspaceDao, UserDao userDao, MemberDao memberDao,
                            UserProfileDao userProfileDao, OrganizationDao organizationDao) {
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
        this.userProfileDao = userProfileDao;
        this.organizationDao = organizationDao;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required @Description("new workspace") Workspace newWorkspace)
            throws WorkspaceException, UserException, MembershipException, OrganizationException {
        if (newWorkspace == null) {
            throw new WorkspaceException("Missed workspace to create");
        }
        String organizationId = newWorkspace.getOrganizationId();
        Organization currentOrg;
        if (organizationId == null || organizationId.isEmpty() || (currentOrg = organizationDao.getById(organizationId)) == null) {
            throw new WorkspaceException("Incorrect organization to associate workspace with.");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        if (user == null) {
            throw new UserNotFoundException(principal.getName());
        }
        if (!currentOrg.getOwner().equals(user.getId())) {
            throw new WorkspaceException("You can only create workspace associated to your own organization.");
        }
        if (securityContext.isUserInRole("user")) {
            boolean isMultipleWorkspaceAvailable = false;
            for (int i = 0; i < currentOrg.getAttributes().size() && !isMultipleWorkspaceAvailable; ++i) {
                isMultipleWorkspaceAvailable = currentOrg.getAttributes().get(i).getName().equals("codenvy_workspace_multiple_till");
            }
            if (!isMultipleWorkspaceAvailable && workspaceDao.getByOrganization(organizationId).size() > 0) {
                throw new WorkspaceException("You have not access to create more workspaces");
            }
        }
        String wsId = NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        newWorkspace.setId(wsId);
        newWorkspace.setTemporary(false);
        workspaceDao.create(newWorkspace);

        Member member =
                DtoFactory.getInstance().createDto(Member.class)
                          .withRoles(Arrays.asList("workspace/admin", "workspace/developer")).withUserId(user.getId())
                          .withWorkspaceId(wsId);
        memberDao.create(member);
        injectLinks(newWorkspace, securityContext);

        LOG.info("EVENT#workspace-created# WS#{}# USER#{}#", newWorkspace.getName(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(newWorkspace).build();
    }

    @POST
    @Path("temp")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_TEMP_WORKSPACE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTemporary(@Context SecurityContext securityContext,
                                    @Required @Description("New temporary workspace") Workspace newWorkspace)
            throws WorkspaceException, UserException, UserProfileException, MembershipException {
        String wsId = NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        newWorkspace.setId(wsId);
        newWorkspace.setTemporary(true);
        workspaceDao.create(newWorkspace);
        final Principal principal = securityContext.getUserPrincipal();
        //temporary user should be created if real user does not exist
        User user;
        if (principal == null) {
            user = DtoFactory.getInstance().createDto(User.class)
                             .withId(NameGenerator.generate("tmp_user", com.codenvy.api.user.server.Constants.ID_LENGTH));
            userDao.create(user);
            try {
                Profile profile = DtoFactory.getInstance().createDto(Profile.class);
                profile.setId(user.getId());
                profile.setUserId(user.getId());
                profile.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(com.codenvy.api.user.shared.dto.Attribute.class)
                                                              .withName("temporary")
                                                              .withValue(String.valueOf(true))
                                                              .withDescription("Indicates user as temporary")));
                userProfileDao.create(profile);
            } catch (UserProfileException e) {
                userDao.remove(user.getId());
                throw e;
            }
        } else {
            //if user exists we don't need to create it
            user = userDao.getByAlias(principal.getName());
            if (user == null) {
                throw new UserNotFoundException(principal.getName());
            }
        }
        Member member = DtoFactory.getInstance().createDto(Member.class)
                                  .withUserId(user.getId())
                                  .withWorkspaceId(wsId)
                                  .withRoles(Arrays.asList("workspace/developer"));
        memberDao.create(member);

        LOG.info("EVENT#workspace-created# WS#{}# USER#{}#", newWorkspace.getName(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(newWorkspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_ID)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws WorkspaceException, MembershipException, UserException {
        Workspace workspace = workspaceDao.getById(id);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(id);
        }
        ensureUserHasAccessToWorkspace(id, new String[]{"workspace/admin", "workspace/developer"}, securityContext);
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getByName(@Context SecurityContext securityContext,
                               @Required @Description("workspace name") @QueryParam("name") String name)
            throws WorkspaceException, MembershipException, UserException {
        if (name == null) {
            throw new WorkspaceException("Missed parameter name");
        }
        Workspace workspace = workspaceDao.getByName(name);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(name);
        }
        ensureUserHasAccessToWorkspace(workspace.getId(), new String[]{"workspace/admin", "workspace/developer"}, securityContext);
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace update(@Context SecurityContext securityContext, @PathParam("id") String id,
                            @Required @Description("workspace to update") Workspace workspaceToUpdate)
            throws WorkspaceException, MembershipException, UserException {
        if (workspaceToUpdate == null) {
            throw new WorkspaceException("Missed workspace to update");
        }
        final Workspace workspace = workspaceDao.getById(id);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(id);
        }
        ensureUserHasAccessToWorkspace(id, new String[]{"workspace/admin"}, securityContext);
        final List<Attribute> actualAttributes = workspace.getAttributes();
        if (workspaceToUpdate.getAttributes() != null) {
            Map<String, Attribute> updates = new LinkedHashMap<>(workspaceToUpdate.getAttributes().size());
            for (Attribute attribute : workspaceToUpdate.getAttributes()) {
                updates.put(attribute.getName(), attribute);
            }
            for (Iterator<Attribute> it = actualAttributes.iterator(); it.hasNext(); ) {
                Attribute attribute = it.next();
                if (updates.containsKey(attribute.getName())) {
                    it.remove();
                }
            }
            actualAttributes.addAll(updates.values());
        }
        if (workspaceToUpdate.getName() != null && !workspaceToUpdate.getName().equals(workspace.getName()) &&
            workspaceDao.getByName(workspaceToUpdate.getName()) == null) {
            workspace.setName(workspaceToUpdate.getName());
        }
        //todo what about organizationId ? should it be possible to change organization?
        workspaceDao.update(workspace);
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @GET
    @Path("find/organization")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACES_BY_ORGANIZATION)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getWorkspacesByOrganization(@Context SecurityContext securityContext,
                                                       @Required @Description("Organization id to get workspaces")
                                                       @QueryParam("id") String organizationId)
            throws WorkspaceException, UserException, MembershipException {
        if (organizationId == null) {
            throw new WorkspaceException("Organization id required");
        }
        final List<Workspace> workspaces = new ArrayList<>();
        for (Workspace workspace : workspaceDao.getByOrganization(organizationId)) {
            injectLinks(workspace, securityContext);
            workspaces.add(workspace);
        }
        return workspaces;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Membership> getMembershipsOfCurrentUser(@Context SecurityContext securityContext)
            throws WorkspaceException, UserException, MembershipException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        if (user == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link userLink = createLink("GET",
                                         com.codenvy.api.user.server.Constants.LINK_REL_GET_CURRENT_USER,
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         getServiceContext().getBaseUriBuilder().path(UserService.class)
                                                            .path(UserService.class, "getCurrent").build().toString());
        final List<Membership> memberships = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(user.getId())) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            if (workspace == null) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(),
                                        principal.getName()));
                continue;
            }
            final Link wsLink = createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_ID, null, MediaType.APPLICATION_JSON,
                                           uriBuilder.clone().path(getClass(), "getById").build(workspace.getId()).toString());
            final WorkspaceRef wsRef = DtoFactory.getInstance().createDto(WorkspaceRef.class)
                                                 .withName(workspace.getName())
                                                 .withTemporary(workspace.isTemporary())
                                                 .withWorkspaceLink(wsLink);
            final Membership membership = DtoFactory.getInstance().createDto(Membership.class)
                                                    .withWorkspaceRef(wsRef)
                                                    .withUserLink(userLink)
                                                    .withRoles(member.getRoles());
            memberships.add(membership);
        }
        return memberships;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Membership> getMembershipsOfSpecificUser(@Context SecurityContext securityContext,
                                                         @Required @Description("user id to find workspaces")
                                                         @QueryParam("userid") String userId)
            throws WorkspaceException, UserException, MembershipException {
        if (userId == null) {
            throw new WorkspaceException("Missed parameter userid");
        }
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }

        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link userLink = createLink("GET",
                                         com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID,
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         getServiceContext().getBaseUriBuilder().path(UserService.class)
                                                            .path(UserService.class, "getById").build(userId).toString());
        final List<Membership> memberships = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(userId)) {
            Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
            if (workspace == null) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(), userId));
                continue;
            }
            final Link wsLink = createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_ID, null, MediaType.APPLICATION_JSON,
                                           uriBuilder.clone().path(getClass(), "getById").build(workspace.getId()).toString());
            final WorkspaceRef wsRef = DtoFactory.getInstance().createDto(WorkspaceRef.class)
                                                 .withName(workspace.getName())
                                                 .withTemporary(workspace.isTemporary())
                                                 .withWorkspaceLink(wsLink);
            final Membership membership = DtoFactory.getInstance().createDto(Membership.class)
                                                    .withWorkspaceRef(wsRef)
                                                    .withUserLink(userLink)
                                                    .withRoles(member.getRoles());
            memberships.add(membership);
        }
        return memberships;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_MEMBERS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws WorkspaceException, MembershipException, UserException {
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        Link self = createLink("GET", Constants.LINK_REL_GET_WORKSPACE_MEMBERS, null, MediaType.APPLICATION_JSON,
                               uriBuilder.clone().path(getClass(), "getMembers").build(wsId).toString());
        for (Member member : members) {
            Link remove = createLink("DELETE", Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, null, null,
                                     uriBuilder.clone().path(getClass(), "removeMember").build(wsId, member.getUserId()).toString());
            Link profile = createLink("GET", com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_PROFILE_BY_ID, null,
                                      MediaType.APPLICATION_JSON,
                                      getServiceContext().getBaseUriBuilder().clone().path(UserProfileService.class)
                                                         .path(UserProfileService.class, "getById").build(member.getUserId()).toString());
            member.setLinks(Arrays.asList(self, remove, profile));
        }
        return members;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void addAttribute(@PathParam("id") String wsId, @Required @Description("New attribute") Attribute newAttribute,
                             @Context SecurityContext securityContext)
            throws WorkspaceException, MembershipException, UserException {
        final Workspace workspace = workspaceDao.getById(wsId);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(wsId);
        }
        if (newAttribute == null) {
            throw new WorkspaceException("Attribute required");
        }
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        List<Attribute> attributes = workspace.getAttributes();
        removeAttribute(attributes, newAttribute.getName());
        attributes.add(newAttribute);
        workspaceDao.update(workspace);
    }

    @DELETE
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String wsId,
                                @Required @Description("The name of attribute") @QueryParam("name") String attributeName,
                                @Context SecurityContext securityContext)
            throws WorkspaceException, MembershipException, UserException {
        final Workspace workspace = workspaceDao.getById(wsId);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(wsId);
        }
        if (attributeName == null) {
            throw new WorkspaceException("Attribute name required");
        }
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        List<Attribute> attributes = workspace.getAttributes();
        removeAttribute(attributes, attributeName);
        workspaceDao.update(workspace);
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_WORKSPACE_MEMBER)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Member addMember(@PathParam("id") String wsId,
                            @Description("describes new workspace member") @Required NewMembership newMembership,
                            @Context SecurityContext securityContext)
            throws MembershipException, WorkspaceException, UserException {
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        if (newMembership == null) {
            throw new MembershipException("Missed new membership");
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class);
        newMember.setWorkspaceId(wsId);
        newMember.setRoles(newMembership.getRoles());
        newMember.setUserId(newMembership.getUserId());
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
    @RolesAllowed("user")
    public void removeMember(@PathParam("id") String wsId, @PathParam("userid") String userId, @Context SecurityContext securityContext)
            throws MembershipException, WorkspaceException, UserException {
        if (workspaceDao.getById(wsId) == null) {
            throw new WorkspaceNotFoundException(wsId);
        }
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        Member member = DtoFactory.getInstance().createDto(Member.class);
        member.setUserId(userId);
        member.setWorkspaceId(wsId);
        memberDao.remove(member);
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    public void remove(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws WorkspaceException, MembershipException, UserException {
        Workspace workspace = workspaceDao.getById(wsId);
        if (workspace == null) {
            throw new WorkspaceNotFoundException(wsId);
        }
        ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        for (Member member : members) {
            memberDao.remove(member);
        }
        workspaceDao.remove(wsId);

        LOG.info("EVENT#workspace-destroyed# WS#{}#", workspace.getName());
    }

    private void removeAttribute(List<Attribute> src, String attributeName) {
        for (Iterator<Attribute> it = src.iterator(); it.hasNext(); ) {
            Attribute current = it.next();
            if (current.getName().equals(attributeName)) {
                it.remove();
                break;
            }
        }
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
                                 uriBuilder.clone().path(getClass(), "getMembershipsOfCurrentUser").build().toString()));
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

    private void ensureUserHasAccessToWorkspace(String wsId, String[] roles, SecurityContext securityContext)
            throws WorkspaceException, UserException, MembershipException {
        if (securityContext.isUserInRole("user")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User user = userDao.getByAlias(principal.getName());
            List<Member> members = memberDao.getUserRelationships(user.getId());
            for (Member member : members) {
                if (member.getWorkspaceId().equals(wsId)) {
                    for (String role : roles) {
                        if (member.getRoles().contains(role)) {
                            return;
                        }
                    }
                }
            }
            throw new WorkspaceException("Access denied");
        }
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