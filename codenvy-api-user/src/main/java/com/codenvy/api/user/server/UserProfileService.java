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
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.ProfileDescriptor;
import com.codenvy.api.user.shared.dto.User;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.commons.lang.Strings.nullToEmpty;

/**
 * User Profile API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Path("profile")
public class UserProfileService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileDao profileDao;
    private final UserDao        userDao;

    @Inject
    public UserProfileService(UserProfileDao profileDao, UserDao userDao) {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    @GET
    @RolesAllowed({"user", "temp_user"})
    @GenerateLink(rel = "current profile")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileDescriptor getCurrent(@Context SecurityContext securityContext,
                                        @Description("Preferences path filter") @QueryParam("filter") String filter)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final Profile profile;
        if (filter == null) {
            profile = profileDao.getById(user.getId());
        } else {
            profile = profileDao.getById(user.getId(), filter);
        }
        profile.getAttributes().put("email", user.getEmail());
        return toDescriptor(profile, securityContext);
    }

    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = "update current")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileDescriptor updateCurrent(@Context SecurityContext securityContext,
                                           @Description("updates for profile") Map<String, String> updates)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final Profile profile = profileDao.getById(user.getId());
        if (updates != null) {
            profile.getAttributes().putAll(updates);
        }
        //if updates are null or empty - clear profile attributes
        if (updates == null || updates.isEmpty()) {
            profile.getAttributes().clear();
        }
        profileDao.update(profile);
        logEventUserUpdateProfile(user, profile.getAttributes());
        return toDescriptor(profile, securityContext);
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileDescriptor getById(@PathParam("id") String profileId,
                                     @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Profile profile = profileDao.getById(profileId);
        final User user = userDao.getById(profile.getUserId());
        profile.getPreferences().clear();
        profile.getAttributes().put("email", user.getEmail());
        return toDescriptor(profile, securityContext);
    }

    @POST
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileDescriptor update(@PathParam("id") String profileId,
                                    Map<String, String> updates,
                                    @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Profile profile = profileDao.getById(profileId);
        //if updates are not null or empty, append it to existed attributes
        if (updates != null) {
            profile.getAttributes().putAll(updates);
        }
        //if updates are null or empty - clear profile attributes
        if (updates == null || updates.isEmpty()) {
            profile.getAttributes().clear();
        }
        profileDao.update(profile);
        final User user = userDao.getById(profile.getUserId());
        logEventUserUpdateProfile(user, profile.getAttributes());
        return toDescriptor(profile, securityContext);
    }

    @POST
    @Path("prefs")
    @RolesAllowed({"user", "temp_user"})
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PREFERENCES)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileDescriptor updatePreferences(@Context SecurityContext securityContext,
                                               @Description("preferences to update") Map<String, String> preferencesToUpdate)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final Profile currentProfile = profileDao.getById(current.getId());
        //if given preferences are not null append it to existed preferences
        if (preferencesToUpdate != null) {
            currentProfile.getPreferences().putAll(preferencesToUpdate);
        }
        //if given preferences are null or empty - clear profile preferences
        if (preferencesToUpdate == null || preferencesToUpdate.isEmpty()) {
            currentProfile.getPreferences().clear();
        }
        profileDao.update(currentProfile);
        return toDescriptor(currentProfile, securityContext);
    }

    @DELETE
    @Path("attributes")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTES)
    @RolesAllowed({"user", "temp_user"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeAttributes(@Required @Description("Attributes names to remove") List<String> attrNames,
                                 @Context SecurityContext securityContext) throws NotFoundException, ServerException, ConflictException {
        if (attrNames == null) {
            throw new ConflictException("Attributes names required");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final Profile currentProfile = profileDao.getById(current.getId());
        final Map<String, String> attributes = currentProfile.getAttributes();
        for (String attributeName : attrNames) {
            attributes.remove(attributeName);
        }
        profileDao.update(currentProfile);
    }

    @DELETE
    @Path("prefs")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_PREFERENCES)
    @RolesAllowed({"user", "temp_user"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void removePreferences(@Required List<String> prefNames,
                                  @Context SecurityContext securityContext) throws NotFoundException, ConflictException, ServerException {
        if (prefNames == null) {
            throw new ConflictException("Preferences names required");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final Profile currentProfile = profileDao.getById(current.getId());
        final Map<String, String> currentPreferences = currentProfile.getPreferences();
        for (String prefName : prefNames) {
            currentPreferences.remove(prefName);
        }
        profileDao.update(currentProfile);
    }

    private ProfileDescriptor toDescriptor(Profile profile, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new LinkedList<>();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_CURRENT_USER_PROFILE,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getCurrent")
                                           .build()
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE,
                                 MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "updateCurrent")
                                           .build()
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_UPDATE_PREFERENCES,
                                 MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "updatePreferences")
                                           .build()
                                           .toString()));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET",
                                 Constants.LINK_REL_GET_USER_PROFILE_BY_ID,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(profile.getId())
                                           .toString()));
            links.add(createLink("POST",
                                 Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID,
                                 MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
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