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
package com.codenvy.api.user;


import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.user.dao.UserDao;
import com.codenvy.api.user.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User profile API
 *
 * @author Eugene Voevodin
 */
@Path("/profile")
public class UserProfileService extends Service {

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
    public Profile getCurrent(@Context SecurityContext securityContext) throws ApiException {
        Principal principal = securityContext.getUserPrincipal();
        User current = userDao.getByAlias(principal.getName());
        Profile profile = profileDao.getById(current.getProfileId());
        injectLinks(profile, securityContext);
        return profile;
    }

    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = "update current")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateCurrent(@Context SecurityContext securityContext,
                                 @Required @Description("new user profile to update") Profile newProfile)
            throws ApiException {
        Principal principal = securityContext.getUserPrincipal();
        User user = userDao.getByAlias(principal.getName());
        newProfile.setUserId(user.getId());
        newProfile.setId(user.getProfileId());
        profileDao.update(newProfile);
        injectLinks(newProfile, securityContext);
        return newProfile;
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "get by id")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getById(@PathParam("id") String profileId, @Context SecurityContext securityContext) throws ApiException {
        Profile profile = profileDao.getById(profileId);
        injectLinks(profile, securityContext);
        return profile;
    }

    @POST
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateById(@PathParam("id") String profileId, @Required @Description("new user profile to update") Profile newProfile, @Context SecurityContext securityContext)
            throws ApiException {
        profileDao.update(newProfile);
        injectLinks(newProfile, securityContext);
        return newProfile;
    }


    private void injectLinks(Profile profile, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("user")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_USER_PROFILE, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updateCurrent").build().toString())
                                 .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withDescription("update profile")
                                                                      .withRequired(true)
                                                                      .withType(ParameterType.Object))));
        }

        if (isUserInAnyRole(securityContext, "system/admin", "system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_USER_PROFILE_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(profile.getId()).toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE__USER_PROFILE_BY_ID, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "updateById").build(profile.getId()).toString())
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

    private boolean isUserInAnyRole(SecurityContext securityContext, String... roles) {
        boolean isUserInAnyRole = false;
        for (String role : roles) {
            isUserInAnyRole |= securityContext.isUserInRole(role);
        }
        return isUserInAnyRole;
    }
}