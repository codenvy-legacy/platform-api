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
package com.codenvy.api.user.server;


import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
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
@Path("user")
public class UserService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserDao        userDao;
    private final UserProfileDao profileDao;
    private final MemberDao      memberDao;
    private final TokenValidator tokenValidator;

    @Inject
    public UserService(UserDao userDao, UserProfileDao profileDao, MemberDao memberDao, TokenValidator tokenValidator) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.memberDao = memberDao;
        this.tokenValidator = tokenValidator;
    }

    @POST
    @Path("create")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_USER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @Required @QueryParam("token") String token,
                           @Description("is user temporary") @QueryParam("temporary") boolean isTemporary)
            throws UnauthorizedException, ConflictException, ServerException {
        if (token == null) {
            throw new UnauthorizedException("Missed token parameter");
        }
        final String userEmail = tokenValidator.validateToken(token);
        final User user = DtoFactory.getInstance().createDto(User.class);
        String userId = NameGenerator.generate(User.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        user.setId(userId);
        user.setEmail(userEmail);
        user.setPassword(NameGenerator.generate("pass", Constants.PASSWORD_LENGTH));
        userDao.create(user);
        Profile profile = DtoFactory.getInstance().createDto(Profile.class);
        profile.setId(userId);
        profile.setUserId(userId);
        profile.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                      .withName("temporary")
                                                      .withValue(String.valueOf(isTemporary))
                                                      .withDescription("Indicates is this user is temporary")));
        profileDao.create(profile);

        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrent(@Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        if (user == null) {
            throw new NotFoundException(String.format("User %s doesn't exist.", principal.getName()));
        }
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    @POST
    @Path("password")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PASSWORD)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updatePassword(@Context SecurityContext securityContext, @FormParam("password") String password)
            throws ForbiddenException, NotFoundException, ServerException {
        if (password == null) {
            throw new ForbiddenException("Password required");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        if (user == null) {
            throw new NotFoundException(String.format("User %s doesn't exist.", principal.getName()));
        }
        user.setPassword(password);
        userDao.update(user);
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_USER_BY_ID)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws NotFoundException, ServerException {
        final User user = userDao.getById(id);
        if (user == null) {
            throw new NotFoundException(String.format("User %s doesn't exist.", id));
        }
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_USER_BY_EMAIL)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getByEmail(@Context SecurityContext securityContext, @Required @Description("user email") @QueryParam("email") String email)
            throws ForbiddenException, NotFoundException, ServerException {
        if (email == null) {
            throw new ForbiddenException("Missed parameter email");
        }
        final User user = userDao.getByAlias(email);
        if (user == null) {
            throw new NotFoundException(String.format("User %s doesn't exist.", email));
        }
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_USER_BY_ID)
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws NotFoundException, ServerException {
        final User user = userDao.getById(id);
        if (user == null) {
            throw new NotFoundException(String.format("User %s doesn't exist.", id));
        }
        List<Member> members = memberDao.getUserRelationships(id);
        for (Member member : members) {
            memberDao.remove(member);
        }
        profileDao.remove(user.getId());
        userDao.remove(id);
    }

    private void injectLinks(User user, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_USER_PROFILE, null, MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().path(UserProfileService.class)
                                                    .path(UserProfileService.class, "getCurrent").build().toString()
                                ));
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_USER, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_PASSWORD, MediaType.APPLICATION_FORM_URLENCODED, null,
                                 uriBuilder.clone().path(getClass(), "updatePassword").build().toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withRequired(true)
                                                                      .withName("password")
                                                                      .withDescription("new password")
                                                                      .withType(ParameterType.String))));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_USER_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(user.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_USER_PROFILE_BY_ID, null, MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().path(UserProfileService.class).path(
                                         UserProfileService.class, "getById")
                                                    .build(user.getId()).toString()
                                ));
            links.add(createLink("GET", Constants.LINK_REL_GET_USER_BY_EMAIL, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByEmail").queryParam("email", user.getEmail()).build()
                                           .toString()
                                ));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE", Constants.LINK_REL_REMOVE_USER_BY_ID, null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(user.getId()).toString()));
        }
        user.setLinks(links);
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