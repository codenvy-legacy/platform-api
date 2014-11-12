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
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.codenvy.api.core.rest.shared.Links.createLink;
import static java.lang.Boolean.parseBoolean;
import static javax.ws.rs.core.Response.Status.CREATED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
import static com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS;
import static com.codenvy.api.workspace.server.Constants.ID_LENGTH;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_CREATE_TEMP_WORKSPACE;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_BY_ID;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_MEMBERS;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACES_BY_ACCOUNT;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_BY_NAME;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE;
import static javax.ws.rs.core.Response.status;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Api(value = "/workspace",
     description = "Workspace manager")
@Path("/workspace")
public class WorkspaceService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceDao   workspaceDao;
    private final UserDao        userDao;
    private final MemberDao      memberDao;
    private final UserProfileDao profileDao;
    private final AccountDao     accountDao;
    private final boolean        isOrgAddonEnabledByDefault;

    @Inject
    public WorkspaceService(WorkspaceDao workspaceDao,
                            UserDao userDao,
                            MemberDao memberDao,
                            UserProfileDao profileDao,
                            AccountDao accountDao,
                            @Named("subscription.orgaddon.enabled") String isOrgAddonEnabledByDefault) {
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.memberDao = memberDao;
        this.profileDao = profileDao;
        this.accountDao = accountDao;
        this.isOrgAddonEnabledByDefault = parseBoolean(isOrgAddonEnabledByDefault);
    }

    /**
     * Creates new workspace and adds current user as member to created workspace
     * with roles <i>"workspace/admin"</i> and <i>"workspace/developer"</i>. Returns status code <strong>201 CREATED</strong>
     * and {@link com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor} if workspace has been created successfully.
     * Each new workspace should contain at least name and account identifier.
     *
     * @param newWorkspace
     *         new workspace
     * @return descriptor of created workspace
     * @throws com.codenvy.api.core.ConflictException
     *         when current user account identifier and given account identifier are different
     * @throws com.codenvy.api.core.NotFoundException
     *         when account with given identifier does not exist
     * @throws com.codenvy.api.core.ServerException
     *         when some error occurred while retrieving/persisting account, workspace or member
     * @throws com.codenvy.api.core.ForbiddenException
     *         when user has not access to create workspaces,
     *         or when new workspace is {@code null},
     *         or any of workspace name or account id is {@code null}
     * @see com.codenvy.api.workspace.shared.dto.NewWorkspace
     * @see com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor
     * @see #getById(String, javax.ws.rs.core.SecurityContext)
     * @see #getByName(String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Create a new workspace",
                  response = WorkspaceDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "You have no access to create more workspaces"),
            @ApiResponse(code = 404, message = "NOT FOUND"),
            @ApiResponse(code = 409, message = "You can create workspace associated only to your own account"),
            @ApiResponse(code = 500, message = "INTERNAL SERVER ERROR")})
    @POST
    @GenerateLink(rel = LINK_REL_CREATE_WORKSPACE)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response create(@ApiParam(value = "new workspace", required = true)
                           @Required
                           @Description("new workspace")
                           NewWorkspace newWorkspace,
                           @Context SecurityContext context) throws ConflictException,
                                                                    NotFoundException,
                                                                    ServerException,
                                                                    ForbiddenException {
        requiredNotNull(newWorkspace, "New workspace");
        requiredNotNull(newWorkspace.getAccountId(), "Account ID");
        requiredNotNull(newWorkspace.getName(), "Workspace name");
        if (newWorkspace.getAttributes() != null) {
            validateAttributes(newWorkspace.getAttributes());
        }
        final Account account = accountDao.getById(newWorkspace.getAccountId());
        //check user has access to add new workspace
        if (!context.isUserInRole("system/admin") && !isOrgAddonEnabledByDefault) {
            ensureCurrentUserOwnerOf(account);
            final String multiWs = account.getAttributes().get("codenvy:multi-ws");
            final List<Workspace> existedWorkspaces = workspaceDao.getByAccount(newWorkspace.getAccountId());
            if (!parseBoolean(multiWs) && !existedWorkspaces.isEmpty()) {
                throw new ForbiddenException("You don't have access to create more workspaces");
            }
        }
        final Workspace workspace = new Workspace().withId(NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), ID_LENGTH))
                                                   .withName(newWorkspace.getName())
                                                   .withTemporary(false)
                                                   .withAccountId(newWorkspace.getAccountId())
                                                   .withAttributes(newWorkspace.getAttributes());
        workspaceDao.create(workspace);
        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", newWorkspace.getName(), workspace.getId(), currentUser().getId());
        return status(CREATED).entity(toDescriptor(workspace, context)).build();
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
    @ApiOperation(value = "Create a temporary workspace",
                  notes = "Create a temporary workspace created by a Factory",
                  response = WorkspaceDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "You have no access to create more workspaces"),
            @ApiResponse(code = 404, message = "NOT FOUND"),
            @ApiResponse(code = 409, message = "You can create workspace associated only to your own account"),
            @ApiResponse(code = 500, message = "INTERNAL SERVER ERROR")
    })
    @POST
    @Path("/temp")
    @GenerateLink(rel = LINK_REL_CREATE_TEMP_WORKSPACE)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuppressWarnings("static-access")
    public Response createTemporary(@ApiParam(value = "New Temporary workspace", required = true)
                                    @Required
                                    @Description("New temporary workspace")
                                    NewWorkspace newWorkspace,
                                    @Context SecurityContext context) throws ConflictException,
                                                                             NotFoundException,
                                                                             ForbiddenException,
                                                                             ServerException {
        requiredNotNull(newWorkspace, "New workspace");
        if (newWorkspace.getAttributes() != null) {
            validateAttributes(newWorkspace.getAttributes());
        }
        final Workspace workspace = new Workspace().withId(NameGenerator.generate(Workspace.class.getSimpleName().toLowerCase(), ID_LENGTH))
                                                   .withName(newWorkspace.getName())
                                                   .withTemporary(true)
                                                   .withAccountId(newWorkspace.getAccountId())
                                                   .withAttributes(newWorkspace.getAttributes());
        createTemporaryWorkspace(workspace);
        //temporary user should be created if real user does not exist
        final User user;
        if (context.getUserPrincipal() == null) {
            user = createTemporaryUser();
        } else {
            user = userDao.getById(currentUser().getId());
        }

        final Member newMember = new Member().withUserId(user.getId())
                                             .withWorkspaceId(workspace.getId())
                                             .withRoles(asList("workspace/developer", "workspace/admin"));
        memberDao.create(newMember);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(), user.getId());
        return status(CREATED).entity(toDescriptor(workspace, context)).build();
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
    @ApiOperation(value = "Get workspace by ID",
                  response = WorkspaceDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Workspace with specified ID does not exist"),
            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    public WorkspaceDescriptor getById(@ApiParam(value = "Workspace ID")
                                       @Description("Workspace ID")
                                       @PathParam("id")
                                       String id,
                                       @Context SecurityContext context) throws NotFoundException,
                                                                                ServerException,
                                                                                ForbiddenException {
        final Workspace workspace = workspaceDao.getById(id);
        if (!context.isUserInRole("workspace/developer") && !context.isUserInRole("workspace/admin")) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            final Map<String, String> attributes = workspace.getAttributes();
            if (attributes.containsKey("allowAnyoneAddMember")) {
                workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
            } else {
                attributes.clear();
            }
        }
        return toDescriptor(workspace, context);
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
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Workspace with specified name doesn't exist"),
            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @GenerateLink(rel = LINK_REL_GET_WORKSPACE_BY_NAME)
    @Produces(APPLICATION_JSON)
    public WorkspaceDescriptor getByName(@ApiParam(value = "Name of workspace", required = true)
                                         @Required
                                         @Description("Name of workspace")
                                         @QueryParam("name")
                                         String name,
                                         @Context SecurityContext context) throws NotFoundException,
                                                                                  ServerException,
                                                                                  ForbiddenException {
        requiredNotNull(name, "Workspace name");
        final Workspace workspace = workspaceDao.getByName(name);
        if (!context.isUserInRole("workspace/developer") && !context.isUserInRole("workspace/admin")) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            final Map<String, String> attributes = workspace.getAttributes();
            if (attributes.containsKey("allowAnyoneAddMember")) {
                workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
            } else {
                attributes.clear();
            }
        }
        return toDescriptor(workspace, context);
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
    @ApiOperation(value = "Update workspace",
                  response = WorkspaceDescriptor.class,
                  notes = "Update an existing workspace. A JSON with updated properties is sent.",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Workspace updated"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 403, message = "Access to required workspace is forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @POST
    @Path("/{id}")
    @RolesAllowed({"workspace/admin", "system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public WorkspaceDescriptor update(@ApiParam(value = "Workspace ID")
                                      @PathParam("id")
                                      String id,
                                      @ApiParam(value = "Workspace update", required = true)
                                      @Required
                                      WorkspaceUpdate update,
                                      @Context SecurityContext context) throws NotFoundException,
                                                                               ConflictException,
                                                                               ForbiddenException,
                                                                               ServerException {
        requiredNotNull(update, "Workspace update");
        final Workspace workspace = workspaceDao.getById(id);
        final Map<String, String> attributes = update.getAttributes();
        if (attributes != null) {
            validateAttributes(attributes);
            workspace.getAttributes().putAll(attributes);
        }
        final String newName = update.getName();
        if (newName != null) {
            workspace.setName(newName);
        }
        workspaceDao.update(workspace);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
        return toDescriptor(workspace, context);
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
    @ApiOperation(value = "Get workspace by Account ID",
                  notes = "Search for a workspace by its Account ID which is added as query parameter",
                  response = WorkspaceDescriptor.class,
                  responseContainer = "List",
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "User is not authorized to call this operation"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find/account")
    @GenerateLink(rel = LINK_REL_GET_WORKSPACES_BY_ACCOUNT)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public List<WorkspaceDescriptor> getWorkspacesByAccount(@ApiParam(value = "Account ID", required = true)
                                                            @Required
                                                            @QueryParam("id")
                                                            String accountId,
                                                            @Context SecurityContext context) throws ServerException,
                                                                                                     ForbiddenException {
        requiredNotNull(accountId, "Account ID");
        final List<Workspace> workspaces = workspaceDao.getByAccount(accountId);
        final List<WorkspaceDescriptor> descriptors = new ArrayList<>(workspaces.size());
        for (Workspace workspace : workspaces) {
            descriptors.add(toDescriptor(workspace, context));
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
    @ApiOperation(value = "Get membership of a current user",
                  notes = "Get membership and workspace roles of a current user",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/all")
    @GenerateLink(rel = LINK_REL_GET_CURRENT_USER_WORKSPACES)
    @RolesAllowed({"user", "temp_user"})
    @Produces(APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfCurrentUser(@Context SecurityContext context) throws NotFoundException,
                                                                                                       ServerException {
        final List<Member> members = memberDao.getUserRelationships(currentUser().getId());
        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
        for (Member member : members) {
            try {
                final Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
                memberships.add(toDescriptor(member, workspace, context));
            } catch (NotFoundException nfEx) {
                LOG.error("Workspace {} doesn't exist but user {} refers to it. ", member.getWorkspaceId(), currentUser().getId());
            }
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
    @ApiOperation(value = "Get memberships by user ID",
                  notes = "Search for a workspace by User ID which is added to URL as query parameter. JSON with workspace details and user roles is returned",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this action"),
            @ApiResponse(code = 404, message = "Not Foound"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find")
    @GenerateLink(rel = LINK_REL_GET_CONCRETE_USER_WORKSPACES)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@ApiParam(value = "User ID", required = true)
                                                               @Required
                                                               @QueryParam("userid")
                                                               String userId,
                                                               @Context SecurityContext context) throws NotFoundException,
                                                                                                        ForbiddenException,
                                                                                                        ServerException {
        requiredNotNull(userId, "User ID");
        final List<Member> members = memberDao.getUserRelationships(userId);
        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
        for (Member member : members) {
            try {
                final Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
                memberships.add(toDescriptor(member, workspace, context));
            } catch (NotFoundException nfEx) {
                LOG.error("Workspace {} doesn't exist but user {} refers to it. ", member.getWorkspaceId(), userId);
            }
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
    @ApiOperation(value = "Get workspace members by workspace ID",
                  notes = "Get all workspace members of a specified workspace. A JSOn with members and their roles is returned",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/members")
    @RolesAllowed({"workspace/admin", "workspace/developer", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@ApiParam(value = "Workspace ID")
                                             @PathParam("id")
                                             String wsId,
                                             @Context SecurityContext context) throws NotFoundException, ServerException {
        final Workspace workspace = workspaceDao.getById(wsId);
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final List<MemberDescriptor> descriptors = new ArrayList<>(members.size());
        for (Member member : members) {
            descriptors.add(toDescriptor(member, workspace, context));
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
    @ApiOperation(value = "Get user membership in a specified workspace",
                  notes = "Returns membership of a user with roles",
                  response = MemberDescriptor.class,
                  position = 10)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/membership")
    @RolesAllowed({"workspace/developer", "workspace/admin"})
    @Produces(APPLICATION_JSON)
    public MemberDescriptor getMembershipOfCurrentUser(@ApiParam(value = "Workspace ID")
                                                       @PathParam("id")
                                                       String wsId,
                                                       @Context SecurityContext context) throws NotFoundException,
                                                                                                ServerException {
        final Workspace workspace = workspaceDao.getById(wsId);
        final Member member = memberDao.getWorkspaceMember(wsId, currentUser().getId());
        return toDescriptor(member, workspace, context);
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
    @ApiOperation(value = "Delete workspace attribute",
                  notes = "Deletes attributes of a specified workspace",
                  position = 11)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No Content"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Invalid attribute name"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/attribute")
    @RolesAllowed({"workspace/admin", "system/admin"})
    public void removeAttribute(@ApiParam(value = "Workspace ID")
                                @PathParam("id")
                                String wsId,
                                @ApiParam(value = "Attribute name", required = true)
                                @Required
                                @QueryParam("name")
                                String attributeName,
                                @Context SecurityContext context) throws NotFoundException,
                                                                         ServerException,
                                                                         ConflictException {
        validateAttributeName(attributeName);
        final Workspace workspace = workspaceDao.getById(wsId);
        if (null != workspace.getAttributes().remove(attributeName)) {
            workspaceDao.update(workspace);
        }
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
     *         when some error occurred while retrieving {@link Workspace}, {@link com.codenvy.api.user.shared.dto.UserDescriptor}
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
    @ApiOperation(value = "Create new workspace member",
                  notes = "Add a new member into a workspace",
                  response = MemberDescriptor.class,
                  position = 12)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to perform this operation"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No user ID and/or role specified")})
    @POST
    @Path("/{id}/members")
    @RolesAllowed({"user", "temp_user"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response addMember(@ApiParam(value = "Workspace ID")
                              @PathParam("id")
                              String wsId,
                              @ApiParam(value = "New membership", required = true)
                              @Required
                              NewMembership newMembership,
                              @Context SecurityContext context) throws NotFoundException,
                                                                       ServerException,
                                                                       ConflictException,
                                                                       ForbiddenException {
        requiredNotNull(newMembership, "New membership");
        requiredNotNull(newMembership.getUserId(), "User ID");
        final Workspace workspace = workspaceDao.getById(wsId);
        if (memberDao.getWorkspaceMembers(wsId).isEmpty()) {
            //if workspace doesn't contain members then member that is been added
            //should be added with roles 'workspace/admin' and 'workspace/developer'
            newMembership.setRoles(asList("workspace/admin", "workspace/developer"));

        } else {
            requiredNotNull(newMembership.getRoles(), "Roles");
            if (newMembership.getRoles().isEmpty()) {
                throw new ConflictException("Roles should not be empty");
            }
            if (!context.isUserInRole("workspace/admin") && !parseBoolean(workspace.getAttributes().get("allowAnyoneAddMember"))) {
                throw new ForbiddenException("Access denied");
            }
        }
        final User user = userDao.getById(newMembership.getUserId());
        final Member newMember = new Member().withWorkspaceId(wsId)
                                             .withUserId(user.getId())
                                             .withRoles(newMembership.getRoles());
        memberDao.create(newMember);
        return status(CREATED).entity(toDescriptor(newMember, workspace, context)).build();
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
    @ApiOperation(value = "Remove user from workspace",
                  notes = "Remove a user from a workspace by User ID",
                  position = 13)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No Content"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Cannot remove workspace/admin"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/members/{userid}")
    @RolesAllowed("workspace/admin")
    public void removeMember(@ApiParam(value = "Workspace ID")
                             @PathParam("id")
                             String wsId,
                             @ApiParam(value = "User ID")
                             @PathParam("userid")
                             String userId,
                             @Context SecurityContext context) throws NotFoundException,
                                                                      ServerException,
                                                                      ConflictException {
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        //search for member
        Member target = null;
        int admins = 0;
        for (Member member : members) {
            if (member.getRoles().contains("workspace/admin")) admins++;
            if (member.getUserId().equals(userId)) target = member;
        }
        if (target == null) {
            throw new ConflictException(String.format("User %s doesn't have membership with workspace %s", userId, wsId));
        }
        //workspace should have at least 1 admin
        if (admins == 1 && target.getRoles().contains("workspace/admin")) {
            throw new ConflictException("Workspace should have at least 1 admin");
        }
        memberDao.remove(target);
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
    @ApiOperation(value = "Delete a workspace",
                  notes = "Delete a workspace by its ID",
                  position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No Content"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Failed to remove workspace member"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"workspace/admin", "system/admin"})
    public void remove(@ApiParam(value = "Workspace ID")
                       @PathParam("id")
                       String wsId) throws NotFoundException, ServerException, ConflictException {
        workspaceDao.remove(wsId);
    }

    private void createTemporaryWorkspace(Workspace workspace) throws ConflictException, ServerException {
        try {
            //let vfs create temporary workspace in correct place
            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
            workspaceDao.create(workspace);
        } finally {
            EnvironmentContext.getCurrent().setWorkspaceTemporary(false);
        }
    }

    private User createTemporaryUser() throws ConflictException, ServerException, NotFoundException {
        final User user = new User().withId(NameGenerator.generate("tmp_user", com.codenvy.api.user.server.Constants.ID_LENGTH));
        userDao.create(user);
        try {
            final Map<String, String> attributes = new HashMap<>(4);
            attributes.put("temporary", String.valueOf(true));
            attributes.put("codenvy:created", Long.toString(System.currentTimeMillis()));
            profileDao.create(new Profile().withId(user.getId())
                                           .withUserId(user.getId())
                                           .withAttributes(attributes));
        } catch (ApiException e) {
            userDao.remove(user.getId());
            throw e;
        }
        return user;
    }

    /**
     * Converts {@link Member} to {@link MemberDescriptor}
     */
    /* used in tests */MemberDescriptor toDescriptor(Member member, Workspace workspace, SecurityContext context) {
        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
        final List<Link> links = new LinkedList<>();

        if (context.isUserInRole("workspace/admin") || context.isUserInRole("workspace/developer")) {

            links.add(createLink("GET",
                                 serviceUriBuilder.clone()
                                                  .path(getClass(), "getMembers")
                                                  .build(workspace.getId())
                                                  .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_WORKSPACE_MEMBERS));
        }
        if (context.isUserInRole("workspace/admin")) {
            links.add(createLink("DELETE",
                                 serviceUriBuilder.clone()
                                                  .path(getClass(), "removeMember")
                                                  .build(workspace.getId(), member.getUserId())
                                                  .toString(),
                                 null,
                                 null,
                                 LINK_REL_REMOVE_WORKSPACE_MEMBER));
        }
        links.add(createLink("GET",
                             baseUriBuilder.clone()
                                           .path(UserService.class)
                                           .path(UserService.class, "getById")
                                           .build(member.getUserId())
                                           .toString(),
                             null,
                             APPLICATION_JSON,
                             LINK_REL_GET_USER_BY_ID));
        final Link wsLink = createLink("GET",
                                       serviceUriBuilder.clone()
                                                        .path(getClass(), "getById")
                                                        .build(workspace.getId())
                                                        .toString(),
                                       null,
                                       APPLICATION_JSON,
                                       LINK_REL_GET_WORKSPACE_BY_ID);
        final Link projectsLink = createLink("GET",
                                             baseUriBuilder.clone()
                                                           .path(ProjectService.class)
                                                           .path(ProjectService.class, "getProjects")
                                                           .build(workspace.getId())
                                                           .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_PROJECTS);
        final WorkspaceReference wsRef = DtoFactory.getInstance().createDto(WorkspaceReference.class)
                                                   .withId(workspace.getId())
                                                   .withName(workspace.getName())
                                                   .withTemporary(workspace.isTemporary())
                                                   .withLinks(asList(wsLink, projectsLink));
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withWorkspaceReference(wsRef)
                         .withRoles(member.getRoles())
                         .withLinks(links);
    }

    /**
     * Converts {@link Workspace} to {@link WorkspaceDescriptor}
     */
    /* used in tests */WorkspaceDescriptor toDescriptor(Workspace workspace, SecurityContext context) {
        final WorkspaceDescriptor workspaceDescriptor = DtoFactory.getInstance().createDto(WorkspaceDescriptor.class)
                                                                  .withId(workspace.getId())
                                                                  .withName(workspace.getName())
                                                                  .withTemporary(workspace.isTemporary())
                                                                  .withAccountId(workspace.getAccountId())
                                                                  .withAttributes(workspace.getAttributes());
        final List<Link> links = new LinkedList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (context.isUserInRole("user")) {
            links.add(createLink("GET",
                                 getServiceContext().getBaseUriBuilder().clone()
                                                    .path(ProjectService.class)
                                                    .path(ProjectService.class, "getProjects")
                                                    .build(workspaceDescriptor.getId())
                                                    .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS));
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getMembershipsOfCurrentUser")
                                           .build()
                                           .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_CURRENT_USER_WORKSPACES));
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getMembershipOfCurrentUser")
                                           .build(workspaceDescriptor.getId())
                                           .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_CURRENT_USER_MEMBERSHIP));
        }
        if (context.isUserInRole("workspace/admin") || context.isUserInRole("workspace/developer") ||
            context.isUserInRole("system/admin") || context.isUserInRole("system/manager")) {
            links.add(createLink("GET",
                                 uriBuilder.clone().
                                         path(getClass(), "getByName")
                                           .queryParam("name", workspaceDescriptor.getName())
                                           .build()
                                           .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_WORKSPACE_BY_NAME));
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(workspaceDescriptor.getId())
                                           .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_WORKSPACE_BY_ID));
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getMembers")
                                           .build(workspaceDescriptor.getId())
                                           .toString(),
                                 null,
                                 APPLICATION_JSON,
                                 LINK_REL_GET_WORKSPACE_MEMBERS));
        }
        if (context.isUserInRole("workspace/admin") || context.isUserInRole("system/admin")) {
            links.add(createLink("DELETE",
                                 uriBuilder.clone()
                                           .path(getClass(), "remove")
                                           .build(workspaceDescriptor.getId())
                                           .toString(),
                                 null,
                                 null,
                                 LINK_REL_REMOVE_WORKSPACE));
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

    private void validateAttributes(Map<String, String> attributes) throws ConflictException {
        for (String attributeName : attributes.keySet()) {
            validateAttributeName(attributeName);
        }
    }

    private void ensureCurrentUserOwnerOf(Account target) throws ServerException, NotFoundException, ConflictException {
        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
        for (Account account : accounts) {
            if (account.getId().equals(target.getId())) {
                return;
            }
        }
        throw new ConflictException("You can create workspace associated only with your own account");
    }

    private com.codenvy.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}