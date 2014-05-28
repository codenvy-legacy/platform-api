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


import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
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
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.Membership;
import com.codenvy.api.workspace.shared.dto.NewMembership;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.api.workspace.shared.dto.WorkspaceRef;
import com.codenvy.commons.env.EnvironmentContext;
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
import java.util.Collections;
import java.util.HashMap;
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

    private final WorkspaceDao   workspaceDao;
    private final UserDao        userDao;
    private final MemberDao      memberDao;
    private final UserProfileDao userProfileDao;
    private final AccountDao     accountDao;

    @Inject
    public WorkspaceService(WorkspaceDao workspaceDao, UserDao userDao, MemberDao memberDao,
                            UserProfileDao userProfileDao, AccountDao accountDao) {
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
        this.userProfileDao = userProfileDao;
        this.accountDao = accountDao;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required @Description("new workspace") Workspace newWorkspace)
            throws ConflictException, NotFoundException, ServerException, ForbiddenException {
        if (newWorkspace == null) {
            throw new ConflictException("Missed workspace to create");
        }
        String accountId = newWorkspace.getAccountId();
        Account actualAcc;
        if (accountId == null || accountId.isEmpty() || (actualAcc = accountDao.getById(accountId)) == null) {
            throw new ConflictException("Incorrect account to associate workspace with");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        List<Account> accounts = accountDao.getByOwner(user.getId());
        boolean isCurrentUserAccountOwner = false;
        for (int i = 0; i < accounts.size() && !isCurrentUserAccountOwner; ++i) {
            isCurrentUserAccountOwner = accounts.get(i).getId().equals(actualAcc.getId());
        }
        if (!isCurrentUserAccountOwner) {
            throw new ConflictException("You can create workspace associated only to your own account");
        }
        if (securityContext.isUserInRole("user")) {
            boolean isMultipleWorkspaceAvailable = false;
            for (int i = 0; i < actualAcc.getAttributes().size() && !isMultipleWorkspaceAvailable; ++i) {
                isMultipleWorkspaceAvailable = actualAcc.getAttributes().get(i).getName().equals("codenvy_workspace_multiple_till");
            }
            if (!isMultipleWorkspaceAvailable && workspaceDao.getByAccount(accountId).size() > 0) {
                throw new ForbiddenException("You have not access to create more workspaces");
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
    @SuppressWarnings("static-access")
    public Response createTemporary(@Context SecurityContext securityContext,
                                    @Required @Description("New temporary workspace") Workspace newWorkspace)
            throws ConflictException, NotFoundException, ServerException {
        String wsId = NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        newWorkspace.setId(wsId);
        newWorkspace.setTemporary(true);
        try {
            //let vfs create temporary workspace in correct place
            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
            workspaceDao.create(newWorkspace);
        } finally {
            EnvironmentContext.getCurrent().reset();
        }
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
            } catch (ApiException e) {
                userDao.remove(user.getId());
                throw e;
            }
        } else {
            user = userDao.getByAlias(principal.getName());
        }
        Member member = DtoFactory.getInstance().createDto(Member.class)
                                  .withUserId(user.getId())
                                  .withWorkspaceId(wsId)
                                  .withRoles(Arrays.asList("workspace/developer", "workspace/admin"));
        memberDao.create(member);

        LOG.info("EVENT#workspace-created# WS#{}# USER#{}#", newWorkspace.getName(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(newWorkspace).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws NotFoundException, ServerException, ForbiddenException {
        Workspace workspace = workspaceDao.getById(id);
        try {
            ensureUserHasAccessToWorkspace(workspace.getId(), new String[]{"workspace/admin", "workspace/developer"}, securityContext);
        } catch (ForbiddenException e) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            List<Attribute> safeAttributes = new ArrayList<>(1);
            for (Attribute attribute : workspace.getAttributes()) {
                if (attribute.getName().equals("allowAnyoneAddMember")) {
                    safeAttributes.add(attribute);
                }
            }
            workspace.setAttributes(safeAttributes.size() > 0 ? safeAttributes : Collections.<Attribute>emptyList());
        }
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getByName(@Context SecurityContext securityContext,
                               @Required @Description("workspace name") @QueryParam("name") String name)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (name == null) {
            throw new ConflictException("Missed parameter name");
        }
        Workspace workspace = workspaceDao.getByName(name);
        try {
            ensureUserHasAccessToWorkspace(workspace.getId(), new String[]{"workspace/admin", "workspace/developer"}, securityContext);
        } catch (ForbiddenException e) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            List<Attribute> safeAttributes = new ArrayList<>(1);
            for (Attribute attribute : workspace.getAttributes()) {
                if (attribute.getName().equals("allowAnyoneAddMember")) {
                    safeAttributes.add(attribute);
                }
            }
            workspace.setAttributes(safeAttributes.size() > 0 ? safeAttributes : Collections.<Attribute>emptyList());
        }
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID)
    @RolesAllowed({"workspace/admin", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace update(@Context SecurityContext securityContext, @PathParam("id") String id,
                            @Required @Description("workspace to update") Workspace workspaceToUpdate)
            throws NotFoundException, ConflictException, ServerException, ForbiddenException {
        if (workspaceToUpdate == null) {
            throw new ConflictException("Missed workspace to update");
        }
        final Workspace workspace = workspaceDao.getById(id);
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
        //todo what about accountId ? should it be possible to change account?
        workspaceDao.update(workspace);
        injectLinks(workspace, securityContext);
        return workspace;
    }

    @GET
    @Path("find/account")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACES_BY_ACCOUNT)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Workspace> getWorkspacesByAccount(@Context SecurityContext securityContext,
                                                  @Required @Description("Account id to get workspaces")
                                                  @QueryParam("id") String accountId)
            throws NotFoundException, ConflictException, ServerException {
        if (accountId == null) {
            throw new ConflictException("Account id required");
        }
        final List<Workspace> workspaces = new ArrayList<>();
        for (Workspace workspace : workspaceDao.getByAccount(accountId)) {
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
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link userLink = createLink("GET",
                                         com.codenvy.api.user.server.Constants.LINK_REL_GET_CURRENT_USER,
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         getServiceContext().getBaseUriBuilder().path(UserService.class)
                                                            .path(UserService.class, "getCurrent").build().toString()
                                        );
        final List<Membership> memberships = new ArrayList<>();
        for (Member member : memberDao.getUserRelationships(user.getId())) {
            Workspace workspace;
            try {
                workspace = workspaceDao.getById(member.getWorkspaceId());
            } catch (NotFoundException ex) {
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
            throws NotFoundException, ConflictException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed parameter userid");
        }
        userDao.getById(userId);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link userLink = createLink("GET",
                                         com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID,
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         getServiceContext().getBaseUriBuilder().path(UserService.class)
                                                            .path(UserService.class, "getById").build(userId).toString()
                                        );
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
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException {
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
                                                         .path(UserProfileService.class, "getById").build(member.getUserId()).toString()
                                     );
            member.setLinks(Arrays.asList(self, remove, profile));
        }
        return members;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    public void addAttribute(@PathParam("id") String wsId, @Required @Description("New attribute") Attribute newAttribute,
                             @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        final Workspace workspace = workspaceDao.getById(wsId);
        if (newAttribute == null) {
            throw new ConflictException("Attribute required");
        }
        List<Attribute> attributes = workspace.getAttributes();
        removeAttribute(attributes, newAttribute.getName());
        attributes.add(newAttribute);
        workspaceDao.update(workspace);
    }

    @DELETE
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTE)
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String wsId,
                                @Required @Description("The name of attribute") @QueryParam("name") String attributeName,
                                @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        final Workspace workspace = workspaceDao.getById(wsId);
        if (attributeName == null) {
            throw new ConflictException("Attribute name required");
        }
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
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (newMembership == null) {
            throw new ConflictException("Missed new membership");
        }
        if (newMembership.getRoles().isEmpty()) {
            throw new ConflictException("Roles required");
        }
        Workspace workspace = workspaceDao.getById(wsId);

        final Map<String, String> attributes = new HashMap<>(workspace.getAttributes().size());
        for (Attribute attribute : workspace.getAttributes()) {
            attributes.put(attribute.getName(), attribute.getValue());
        }
        if (!"true".equalsIgnoreCase(attributes.get("allowAnyoneAddMember"))) {
            ensureUserHasAccessToWorkspace(wsId, new String[]{"workspace/admin"}, securityContext);
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
    @RolesAllowed("workspace/admin")
    public void removeMember(@PathParam("id") String wsId, @PathParam("userid") String userId, @Context SecurityContext
            securityContext)
            throws NotFoundException, ServerException, ForbiddenException, ConflictException {
        workspaceDao.getById(wsId);
        List<Member> wsMembers = memberDao.getWorkspaceMembers(wsId);
        Member toRemove = null;
        //search for member
        for (Iterator<Member> mIt = wsMembers.iterator(); mIt.hasNext() && toRemove == null; ) {
            Member current = mIt.next();
            if (current.getUserId().equals(userId)) {
                toRemove = current;
            }
        }
        if (toRemove != null) {
            //workspace should have at least 1 admin
            if (toRemove.getRoles().contains("workspace/admin")) {
                boolean isOtherWsAdminPresent = false;
                for (Iterator<Member> mIt = wsMembers.iterator();
                     mIt.hasNext() && !isOtherWsAdminPresent; ) {
                    Member current = mIt.next();
                    isOtherWsAdminPresent = !current.getUserId().equals(toRemove.getUserId())
                                            && current.getRoles().contains("workspace/admin");
                }
                if (!isOtherWsAdminPresent) {
                    throw new ConflictException("Workspace should have at least 1 admin");
                }
            }
            memberDao.remove(toRemove);
        } else {
            throw new NotFoundException(String.format("User %s doesn't have membership with workspace %s", userId, wsId));
        }
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE)
    @RolesAllowed({"workspace/admin", "system/admin"})
    public void remove(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException, ConflictException {
        Workspace workspace = workspaceDao.getById(wsId);
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
                                                    .build(workspace.getId()).toString()
                                ));
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
            links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", workspace.getName()).build()
                                           .toString()
                                ));
            links.add(createLink("GET", Constants.LINK_REL_GET_WORKSPACE_BY_ID, null, MediaType.APPLICATION_JSON,
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
            throws NotFoundException, ServerException, ForbiddenException {
        if (securityContext.isUserInRole("user")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User user = userDao.getByAlias(principal.getName());
            final List<Member> members = memberDao.getUserRelationships(user.getId());
            for (Member member : members) {
                if (member.getWorkspaceId().equals(wsId)) {
                    for (String role : roles) {
                        if (member.getRoles().contains(role)) {
                            return;
                        }
                    }
                }
            }
            throw new ForbiddenException("Access denied");
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