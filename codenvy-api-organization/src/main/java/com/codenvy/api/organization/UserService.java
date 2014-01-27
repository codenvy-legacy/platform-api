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
import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Attribute;
import com.codenvy.api.organization.shared.dto.Member;
import com.codenvy.api.organization.shared.dto.Profile;
import com.codenvy.api.organization.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.google.inject.Inject;

import javax.annotation.security.RolesAllowed;
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
 * User API
 *
 * @author Eugene Voevodin
 */
@Path("/user")
public class UserService extends Service {

    private static final int ID_LENGTH = 16;
    private final UserDao        userDao;
    private final UserProfileDao profileDao;
    private final MemberDao      memberDao;

    @Inject
    public UserService(UserDao userDao, UserProfileDao profileDao, MemberDao memberDao) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.memberDao = memberDao;
    }

    @POST
    @Path("create")
    @GenerateLink(rel = "create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @QueryParam("token") String token,
                           @QueryParam("temporary") Boolean isTemporary)
            throws OrganizationServiceException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = DtoFactory.getInstance().createDto(User.class);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        String userId = NameGenerator.generate(User.class.getSimpleName(), ID_LENGTH);
        user.setId(userId);
        user.setAliases(Arrays.asList(principal.getName()));
        userDao.create(user);
        try {
            Profile profile = DtoFactory.getInstance().createDto(Profile.class);
            String profileId = NameGenerator.generate(Profile.class.getSimpleName(), ID_LENGTH);
            profile.setId(profileId);
            profile.setUserId(userId);
            profile.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                          .withName("temporary")
                                                          .withValue(String.valueOf(isTemporary))
                                                          .withDescription("Is workspace temporary")));
            profileDao.create(profile);
        } catch (OrganizationServiceException e) {
            userDao.removeById(userId);
        }
        final List<Link> links = new ArrayList<>();
        if (securityContext.isUserInRole("user")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withMethod("GET")
                                .withRel("self")
                                .withHref(uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withConsumes(MediaType.APPLICATION_JSON)
                                .withMethod("POST")
                                .withRel("update password")
                                .withHref(uriBuilder.clone().path(getClass(), "updatePassword").build().toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withMethod("GET")
                                .withRel("profile")
                                .withHref(getServiceContext().getBaseUriBuilder().clone().path(UserProfileService.class)
                                                  .path(UserProfileService.class, "getCurrent")
                                                  .build().toString()));
        }
        if (securityContext.isUserInRole("system/manager") || securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("user by id")
                                .withHref(uriBuilder.clone().path(getClass(), "getById").build(userId).toString()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON)
                                .withRel("user by email")
                                .withHref(uriBuilder.clone().path(getClass(), "getByEmail").queryParam("email", principal.getName()).build()
                                                    .toString()));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove by id")
                                .withHref(uriBuilder.clone().path(getClass(), "removeById").build(userId).toString()));
        }
        user.setLinks(links);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @GET
    @GenerateLink(rel = "current")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrent(@Context SecurityContext securityContext) throws OrganizationServiceException {
        final User user = userDao.getByAlias(securityContext.getUserPrincipal().getName());
        final List<Link> links = new ArrayList<>(1);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withConsumes(MediaType.APPLICATION_JSON)
                            .withMethod("POST")
                            .withRel("change password")
                            .withHref(uriBuilder.clone().path(getClass(), "updatePassword").build().toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("profile")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(UserProfileService.class)
                                              .path(UserProfileService.class, "getCurrent")
                                              .build().toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("workspaces")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(WorkspaceService.class)
                                              .path(WorkspaceService.class, "getAll")
                                              .build().toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("accounts")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(AccountService.class)
                                              .path(AccountService.class, "getAll")
                                              .build().toString()));
        user.setLinks(links);
        return user;
    }

    @POST
    @Path("password")
    @GenerateLink(rel = "update password")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePassword(@Context SecurityContext securityContext, String password) throws OrganizationServiceException {
        final User user = userDao.getByAlias(securityContext.getUserPrincipal().getName());
        user.setPassword(password);
        userDao.update(user);
        return Response.noContent().build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "get by id")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getById(@Context SecurityContext securityContext, @PathParam("id") String id) throws OrganizationServiceException {
        final User user = userDao.getById(id);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("user by email")
                            .withHref(uriBuilder.clone().path(getClass(), "getByEmail")
                                                .queryParam("email", securityContext.getUserPrincipal().getName()).build().toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("profile")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(UserProfileService.class)
                                              .path(UserProfileService.class, "getById")
                                              .build(user.getProfileId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("workspaces")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(WorkspaceService.class)
                                              .path(WorkspaceService.class, "getAllById")
                                              .build(user.getId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("accounts")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(AccountService.class)
                                              .path(AccountService.class, "getAllById")
                                              .build(user.getId()).toString()));
        if (securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove by id")
                                .withHref(uriBuilder.clone().path(getClass(), "removeById").build(user.getId()).toString()));
        }
        user.setLinks(links);
        return user;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = "user by email")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getByEmail(@Context SecurityContext securityContext, @QueryParam("email") String email)
            throws OrganizationServiceException {
        final User user = userDao.getByAlias(email);
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withRel("user by id")
                            .withHref(uriBuilder.clone().path(getClass(), "getById").build(user.getId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("profile")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(UserProfileService.class)
                                              .path(UserProfileService.class, "getById")
                                              .build(user.getProfileId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("workspaces")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(WorkspaceService.class)
                                              .path(WorkspaceService.class, "getAllById")
                                              .build(user.getId()).toString()));
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withMethod("GET")
                            .withRel("accounts")
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withHref(getServiceContext().getBaseUriBuilder().clone().path(AccountService.class)
                                              .path(AccountService.class, "getAllById")
                                              .build(user.getId()).toString()));
        if (securityContext.isUserInRole("system/admin")) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withMethod("DELETE")
                                .withRel("remove by id")
                                .withHref(uriBuilder.clone().path(getClass(), "removeById").build(user.getId()).toString()));
        }
        user.setLinks(links);
        return user;
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = "remove")
    @RolesAllowed("system/admin")
    public Response removeById(@PathParam("id") String id) throws OrganizationServiceException {
        List<Member> members = memberDao.getUserRelationships(id);
        for (Member member : members) {
            memberDao.removeWorkspaceMember(member.getWorkspaceId(), member.getUserId());
        }
        profileDao.remove(id);
        userDao.removeById(id);
        return Response.noContent().build();
    }
}