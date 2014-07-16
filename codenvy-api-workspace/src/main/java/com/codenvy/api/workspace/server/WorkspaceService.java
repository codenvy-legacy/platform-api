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
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.user.server.UserService;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.MemberDescriptor;
import com.codenvy.api.workspace.shared.dto.NewMembership;
import com.codenvy.api.workspace.shared.dto.NewWorkspace;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.api.workspace.shared.dto.WorkspaceReference;
import com.codenvy.api.workspace.shared.dto.WorkspaceUpdate;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
                           @Required @Description("new workspace") NewWorkspace newWorkspace)
            throws ConflictException, NotFoundException, ServerException, ForbiddenException {
        if (newWorkspace == null) {
            throw new ConflictException("Required new workspace");
        }
        if (newWorkspace.getAttributes() != null) {
            for (Attribute attribute : newWorkspace.getAttributes()) {
                validateAttributeName(attribute.getName());
            }
        }
        final String accountId = newWorkspace.getAccountId();
        final Account actualAcc;
        if (accountId == null || accountId.isEmpty() || (actualAcc = accountDao.getById(accountId)) == null) {
            throw new ConflictException("Incorrect account to associate workspace with");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final List<Account> accounts = accountDao.getByOwner(user.getId());
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

    @POST
    @Path("temp")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_TEMP_WORKSPACE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("static-access")
    public Response createTemporary(@Context SecurityContext securityContext,
                                    @Required @Description("New temporary workspace") NewWorkspace newWorkspace)
            throws ConflictException, NotFoundException, ServerException {
        if (newWorkspace == null) {
            throw new ConflictException("Missed workspace to create");
        }
        if (newWorkspace.getAttributes() != null) {
            for (Attribute attribute : newWorkspace.getAttributes()) {
                validateAttributeName(attribute.getName());
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
                final Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(user.getId()).withUserId(user.getId())
                                                  .withAttributes(Arrays.asList(DtoFactory.getInstance().createDto(
                                                          com.codenvy.api.user.shared.dto.Attribute.class)
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
        final Member member = new Member().withUserId(user.getId())
                                          .withWorkspaceId(wsId)
                                          .withRoles(Arrays.asList("workspace/developer", "workspace/admin"));
        memberDao.create(member);

        final WorkspaceDescriptor workspaceDescriptor = toDescriptor(workspace, securityContext);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(), user.getEmail());
        return Response.status(Response.Status.CREATED).entity(workspaceDescriptor).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws NotFoundException, ServerException, ForbiddenException {
        final Workspace workspace = workspaceDao.getById(id);
        try {
            ensureUserHasAccessToWorkspace(securityContext, workspace.getId(), "workspace/admin", "workspace/developer");
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
        return toDescriptor(workspace, securityContext);
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor getByName(@Context SecurityContext securityContext,
                                         @Required @Description("workspace name") @QueryParam("name") String name)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (name == null) {
            throw new ConflictException("Missed parameter name");
        }
        final Workspace workspace = workspaceDao.getByName(name);
        try {
            ensureUserHasAccessToWorkspace(securityContext, workspace.getId(), "workspace/admin", "workspace/developer");
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
        return toDescriptor(workspace, securityContext);
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID)
    @RolesAllowed({"workspace/admin", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public WorkspaceDescriptor update(@Context SecurityContext securityContext, @PathParam("id") String id,
                                      @Required @Description("workspace to update") WorkspaceUpdate update)
            throws NotFoundException, ConflictException, ServerException, ForbiddenException {
        if (update == null) {
            throw new ConflictException("Missed workspace to update");
        }
        final Workspace workspace = workspaceDao.getById(id);
        final List<Attribute> actualAttributes = workspace.getAttributes();
        if (update.getAttributes() != null) {
            Map<String, Attribute> updates = new LinkedHashMap<>(update.getAttributes().size());
            for (Attribute attribute : update.getAttributes()) {
                validateAttributeName(attribute.getName());
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
        if (update.getName() != null && !update.getName().equals(workspace.getName()) &&
            workspaceDao.getByName(update.getName()) == null) {
            workspace.setName(update.getName());
        }
        //todo what about accountId ? should it be possible to change account?
        workspaceDao.update(workspace);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
        return toDescriptor(workspace, securityContext);
    }

    @GET
    @Path("find/account")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACES_BY_ACCOUNT)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WorkspaceDescriptor> getWorkspacesByAccount(@Context SecurityContext securityContext,
                                                            @Required @Description("Account id to get workspaces")
                                                            @QueryParam("id") String accountId)
            throws NotFoundException, ConflictException, ServerException {
        if (accountId == null) {
            throw new ConflictException("Account id required");
        }
        final List<WorkspaceDescriptor> result = new LinkedList<>();
        for (Workspace workspace : workspaceDao.getByAccount(accountId)) {
            result.add(toDescriptor(workspace, securityContext));
        }
        return result;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES)
    @RolesAllowed({"user", "temp_user"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfCurrentUser(@Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
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
            memberships.add(toDescriptor(member, workspace));
        }
        return memberships;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@Context SecurityContext securityContext,
                                                               @Required @Description("user id to find workspaces")
                                                               @QueryParam("userid") String userId)
            throws NotFoundException, ConflictException, ServerException {
        if (userId == null) {
            throw new ConflictException("User identifier required");
        }
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
            memberships.add(toDescriptor(member, workspace));
        }
        return memberships;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_MEMBERS)
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException {
        final Workspace workspace = workspaceDao.getById(wsId);
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        final List<MemberDescriptor> descriptors = new ArrayList<>(members.size());
        for (Member member : members) {
            descriptors.add(toDescriptor(member, workspace));
        }
        return descriptors;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"workspace/admin", "system/admin", "system/manager"})
    public void addAttribute(@PathParam("id") String wsId, @Required @Description("New attribute") Attribute newAttribute,
                             @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (newAttribute == null) {
            throw new ConflictException("Attribute required");
        }
        validateAttributeName(newAttribute.getName());
        final Workspace workspace = workspaceDao.getById(wsId);
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
        validateAttributeName(attributeName);
        final List<Attribute> attributes = workspace.getAttributes();
        removeAttribute(attributes, attributeName);
        workspaceDao.update(workspace);
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_WORKSPACE_MEMBER)
    @RolesAllowed({"user", "temp_user"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MemberDescriptor addMember(@PathParam("id") String wsId,
                                      @Description("describes new workspace member") @Required NewMembership newMembership,
                                      @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (newMembership == null) {
            throw new ConflictException("Membership required");
        }
        if (newMembership.getRoles().isEmpty()) {
            throw new ConflictException("Roles required");
        }
        final Workspace workspace = workspaceDao.getById(wsId);
        final Map<String, String> attributes = new HashMap<>(workspace.getAttributes().size());
        for (Attribute attribute : workspace.getAttributes()) {
            attributes.put(attribute.getName(), attribute.getValue());
        }
        if (!"true".equalsIgnoreCase(attributes.get("allowAnyoneAddMember"))) {
            ensureUserHasAccessToWorkspace(securityContext, wsId, "workspace/admin");
        }
        final Member newMember = new Member().withWorkspaceId(wsId)
                                             .withUserId(newMembership.getUserId())
                                             .withRoles(newMembership.getRoles());
        memberDao.create(newMember);
        return toDescriptor(newMember, workspace);
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER)
    @RolesAllowed("workspace/admin")
    public void removeMember(@PathParam("id") String wsId, @PathParam("userid") String userId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException, ConflictException {
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
            throw new NotFoundException(String.format("User %s doesn't have membership with workspace %s", userId, wsId));
        }
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_WORKSPACE)
    @RolesAllowed({"workspace/admin", "system/admin"})
    public void remove(@PathParam("id") String wsId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException, ConflictException {
        workspaceDao.getById(wsId); // check workspaces' existence
        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
        for (Member member : members) {
            memberDao.remove(member);
        }
        workspaceDao.remove(wsId);
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

    private MemberDescriptor toDescriptor(Member member, Workspace workspace) {
        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
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
        final Link userLink = createLink("GET",
                                         com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID,
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         baseUriBuilder.clone()
                                                       .path(UserService.class)
                                                       .path(UserService.class, "getById")
                                                       .build(member.getUserId())
                                                       .toString());
        final Link removeLink = createLink("DELETE",
                                           Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                           null,
                                           null,
                                           serviceUriBuilder.clone()
                                                            .path(getClass(), "removeMember")
                                                            .build(workspace.getId(), member.getUserId())
                                                            .toString());
        final Link allMembersLink = createLink("GET",
                                               Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                               null,
                                               MediaType.APPLICATION_JSON,
                                               serviceUriBuilder.clone()
                                                                .path(getClass(), "getMembers")
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
                         .withLinks(Arrays.asList(userLink, removeLink, allMembersLink));
    }

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

    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute name '%s' is not valid", attributeName));
        }
    }

    private void ensureUserHasAccessToWorkspace(SecurityContext securityContext, String wsId, String role)
            throws NotFoundException, ServerException, ForbiddenException {
        if ((securityContext.isUserInRole("user") || securityContext.isUserInRole("temp_user")) &&
            !getCurrentUserRoles(wsId, securityContext).contains(role)) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void ensureUserHasAccessToWorkspace(SecurityContext securityContext, String wsId, String role1, String role2)
            throws NotFoundException, ServerException, ForbiddenException {
        if (securityContext.isUserInRole("user") || securityContext.isUserInRole("temp_user")) {
            final Set<String> roles = getCurrentUserRoles(wsId, securityContext);
            if (!(roles.contains(role1) || roles.contains(role2))) {
                throw new ForbiddenException("Access denied");
            }
        }
    }

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

    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }
}