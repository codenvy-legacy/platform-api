/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.workspace.server;


import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.user.server.UserService;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.MemberDescriptor;
import com.codenvy.api.workspace.shared.dto.NewMembership;
import com.codenvy.api.workspace.shared.dto.NewWorkspace;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.api.workspace.shared.dto.WorkspaceReference;
import com.codenvy.api.workspace.shared.dto.WorkspaceUpdate;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Api(value = "/workspace",
     description = "Workspace manager")
@Path("workspace")
public class WorkspaceService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceDao   workspaceDao;
    private final UserDao        userDao;
    private final MemberDao      memberDao;
    private final UserProfileDao userProfileDao;
    private final AccountDao     accountDao;
    private final boolean        isOrgAddonEnabledByDefault;

    @Inject
    public WorkspaceService(WorkspaceDao workspaceDao,
                            UserDao userDao,
                            MemberDao memberDao,
                            UserProfileDao userProfileDao,
                            AccountDao accountDao,
                            @Named("subscription.orgaddon.enabled") String isOrgAddonEnabledByDefault) {
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
        this.userProfileDao = userProfileDao;
        this.accountDao = accountDao;
        this.isOrgAddonEnabledByDefault = Boolean.parseBoolean(isOrgAddonEnabledByDefault);
    }

    /**
     * Creates new workspace and adds current user as member to created workspace
     * with roles <i>"workspace/admin"</i> and <i>"workspace/developer"</i>. Returns status code <strong>201 CREATED</strong>
     * and {@link WorkspaceDescriptor} if workspace has been created successfully.
     * Each new workspace should contain at least name and account identifier.
     *
     * @param newWorkspace
     *         new workspace
     * @return descriptor of created workspace
     * @throws ConflictException
     *         when current user account identifier and given account identifier are different
     * @throws NotFoundException
     *         when account with given identifier does not exist
     * @throws ServerException
     *         when some error occurred while retrieving/persisting account, workspace or member
     * @throws ForbiddenException
     *         when user has not access to create workspaces,
     *         or when new workspace is {@code null},
     *         or any of workspace name or account id is {@code null}
     * @see NewWorkspace
     * @see WorkspaceDescriptor
     * @see #getById(String, SecurityContext)
     * @see #getByName(String, SecurityContext)
     */
    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Required @Description("new workspace") NewWorkspace newWorkspace,
                           @Context SecurityContext securityContext) throws ConflictException,
                                                                            NotFoundException,
                                                                            ServerException,
                                                                            ForbiddenException {
        requiredNotNull(newWorkspace, "New workspace");
        requiredNotNull(newWorkspace.getAccountId(), "Account identifier");
        requiredNotNull(newWorkspace.getName(), "Workspace name");
        if (newWorkspace.getAttributes() != null) {
            for (String attributeName : newWorkspace.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        //check current user is account owner
        final List<Account> accounts = accountDao.getByOwner(user.getId());
        final String accountId = newWorkspace.getAccountId();
        final Account actualAccount = accountDao.getById(accountId);
        boolean isCurrentUserAccountOwner = false;
        for (int i = 0; i < accounts.size() && !isCurrentUserAccountOwner; ++i) {
            isCurrentUserAccountOwner = accounts.get(i).getId().equals(actualAccount.getId());
        }
        if (!isCurrentUserAccountOwner) {
            throw new ConflictException("You can create workspace associated only to your own account");
        }
        if (!isOrgAddonEnabledByDefault && securityContext.isUserInRole("user")) {
            if (!"true".equals(actualAccount.getAttributes().get("codenvy:multi-ws")) &&
                workspaceDao.getByAccount(accountId).size() > 0) {
                throw new ForbiddenException("You have not access to create more workspaces");
            }
        }
        final String wsId = NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final Workspace workspace = new Workspace().withId(wsId)
                                                   .withName(newWorkspace.getName())
                                                   .withTemporary(false)
                                                   .withAccountId(accountId)
                                                   .withAttributes(newWorkspace.getAttributes());
        final Member member = new Member().withUserId(user.getId())
                                          .withWorkspaceId(wsId)
                                          .withRoles(Arrays.asList("workspace/admin", "workspace/developer"));
        workspaceDao.create(workspace);
        memberDao.create(member);

        final WorkspaceDescriptor workspaceDescriptor = toDescriptor(workspace, securityContext);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", newWorkspace.getName(), workspace.getId(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(workspaceDescriptor).build();
    }

    /**
     * Creates new temporary workspace and adds current user
     * as member to created workspace with roles <i>"workspace/admin"</i> and <i>"workspace/developer"</i>.
     * If user does not exist, it will be created with role <i>"tmp_user"</i>.
     * Returns status code <strong>201 CREATED</strong> and {@link WorkspaceDescriptor} if workspace
     * has been created successfully. Each new workspace should contain
     * at least workspace name and account identifier.
     *
     * @param newWorkspace
     *         new workspace
     * @return descriptor of created workspace
     * @throws ConflictException
     *         when current user account identifier and given account identifier are different
     * @throws ForbiddenException
     *         when new workspace is {@code null},
     *         or any of workspace name or account identifier is {@code null}
     * @throws NotFoundException
     *         when account with given identifier does not exist
     * @throws ServerException
     *         when some error occurred while retrieving/persisting account, workspace, member or profile
     * @see WorkspaceDescriptor
     * @see #getById(String, SecurityContext)
     * @see #getByName(String, SecurityContext)
     */
    @POST
    @Path("temp")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_TEMP_WORKSPACE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("static-access")
    public Response createTemporary(@Required @Description("New temporary workspace") NewWorkspace newWorkspace,
                                    @Context SecurityContext securityContext) throws ConflictException,
                                                                                     NotFoundException,
                                                                                     ForbiddenException,
                                                                                     ServerException {
        requiredNotNull(newWorkspace, "New workspace");
        if (newWorkspace.getAttributes() != null) {
            for (String attributeName : newWorkspace.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
        }
        final String wsId = NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final Workspace workspace = new Workspace().withId(wsId)
                                                   .withName(newWorkspace.getName())
                                                   .withTemporary(true)
                                                   .withAccountId(newWorkspace.getAccountId())
                                                   .withAttributes(newWorkspace.getAttributes());
        try {
            //let vfs create temporary workspace in correct place
            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
            workspaceDao.create(workspace);
        } finally {
            EnvironmentContext.getCurrent().setWorkspaceTemporary(false);
        }
        final Principal principal = securityContext.getUserPrincipal();
        //temporary user should be created if real user does not exist
        User user;
        if (principal == null) {
            user = DtoFactory.getInstance().createDto(User.class)
                             .withId(NameGenerator.generate("tmp_user", com.codenvy.api.user.server.Constants.ID_LENGTH));
            userDao.create(user);
            try {
                final Map<String, String> attributes = new HashMap<>(4);
                attributes.put("temporary", String.valueOf(true));
                attributes.put("codenvy:created", Long.toString(System.currentTimeMillis()));
                userProfileDao.create(new Profile().withId(user.getId())
                                                   .withUserId(user.getId())
                                                   .withAttributes(attributes));
            } catch (ApiException e) {
                userDao.remove(user.getId());
                throw e;
            }
        } else {
            user = userDao.getByAlias(principal.getName());
        }
        final Member member = new Member().withUserId(user.getId())
                                          .withWorkspaceId(wsId)
                                          .withRoles(Arrays.asList("workspace/developer", "workspace/admin"));
        memberDao.create(member);

        final WorkspaceDescriptor workspaceDescriptor = toDescriptor(workspace, securityContext);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(workspaceDescriptor).build();
    }

    /**
     * Searches for workspace with given identifier and returns {@link WorkspaceDescriptor} if found.
     * If user that has called this method is not <i>"workspace/admin"</i> or <i>"workspace/developer"</i>
     * workspace attributes will not be added to response.
     *
     * @param id
     *         workspace identifier
     * @return descriptor of found workspace
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving workspace
     * @see WorkspaceDescriptor
     * @see #getByName(String, SecurityContext)
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor getById(@PathParam("id") String id,
                                       @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Workspace workspace = workspaceDao.getById(id);
        try {
            ensureUserHasAccessToWorkspace(securityContext, workspace.getId(), "workspace/admin", "workspace/developer");
        } catch (ForbiddenException e) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            final Map<String, String> attributes = workspace.getAttributes();
            if (attributes.containsKey("allowAnyoneAddMember")) {
                workspace.setAttributes(Collections.singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
            } else {
                workspace.setAttributes(Collections.<String, String>emptyMap());
            }
        }
        return toDescriptor(workspace, securityContext);
    }

    /**
     * Searches for workspace with given name and return {@link WorkspaceDescriptor} for it.
     * If user that has called this method is not <i>"workspace/admin"</i> or <i>"workspace/developer"</i>
     * workspace attributes will not be added to response.
     *
     * @param name
     *         workspace name
     * @return descriptor of found workspace
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving workspace
     * @see WorkspaceDescriptor
     * @see #getById(String, SecurityContext)
     */
    @ApiOperation(value = "Gets workspace by name",
                  response = WorkspaceDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Workspace with specified name doesn't exist"),
            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor getByName(@ApiParam(value = "Name of workspace", required = true)
                                         @Required
                                         @Description("workspace name")
                                         @QueryParam("name") String name,
                                         @Context SecurityContext securityContext) throws NotFoundException,
                                                                                          ServerException,
                                                                                          ForbiddenException {
        requiredNotNull(name, "Workspace name");
        final Workspace workspace = workspaceDao.getByName(name);
        try {
            ensureUserHasAccessToWorkspace(securityContext, workspace.getId(), "workspace/admin", "workspace/developer");
        } catch (ForbiddenException e) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            final Map<String, String> attributes = workspace.getAttributes();
            if (attributes.containsKey("allowAnyoneAddMember")) {
                workspace.setAttributes(Collections.singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
            } else {
                workspace.setAttributes(Collections.<String, String>emptyMap());
            }
        }
        return toDescriptor(workspace, securityContext);
    }

    /**
     * <p>Updates workspace.</p>
     * <strong>Note:</strong> existed workspace attributes with same name as
     * update attributes will be replaced with update attributes.
     *
     * @param id
     *         workspace identifier
     * @param update
     *         workspace update
     * @return descriptor of updated workspace
     * @throws NotFoundException
     *         when workspace with given name doesn't exist
     * @throws ConflictException
     *         when attribute with not valid name
     * @throws ForbiddenException
     *         when update is {@code null} or updated attributes contains
     * @throws ServerException
     *         when some error occurred while retrieving/updating workspace
     * @see WorkspaceUpdate
     * @see WorkspaceDescriptor
     * @see #removeAttribute(String, String, SecurityContext)
     */
    @POST
    @Path("{id}")
    @RolesAllowed({"workspace/admin", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor update(@Context SecurityContext securityContext,
                                      @PathParam("id") String id,
                                      WorkspaceUpdate update) throws NotFoundException,
                                                                     ConflictException,
                                                                     ForbiddenException,
                                                                     ServerException {
        requiredNotNull(update, "Workspace update");
        final Workspace workspace = workspaceDao.getById(id);
        if (update.getAttributes() != null) {
            for (String attributeName : update.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
            workspace.getAttributes().putAll(update.getAttributes());
        }
        if (update.getName() != null && !update.getName().equals(workspace.getName()) &&
            workspaceDao.getByName(update.getName()) == null) {
            workspace.setName(update.getName());
        }
        workspaceDao.update(workspace);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
        return toDescriptor(workspace, securityContext);
    }

    /**
     * Returns workspace descriptors for certain workspaces with given account identifier.
     *
     * @param accountId
     *         account identifier
     * @return workspaces descriptors
     * @throws ForbiddenException
     *         when account identifier is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving workspace
     * @see WorkspaceDescriptor
     */
    @GET
    @Path("find/account")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACES_BY_ACCOUNT)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkspaceDescriptor> getWorkspacesByAccount(@Context SecurityContext securityContext,
                                                            @Required @QueryParam("id") String accountId) throws ServerException,
                                                                                                                 ForbiddenException {
        requiredNotNull(accountId, "Account id");
        final List<Workspace> workspaces = workspaceDao.getByAccount(accountId);
        final List<WorkspaceDescriptor> descriptors = new ArrayList<>(workspaces.size());
        for (Workspace workspace : workspaces) {
            descriptors.add(toDescriptor(workspace, securityContext));
        }
        return descriptors;
    }

    /**
     * Returns all memberships of current user.
     *
     * @return current user memberships
     * @throws ServerException
     *         when some error occurred while retrieving user or members
     * @see MemberDescriptor
     */
    @GET
    @Path("all")
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES)
    @RolesAllowed({"user", "temp_user"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfCurrentUser(@Context SecurityContext securityContext) throws NotFoundException,
                                                                                                               ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final List<Member> members = memberDao.getUserRelationships(user.getId());
        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
        for (Member member : members) {
            final Workspace workspace;
            try {
                workspace = workspaceDao.getById(member.getWorkspaceId());
            } catch (NotFoundException ex) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(),
                                        principal.getName()));
                continue;
            }
            memberships.add(toDescriptor(member, workspace, securityContext));
        }
        return memberships;
    }

    /**
     * Returns all memberships of certain user.
     *
     * @param userId
     *         user identifier to search memberships
     * @return certain user memberships
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ForbiddenException
     *         when user identifier is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving user or members
     * @see MemberDescriptor
     */
    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@Context SecurityContext securityContext,
                                                               @Required @QueryParam("userid") String userId) throws NotFoundException,
                                                                                                                     ForbiddenException,
                                                                                                                     ServerException {
        requiredNotNull(userId, "User identifier");
        userDao.getById(userId);
        final List<Member> members = memberDao.getUserRelationships(userId);
        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
        for (Member member : members) {
            final Workspace workspace;
            try {
                workspace = workspaceDao.getById(member.getWorkspaceId());
            } catch (NotFoundException nfEx) {
                LOG.error(String.format("Workspace %s doesn't exist but user %s refers to it. ", member.getWorkspaceId(), userId));
                continue;
            }
            memberships.add(toDescriptor(member, workspace, securityContext));
        }
        return memberships;
    }

    /**
     * Returns all workspace members.
     *
     * @param wsId
     *         workspace identifier
     * @return workspace members
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving workspace or members
     * @see MemberDescriptor
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #removeMember(String, String, SecurityContext)
     */
    @GET
    @Path("{id}/members")
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@PathParam("id") String wsId,
                                             @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Workspace workspace = workspaceDao.getById(wsId);
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final List<MemberDescriptor> descriptors = new ArrayList<>(members.size());
        for (Member member : members) {
            descriptors.add(toDescriptor(member, workspace, securityContext));
        }
        return descriptors;
    }

    /**
     * Returns membership for current user in the given workspace.
     *
     * @param wsId
     *         workspace identifier
     * @return workspace member
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving workspace or members
     * @see MemberDescriptor
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #removeMember(String, String, SecurityContext)
     */
    @GET
    @Path("{id}/membership")
    @RolesAllowed({"workspace/developer", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public MemberDescriptor getMembershipsOfCurrentUser(@PathParam("id") String wsId,
                                                        @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                         ServerException {
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final Workspace workspace = workspaceDao.getById(wsId);
        final Member member = memberDao.getWorkspaceMember(wsId, userId);

        return toDescriptor(member, workspace, securityContext);
    }

    /**
     * Removes attribute from certain workspace.
     *
     * @param wsId
     *         workspace identifier
     * @param attributeName
     *         attribute name to remove
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting or updating workspace
     * @throws ConflictException
     *         when given attribute name is not valid
     */
    @DELETE
    @Path("{id}/attribute")
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String wsId,
                                @QueryParam("name") String attributeName,
                                @Context SecurityContext securityContext) throws NotFoundException,
                                                                                 ServerException,
                                                                                 ConflictException {
        validateAttributeName(attributeName);
        final Workspace workspace = workspaceDao.getById(wsId);
        workspace.getAttributes().remove(attributeName);
        workspaceDao.update(workspace);
    }

    /**
     * Creates new workspace member.
     *
     * @param wsId
     *         workspace identifier
     * @param newMembership
     *         new membership
     * @return descriptor of created member
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving {@link Workspace}, {@link User}
     *         or persisting new {@link Member}
     * @throws ConflictException
     *         when new membership is {@code null}
     *         or if new membership user id is {@code null} or
     *         of new membership roles is {@code null} or empty
     * @throws ForbiddenException
     *         when current user hasn't access to workspace with given identifier
     * @see MemberDescriptor
     * @see #removeMember(String, String, SecurityContext)
     * @see #getMembers(String, SecurityContext)
     */
    @POST
    @Path("{id}/members")
    @RolesAllowed({"user", "temp_user"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MemberDescriptor addMember(@PathParam("id") String wsId,
                                      NewMembership newMembership,
                                      @Context SecurityContext securityContext) throws NotFoundException,
                                                                                       ServerException,
                                                                                       ConflictException,
                                                                                       ForbiddenException {
        requiredNotNull(newMembership, "New membership");
        requiredNotNull(newMembership.getRoles(), "Roles");
        requiredNotNull(newMembership.getUserId(), "User identifier");
        if (newMembership.getRoles().isEmpty()) {
            throw new ConflictException("Roles should not be empty");
        }
        final Workspace workspace = workspaceDao.getById(wsId);
        final User user = userDao.getById(newMembership.getUserId());
        if (!"true".equalsIgnoreCase(workspace.getAttributes().get("allowAnyoneAddMember"))) {
            ensureUserHasAccessToWorkspace(securityContext, wsId, "workspace/admin");
        }
        final Member newMember = new Member().withWorkspaceId(wsId)
                                             .withUserId(user.getId())
                                             .withRoles(newMembership.getRoles());
        memberDao.create(newMember);
        return toDescriptor(newMember, workspace, securityContext);
    }

    /**
     * Removes user with given identifier as member from certain workspace.
     *
     * @param wsId
     *         workspace identifier
     * @param userId
     *         user identifier to remove member
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving workspace or removing member
     * @throws ConflictException
     *         when removal member is last <i>"workspace/admin"</i> in given workspace
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #getMembers(String, SecurityContext)
     */
    @DELETE
    @Path("{id}/members/{userid}")
    @RolesAllowed("workspace/admin")
    public void removeMember(@PathParam("id") String wsId,
                             @PathParam("userid") String userId,
                             @Context SecurityContext securityContext) throws NotFoundException,
                                                                              ServerException,
                                                                              ConflictException {
        workspaceDao.getById(wsId);
        final List<Member> wsMembers = memberDao.getWorkspaceMembers(wsId);
        Member toRemove = null;
        //search for member
        for (Iterator<Member> mIt = wsMembers.iterator(); mIt.hasNext() && toRemove == null; ) {
            final Member current = mIt.next();
            if (current.getUserId().equals(userId)) {
                toRemove = current;
            }
        }
        if (toRemove != null) {
            //workspace should have at least 1 admin
            if (toRemove.getRoles().contains("workspace/admin")) {
                boolean isOtherWsAdminPresent = false;
                for (Iterator<Member> mIt = wsMembers.iterator(); mIt.hasNext() && !isOtherWsAdminPresent; ) {
                    final Member current = mIt.next();
                    isOtherWsAdminPresent = !current.getUserId().equals(toRemove.getUserId())
                                            && current.getRoles().contains("workspace/admin");
                }
                if (!isOtherWsAdminPresent) {
                    throw new ConflictException("Workspace should have at least 1 admin");
                }
            }
            memberDao.remove(toRemove);
        } else {
            throw new NotFoundException(String.format("User %s doesn't have membership in workspace %s", userId, wsId));
        }
    }

    /**
     * Removes certain workspace.
     * BTW all workspace members are going to be removed as well.
     *
     * @param wsId
     *         workspace identifier to remove workspace
     * @throws NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving/removing workspace or member
     * @throws ConflictException
     *         if some error occurred while removing member
     */
    @DELETE
    @Path("{id}")
    @RolesAllowed({"workspace/admin", "system/admin"})
    public void remove(@PathParam("id") String wsId,
                       @Context SecurityContext securityContext) throws NotFoundException, ServerException, ConflictException {
        workspaceDao.getById(wsId); // check workspaces' existence
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        for (Member member : members) {
            memberDao.remove(member);
        }
        workspaceDao.remove(wsId);
    }

    /**
     * Converts {@link Member} to {@link MemberDescriptor}
     */
    private MemberDescriptor toDescriptor(Member member, Workspace workspace, SecurityContext securityContext) {
        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
        final List<Link> links = new LinkedList<>();

        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("DELETE",
                                 Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                 null,
                                 null,
                                 serviceUriBuilder.clone()
                                                  .path(getClass(), "removeMember")
                                                  .build(workspace.getId(), member.getUserId())
                                                  .toString()));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 serviceUriBuilder.clone()
                                                  .path(getClass(), "getMembers")
                                                  .build(workspace.getId())
                                                  .toString()));
        }
        links.add(createLink("GET",
                             com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID,
                             null,
                             MediaType.APPLICATION_JSON,
                             baseUriBuilder.clone()
                                           .path(UserService.class)
                                           .path(UserService.class, "getById")
                                           .build(member.getUserId())
                                           .toString()));

        final Link wsLink = createLink("GET",
                                       Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                       null,
                                       MediaType.APPLICATION_JSON,
                                       serviceUriBuilder.clone()
                                                        .path(getClass(), "getById")
                                                        .build(workspace.getId())
                                                        .toString());
        final Link projectsLink = createLink("GET",
                                             com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS,
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             baseUriBuilder.clone()
                                                           .path(ProjectService.class)
                                                           .path(ProjectService.class, "getProjects")
                                                           .build(workspace.getId())
                                                           .toString());
        final WorkspaceReference wsRef = DtoFactory.getInstance().createDto(WorkspaceReference.class)
                                                   .withId(workspace.getId())
                                                   .withName(workspace.getName())
                                                   .withTemporary(workspace.isTemporary())
                                                   .withLinks(Arrays.asList(wsLink, projectsLink));
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withWorkspaceReference(wsRef)
                         .withRoles(member.getRoles())
                         .withLinks(links);
    }

    /**
     * Converts {@link Workspace} to {@link WorkspaceDescriptor}
     */
    private WorkspaceDescriptor toDescriptor(Workspace workspace, SecurityContext securityContext) {
        final WorkspaceDescriptor workspaceDescriptor = DtoFactory.getInstance().createDto(WorkspaceDescriptor.class)
                                                                  .withId(workspace.getId())
                                                                  .withName(workspace.getName())
                                                                  .withTemporary(workspace.isTemporary())
                                                                  .withAccountId(workspace.getAccountId())
                                                                  .withAttributes(workspace.getAttributes());
        final List<Link> links = new LinkedList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET",
                                 com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().clone()
                                                    .path(ProjectService.class)
                                                    .path(ProjectService.class, "getProjects")
                                                    .build(workspaceDescriptor.getId())
                                                    .toString()
                                ));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getMembershipsOfCurrentUser")
                                           .build()
                                           .toString()));
        }
        if (securityContext.isUserInRole("workspace/admin")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getMembers")
                                           .build(workspaceDescriptor.getId())
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_ADD_WORKSPACE_MEMBER,
                                 MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "addMember")
                                           .build(workspaceDescriptor.getId())
                                           .toString()));
        }
        if (securityContext.isUserInRole("workspace/admin") || securityContext.isUserInRole("workspace/developer") ||
            securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().
                                         path(getClass(), "getByName")
                                           .queryParam("name", workspaceDescriptor.getName())
                                           .build()
                                           .toString()));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(workspaceDescriptor.getId())
                                           .toString()));
        }
        if (securityContext.isUserInRole("workspace/admin") || securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE",
                                 Constants.LINK_REL_REMOVE_WORKSPACE,
                                 null,
                                 null,
                                 uriBuilder.clone()
                                           .path(getClass(), "remove")
                                           .build(workspaceDescriptor.getId())
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID,
                                 MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "update")
                                           .build(workspaceDescriptor.getId())
                                           .toString()));
        }
        return workspaceDescriptor.withLinks(links);
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ForbiddenException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ForbiddenException {
        if (object == null) {
            throw new ForbiddenException(subject + " required");
        }
    }

    /**
     * Validates attribute name.
     *
     * @param attributeName
     *         attribute name to check
     * @throws ConflictException
     *         when attribute name is {@code null}, empty or it starts with "codenvy"
     */
    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute name '%s' is not valid", attributeName));
        }
    }

    /**
     * Checks current user has access to workspace with given role
     */
    private void ensureUserHasAccessToWorkspace(SecurityContext securityContext, String wsId, String role)
            throws NotFoundException, ServerException, ForbiddenException {
        if ((securityContext.isUserInRole("user") || securityContext.isUserInRole("temp_user")) &&
            !getCurrentUserRoles(wsId, securityContext).contains(role)) {
            throw new ForbiddenException("Access denied");
        }
    }

    /**
     * Checks current user has access to workspace with given roles
     */
    private void ensureUserHasAccessToWorkspace(SecurityContext securityContext, String wsId, String role1, String role2)
            throws NotFoundException, ServerException, ForbiddenException {
        if (securityContext.isUserInRole("user") || securityContext.isUserInRole("temp_user")) {
            final Set<String> roles = getCurrentUserRoles(wsId, securityContext);
            if (!(roles.contains(role1) || roles.contains(role2))) {
                throw new ForbiddenException("Access denied");
            }
        }
    }

    /**
     * Gets current user roles to certain workspace
     */
    private Set<String> getCurrentUserRoles(String wsId, SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        for (Member member : memberDao.getUserRelationships(user.getId())) {
            if (wsId.equalsIgnoreCase(member.getWorkspaceId())) {
                return new HashSet<>(member.getRoles());
            }
        }
        return Collections.emptySet();
    }

    /**
     * Creates {@link Link}
     */
    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }
}