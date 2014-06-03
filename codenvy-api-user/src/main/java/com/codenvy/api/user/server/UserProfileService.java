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


import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.*;

import static com.codenvy.commons.lang.Strings.nullToEmpty;

/**
 * User profile API
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
    @RolesAllowed("user")
    @GenerateLink(rel = "current profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getCurrent(@Context SecurityContext securityContext,
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
        final List<Attribute> attrs = new ArrayList<>();
        if (profile.getAttributes() != null) {
            attrs.addAll(profile.getAttributes());
        }
        attrs.add(DtoFactory.getInstance().createDto(Attribute.class)
                            .withDescription("User email")
                            .withName("email")
                            .withValue(user.getEmail()));
        profile.setAttributes(attrs);
        injectLinks(profile, securityContext);
        return profile;
    }

    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = "update current")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateCurrent(@Context SecurityContext securityContext, @Description("updates for profile") List<Attribute> updates)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User user = userDao.getByAlias(principal.getName());
        final Profile profile = profileDao.getById(user.getId());
        if (updates != null) {
            Map<String, Attribute> m = new LinkedHashMap<>(updates.size());
            for (Attribute attribute : updates) {
                m.put(attribute.getName(), attribute);
            }
            for (Iterator<Attribute> i = profile.getAttributes().iterator(); i.hasNext(); ) {
                Attribute attribute = i.next();
                if (m.containsKey(attribute.getName())) {
                    i.remove();
                }
            }
            profile.getAttributes().addAll(updates);
        } else {
            //if updates are null - clear profile attributes
            profile.setAttributes(new ArrayList<Attribute>());
        }
        profileDao.update(profile);
        injectLinks(profile, securityContext);

        logEventUserUpdateProfile(user, profile.getAttributes());

        return profile;
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @GenerateLink(rel = "get by id")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getById(@PathParam("id") String profileId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        final Profile profile = profileDao.getById(profileId);
        final User user = userDao.getById(profile.getUserId());
        profile.setPreferences(Collections.<String, String>emptyMap());
        List<Attribute> attrs = new ArrayList<>();
        if (profile.getAttributes() != null) {
            attrs.addAll(profile.getAttributes());
        }
        attrs.add(DtoFactory.getInstance().createDto(Attribute.class)
                            .withDescription("User email")
                            .withName("email")
                            .withValue(user.getEmail()));
        profile.setAttributes(attrs);
        injectLinks(profile, securityContext);
        return profile;
    }

    @POST
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile update(@PathParam("id") String profileId, @Required @Description("updates for profile") List<Attribute> updates,
                          @Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        Profile profile = profileDao.getById(profileId);
        //if updates are not null, append it to existed attributes
        if (updates != null) {
            Map<String, Attribute> m = new LinkedHashMap<>(updates.size());
            for (Attribute attribute : updates) {
                m.put(attribute.getName(), attribute);
            }
            for (Iterator<Attribute> i = profile.getAttributes().iterator(); i.hasNext(); ) {
                Attribute attribute = i.next();
                if (m.containsKey(attribute.getName())) {
                    i.remove();
                }
            }
            profile.getAttributes().addAll(updates);
        } else {
            //if updates are null - clear profile attributes
            profile.setAttributes(new ArrayList<Attribute>());
        }
        profileDao.update(profile);
        injectLinks(profile, securityContext);

        final User user = userDao.getById(profile.getUserId());
        logEventUserUpdateProfile(user, profile.getAttributes());

        return profile;
    }

    @POST
    @Path("prefs")
    @RolesAllowed({"user"})
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_PREFERENCES)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updatePreferences(@Context SecurityContext securityContext,
                                     @Description("preferences to update") Map<String, String> preferencesToUpdate)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final Profile currentProfile = profileDao.getById(current.getId());
        //if given preferences are not null append it to existed preferences
        if (preferencesToUpdate != null) {
            Map<String, String> currentPreferences = currentProfile.getPreferences();
            currentPreferences.putAll(preferencesToUpdate);
            currentProfile.setPreferences(currentPreferences);
        } else {
            //if given preferences are null - clear profile preferences
            currentProfile.setPreferences(new HashMap<String, String>());
        }
        profileDao.update(currentProfile);
        injectLinks(currentProfile, securityContext);
        return currentProfile;
    }

    @DELETE
    @Path("prefs")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removePreference(@Required List<String> prefNames, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final Profile currentProfile = profileDao.getById(current.getId());
        if (prefNames == null) {
            throw new ServerException("Preferences names required");
        }
        Map<String, String> currentPreferences = currentProfile.getPreferences();
        for (String pref : prefNames) {
            currentPreferences.remove(pref);
        }
        profileDao.update(currentProfile);
    }

    private void injectLinks(Profile profile, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_USER_PROFILE, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE, MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updateCurrent").build().toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withDescription("update profile")
                                                                      .withRequired(true)
                                                                      .withType(ParameterType.Object))));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_PREFERENCES, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updatePreferences").build().toString()));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_USER_PROFILE_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(profile.getId()).toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID, MediaType.APPLICATION_JSON,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "update").build(profile.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withDescription("update profile")
                                                                      .withRequired(true)
                                                                      .withType(ParameterType.Object))));
        }
        profile.setLinks(links);
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href);
    }

    private void logEventUserUpdateProfile(User user, List<Attribute> attributes) {
        Map<String, String> m = new LinkedHashMap<>(attributes.size());
        for (Attribute attribute : attributes) {
            m.put(attribute.getName(), attribute.getValue());
        }

        Set<String> emails = new HashSet<>(user.getAliases());
        emails.add(user.getEmail());

        LOG.info("EVENT#user-update-profile# USER#{}# FIRSTNAME#{}# LASTNAME#{}# COMPANY#{}# PHONE#{}# JOBTITLE#{}# EMAILS#{}# USER-ID#{}#",
                 user.getEmail(),
                 nullToEmpty(m.get("firstName")),
                 nullToEmpty(m.get("lastName")),
                 nullToEmpty(m.get("employer")),
                 nullToEmpty(m.get("phone")),
                 nullToEmpty(m.get("jobtitle")),
                 user.getAliases(),
                 user.getId());
    }
}