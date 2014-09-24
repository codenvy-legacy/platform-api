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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.ProfileDescriptor;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.commons.env.EnvironmentContext;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.api.user.server.Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE;
import static com.codenvy.api.user.server.Constants.LINK_REL_GET_CURRENT_USER_PROFILE;
import static com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_PROFILE_BY_ID;
import static com.codenvy.api.user.server.Constants.LINK_REL_REMOVE_ATTRIBUTES;
import static com.codenvy.api.user.server.Constants.LINK_REL_REMOVE_PREFERENCES;
import static com.codenvy.api.user.server.Constants.LINK_REL_UPDATE_PREFERENCES;
import static com.codenvy.api.user.server.Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID;
import static com.codenvy.commons.lang.Strings.nullToEmpty;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * User Profile API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Api(value = "/profile",
     description = "User profile manager")
@Path("/profile")
public class UserProfileService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileDao profileDao;
    private final UserDao        userDao;

    @Inject
    public UserProfileService(UserProfileDao profileDao, UserDao userDao) {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    /**
     * <p>Returns {@link ProfileDescriptor} for current user profile.</p>
     * <p>By default user email will be added to attributes with key <i>'email'</i>.</p>
     *
     * @param filter
     *         preferences filter regex, if it is not {@code null}
     *         only preferences matched to filter will be fetched
     * @return descriptor of profile
     * @throws ServerException
     *         when some error occurred while retrieving/updating profile
     * @see ProfileDescriptor
     * @see #updateCurrent(Map, SecurityContext)
     * @see #updatePreferences(Map, SecurityContext)
     */
    @ApiOperation(value = "Get user profile",
                  notes = "Get user profile details",
                  response = ProfileDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @RolesAllowed({"user", "temp_user"})
    @GenerateLink(rel = LINK_REL_GET_CURRENT_USER_PROFILE)
    @Produces(APPLICATION_JSON)
    public ProfileDescriptor getCurrent(@ApiParam(value = "Search filter. Regex can be used")
                                        @Description("Preferences path filter")
                                        @QueryParam("filter")
                                        String filter,
                                        @Context SecurityContext context) throws NotFoundException, ServerException {
        final User user = userDao.getById(currentUser().getId());
        final Profile profile;
        if (filter == null) {
            profile = profileDao.getById(user.getId());
        } else {
            profile = profileDao.getById(user.getId(), filter);
        }
        profile.getAttributes().put("email", user.getEmail());
        return toDescriptor(profile, context);
    }

    /**
     * <p>Updates attributes of current user profile.</p>
     * <p><strong>Note:</strong>if updates are {@code null} or <i>empty</i>
     * then all current user profile attributes will be removed,
     * existed profile attributes with same names as update attributes
     * will be replaced with update attributes.</p>
     *
     * @param updates
     *         attributes to update
     * @return descriptor of updated profile
     * @throws ServerException
     *         when some error occurred while retrieving/persisting profile
     * @see ProfileDescriptor
     * @see #updatePreferences(Map, SecurityContext)
     */
    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = LINK_REL_UPDATE_CURRENT_USER_PROFILE)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public ProfileDescriptor updateCurrent(@Description("attributes to update") Map<String, String> updates,
                                           @Context SecurityContext context) throws NotFoundException, ServerException {
        final User user = userDao.getById(currentUser().getId());
        final Profile profile = profileDao.getById(user.getId());
        //if updates are null or empty - clear profile attributes
        if (updates == null || updates.isEmpty()) {
            profile.getAttributes().clear();
        } else {
            profile.getAttributes().putAll(updates);
        }
        profileDao.update(profile);
        logEventUserUpdateProfile(user, profile.getAttributes());
        return toDescriptor(profile, context);
    }


    /**
     * <p>Updates attributes of certain profile.</p>
     * <p><strong>Note:</strong>if updates are {@code null} or <i>empty</i>
     * then all current user profile attributes will be removed,
     * existed profile attributes with same names as update attributes
     * will be replaced with update attributes.</p>
     *
     * @param profileId
     *         profile identifier
     * @param updates
     *         attributes to update
     * @return descriptor of updated profile
     * @throws NotFoundException
     *         when profile with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving/updating profile
     * @see ProfileDescriptor
     * @see #getById(String, SecurityContext)
     */

    @POST
    @Path("/{id}")
    @RolesAllowed({"system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public ProfileDescriptor update(@PathParam("id") String profileId,
                                    Map<String, String> updates,
                                    @Context SecurityContext context) throws NotFoundException, ServerException {
        final Profile profile = profileDao.getById(profileId);
        //if updates are null or empty - clear profile attributes
        if (updates == null || updates.isEmpty()) {
            profile.getAttributes().clear();
        } else {
            profile.getAttributes().putAll(updates);
        }
        profileDao.update(profile);
        final User user = userDao.getById(profile.getUserId());
        logEventUserUpdateProfile(user, profile.getAttributes());
        return toDescriptor(profile, context);
    }

    /**
     * Searches for profile with given identifier and {@link ProfileDescriptor} if found.
     *
     * @param profileId
     *         profile identifier
     * @return descriptor of found profile
     * @throws NotFoundException
     *         when profile with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user or profile
     * @see ProfileDescriptor
     * @see #getById(String, SecurityContext)
     */
    @ApiOperation(value = "Get profile of a specific user",
                  notes = "Get profile of a specific user. Roles allowed: system/admin, system/manager",
                  response = ProfileDescriptor.class,
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public ProfileDescriptor getById(@ApiParam(value = "User ID")
                                     @PathParam("id")
                                     String profileId,
                                     @Context SecurityContext context) throws NotFoundException, ServerException {
        final Profile profile = profileDao.getById(profileId);
        final User user = userDao.getById(profile.getUserId());
        profile.getPreferences().clear();
        profile.getAttributes().put("email", user.getEmail());
        return toDescriptor(profile, context);
    }

    /**
     * <p>Updates preferences of current user profile.</p>
     * <p><strong>Note:</strong>if updates are {@code null} or <i>empty</i>
     * then all current user profile preferences will be removed,
     * existed profile preferences with same names as update preferences
     * will be replaced with update preferences.</p>
     *
     * @param preferencesToUpdate
     *         update preferences
     * @return descriptor of updated profile
     * @throws ServerException
     *         when some error occurred while retrieving/updating profile
     * @see ProfileDescriptor
     * @see #updateCurrent(Map, SecurityContext)
     */
    @POST
    @Path("/prefs")
    @RolesAllowed({"user", "temp_user"})
    @GenerateLink(rel = LINK_REL_UPDATE_PREFERENCES)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public ProfileDescriptor updatePreferences(@Description("preferences to update") Map<String, String> preferencesToUpdate,
                                               @Context SecurityContext context) throws NotFoundException, ServerException {
        final Profile currentProfile = profileDao.getById(currentUser().getId());
        //if given preferences are null or empty - clear profile preferences
        if (preferencesToUpdate == null || preferencesToUpdate.isEmpty()) {
            currentProfile.getPreferences().clear();
        } else {
            currentProfile.getPreferences().putAll(preferencesToUpdate);
        }
        profileDao.update(currentProfile);
        return toDescriptor(currentProfile, context);
    }

    /**
     * Removes attributes with given names from current user profile.
     *
     * @param attrNames
     *         attributes names to remove
     * @throws ConflictException
     *         when given list of attributes names is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving/updating profile
     * @see #removePreferences(List, SecurityContext)
     */

    @ApiOperation(value = "Remove attributes of a current user",
                  notes = "Remove attributes of a current user",
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Attributes names required"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/attributes")
    @GenerateLink(rel = LINK_REL_REMOVE_ATTRIBUTES)
    @RolesAllowed({"user", "temp_user"})
    @Consumes(APPLICATION_JSON)
    public void removeAttributes(@ApiParam(value = "Attributes", required = true)
                                 @Required
                                 @Description("Attributes names to remove")
                                 List<String> attrNames,
                                 @Context SecurityContext context) throws NotFoundException, ServerException, ConflictException {
        if (attrNames == null) {
            throw new ConflictException("Attributes names required");
        }
        final Profile currentProfile = profileDao.getById(currentUser().getId());
        final Map<String, String> attributes = currentProfile.getAttributes();
        for (String attributeName : attrNames) {
            attributes.remove(attributeName);
        }
        profileDao.update(currentProfile);
    }

    /**
     * Removes preferences with given name from current user profile.
     *
     * @param prefNames
     *         preferences names to remove
     * @throws ConflictException
     *         when given list of preferences names is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving/updating profile
     * @see #removeAttributes(List, SecurityContext)
     */
    @ApiOperation(value = "Remove profile references of a current user",
                  notes = "Remove profile references of a current user",
                  position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Preferences names required"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/prefs")
    @GenerateLink(rel = LINK_REL_REMOVE_PREFERENCES)
    @RolesAllowed({"user", "temp_user"})
    @Consumes(APPLICATION_JSON)
    public void removePreferences(@ApiParam(value = "Preferences to remove", required = true)
                                  @Required List<String> prefNames,
                                  @Context SecurityContext context) throws NotFoundException, ConflictException, ServerException {
        if (prefNames == null) {
            throw new ConflictException("Preferences names required");
        }
        final Profile currentProfile = profileDao.getById(currentUser().getId());
        final Map<String, String> preferences = currentProfile.getPreferences();
        for (String prefName : prefNames) {
            preferences.remove(prefName);
        }
        profileDao.update(currentProfile);
    }

    /**
     * Converts {@link Profile} to {@link ProfileDescriptor}
     */
    /* package-private used in tests*/ProfileDescriptor toDescriptor(Profile profile, SecurityContext context) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new LinkedList<>();
        if (context.isUserInRole("user")) {
            links.add(createLink("GET",
                                 LINK_REL_GET_CURRENT_USER_PROFILE,
                                 null,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getCurrent")
                                           .build()
                                           .toString()));
            links.add(createLink("GET",
                                 LINK_REL_GET_USER_PROFILE_BY_ID,
                                 null,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(profile.getId())
                                           .toString()));
            links.add(createLink("POST",
                                 LINK_REL_UPDATE_CURRENT_USER_PROFILE,
                                 APPLICATION_JSON,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "updateCurrent")
                                           .build()
                                           .toString()));
            links.add(createLink("POST",
                                 LINK_REL_UPDATE_PREFERENCES,
                                 APPLICATION_JSON,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "updatePreferences")
                                           .build()
                                           .toString()));
        }
        if (context.isUserInRole("system/admin") || context.isUserInRole("system/manager")) {
            links.add(createLink("GET",
                                 LINK_REL_GET_USER_PROFILE_BY_ID,
                                 null,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(profile.getId())
                                           .toString()));
        }
        if (context.isUserInRole("system/admin")) {
            links.add(createLink("POST",
                                 LINK_REL_UPDATE_USER_PROFILE_BY_ID,
                                 APPLICATION_JSON,
                                 APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "update")
                                           .build(profile.getId())
                                           .toString()));
        }
        return DtoFactory.getInstance().createDto(ProfileDescriptor.class)
                         .withId(profile.getId())
                         .withUserId(profile.getUserId())
                         .withAttributes(profile.getAttributes())
                         .withPreferences(profile.getPreferences())
                         .withLinks(links);
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }

    private com.codenvy.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }

    private void logEventUserUpdateProfile(User user, Map<String, String> attributes) {
        final Set<String> emails = new HashSet<>(user.getAliases());
        emails.add(user.getEmail());

        LOG.info("EVENT#user-update-profile# USER#{}# FIRSTNAME#{}# LASTNAME#{}# COMPANY#{}# PHONE#{}# JOBTITLE#{}# EMAILS#{}# USER-ID#{}#",
                 user.getEmail(),
                 nullToEmpty(attributes.get("firstName")),
                 nullToEmpty(attributes.get("lastName")),
                 nullToEmpty(attributes.get("employer")),
                 nullToEmpty(attributes.get("phone")),
                 nullToEmpty(attributes.get("jobtitle")),
                 user.getAliases(),
                 user.getId());
    }
}