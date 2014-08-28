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
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User API
 *
 * @author Eugene Voevodin
 */


@Api(value = "/user",
     description = "User manager")
@Path("/user")

public class UserService extends Service {

    private final UserDao        userDao;
    private final UserProfileDao profileDao;
    private final TokenValidator tokenValidator;

    @Inject
    public UserService(UserDao userDao, UserProfileDao profileDao, TokenValidator tokenValidator) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.tokenValidator = tokenValidator;
    }

    /**
     * Creates new user and profile.
     * Returns status code <strong>201 CREATED</strong> and {@link User} entity.
     *
     * @param token
     *         authentication token
     * @param isTemporary
     *         if it is {@code true} creates temporary user
     * @return entity of created user
     * @throws UnauthorizedException
     *         when token is {@code null}
     * @throws ConflictException
     *         when token is not valid
     * @throws ServerException
     *         when some error occurred while persisting user or user profile
     * @see User
     * @see #getCurrent(SecurityContext)
     * @see #updatePassword(String, SecurityContext)
     * @see #getById(String, SecurityContext)
     * @see #getByEmail(String, SecurityContext)
     * @see #remove(String)
     * @see com.codenvy.api.user.server.UserProfileService#getCurrent(String, SecurityContext)
     */
    @ApiOperation(value = "Create a new user",
                  notes = "Create a new user in the system",
                  response = User.class,
                  position = 1)
    @ApiResponses(value = {
                  @ApiResponse(code = 201, message = "Created"),
                  @ApiResponse(code = 401, message = "Missed token parameter"),
                  @ApiResponse(code = 409, message = "Invalid token"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/create")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_USER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@ApiParam(value = "Authentication token", required = true)
                           @Required @QueryParam("token") String token,
                           @ApiParam(value = "User type")
                           @Description("is user temporary") @QueryParam("temporary") boolean isTemporary,
                           @Context SecurityContext securityContext) throws UnauthorizedException, ConflictException, ServerException {
        if (token == null) {
            throw new UnauthorizedException("Missed token parameter");
        }
        final String userEmail = tokenValidator.validateToken(token);
        final String userId = NameGenerator.generate(User.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final User user = DtoFactory.getInstance().createDto(User.class)
                                    .withId(userId)
                                    .withEmail(userEmail)
                                    .withPassword(NameGenerator.generate("pass", Constants.PASSWORD_LENGTH));
        userDao.create(user);
        final Map<String, String> attributes = new HashMap<>(4);
        attributes.put("temporary", String.valueOf(isTemporary));
        attributes.put("codenvy:created", Long.toString(System.currentTimeMillis()));
        final Profile profile = new Profile().withId(userId)
                                             .withUserId(userId)
                                             .withAttributes(attributes);
        profileDao.create(profile);
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return Response.status(Response.Status.CREATED)
                       .entity(user)
                       .build();
    }

    /**
     * Returns current {@link User}.
     *
     * @return entity of current user.
     * @throws ServerException
     *         when some error occurred while retrieving current user
     * @see User
     * @see #updatePassword(String, SecurityContext)
     */
    @ApiOperation(value = "Get current user",
                  notes = "Get user currently logged in the system",
                  response = User.class,
                  position = 2)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER)
    @RolesAllowed({"user", "temp_user"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrent(@Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    /**
     * Updates current user password.
     *
     * @param password
     *         new user password
     * @throws ForbiddenException
     *         when given password is {@code null}
     * @throws ServerException
     *         when some error occurred while updating profile
     * @see User
     */
    @ApiOperation(value = "Update password",
                  notes = "Update current password",
                  position = 3)
    @ApiResponses(value = {
                  @ApiResponse(code = 204, message = "OK"),
                  @ApiResponse(code = 403, message = "Password required"),
                  @ApiResponse(code = 404, message = "Not Found"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/password")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PASSWORD)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updatePassword(@ApiParam(value = "New password", required = true)
                               @FormParam("password") String password,
                               @Context SecurityContext securityContext) throws ForbiddenException, NotFoundException, ServerException {
        if (password == null) {
            throw new ForbiddenException("Password required");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        user.setPassword(password);
        userDao.update(user);
    }

    /**
     * Searches for {@link User} with given identifier.
     *
     * @param id
     *         identifier to search user
     * @return entity of found user
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user
     * @see User
     * @see #getByEmail(String, SecurityContext)
     */
    @ApiOperation(value = "Get user by ID",
                  notes = "Get user by its ID in the system. Roles allowed: system/admin, system/manager.",
                  response = User.class,
                  position = 4)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 404, message = "Not Found"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_USER_BY_ID)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getById(@ApiParam(value = "User ID", required = true)
                        @PathParam("id") String id, @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final User user = userDao.getById(id);
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    /**
     * Searches for {@link User} with given email.
     *
     * @param email
     *         email to search user
     * @return entity of found user
     * @throws ForbiddenException
     *         when given email is {@code null}
     * @throws NotFoundException
     *         when user with given email doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user
     * @see User
     * @see #getById(String, SecurityContext)
     * @see #remove(String)
     */
    @ApiOperation(value = "Get user by email",
                  notes = "Get user by registration email. Roles allowed: system/admin, system/manager.",
                  response = User.class,
                  position = 5)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 403, message = "Missed parameter email"),
                  @ApiResponse(code = 404, message = "Not Found"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find")
    @GenerateLink(rel = Constants.LINK_REL_GET_USER_BY_EMAIL)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getByEmail(@ApiParam(value = "User email", required = true)
                           @Required @Description("user email") @QueryParam("email") String email,
                           @Context SecurityContext securityContext) throws ForbiddenException, NotFoundException, ServerException {
        if (email == null) {
            throw new ForbiddenException("Missed parameter email");
        }
        final User user = userDao.getByAlias(email);
        user.setPassword("<none>");
        injectLinks(user, securityContext);
        return user;
    }

    /**
     * Removes user with given identifier.
     *
     * @param id
     *         identifier to remove user
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while removing user
     * @throws ConflictException
     *         when some error occurred while removing user
     */
    @ApiOperation(value = "Delete user",
                  notes = "Delete a user from the system. Roles allowed: system/admin.",
                  position = 6)
    @ApiResponses(value = {
                  @ApiResponse(code = 204, message = "Deleted"),
                  @ApiResponse(code = 404, message = "Not Found"),
                  @ApiResponse(code = 409, message = "Impossible to remove user"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_USER_BY_ID)
    @RolesAllowed("system/admin")
    public void remove(@ApiParam(value = "User ID", required = true)
                       @PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        userDao.remove(id);
    }

    private void injectLinks(User user, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_CURRENT_USER_PROFILE,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder().path(UserProfileService.class)
                                                    .path(UserProfileService.class, "getCurrent")
                                                    .build()
                                                    .toString()));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_CURRENT_USER,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getCurrent")
                                           .build()
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_UPDATE_PASSWORD,
                                 MediaType.APPLICATION_FORM_URLENCODED,
                                 null,
                                 uriBuilder.clone()
                                           .path(getClass(), "updatePassword")
                                           .build()
                                           .toString()));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_USER_BY_ID,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(user.getId())
                                           .toString()));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_USER_PROFILE_BY_ID,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 getServiceContext().getBaseUriBuilder()
                                                    .path(UserProfileService.class).path(UserProfileService.class, "getById")
                                                    .build(user.getId())
                                                    .toString()));
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_USER_BY_EMAIL,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getByEmail")
                                           .queryParam("email", user.getEmail())
                                           .build()
                                           .toString()));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE",
                                 Constants.LINK_REL_REMOVE_USER_BY_ID,
                                 null,
                                 null,
                                 uriBuilder.clone()
                                           .path(getClass(), "remove")
                                           .build(user.getId())
                                           .toString()));
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