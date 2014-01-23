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
import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Profile;
import com.codenvy.api.organization.shared.dto.User;
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
    public Profile getCurrent(@Context SecurityContext securityContext) throws OrganizationServiceException {
        Principal principal = securityContext.getUserPrincipal();
        User current = userDao.getByAlias(principal.getName());
        Profile profile = profileDao.getById(current.getProfileId());
        final List<Link> links = new ArrayList<>(1);
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withConsumes(MediaType.APPLICATION_JSON)
                            .withMethod("POST")
                            .withRel("update current")
                            .withHref(getServiceContext().getServiceUriBuilder().clone().path(UserProfileService.class, "updateCurrent")
                                              .build().toString()));
        profile.setLinks(links);
        return profile;
    }

    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = "update current")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateCurrent(@Context SecurityContext securityContext, Profile newProfile) throws OrganizationServiceException {
        Principal principal = securityContext.getUserPrincipal();
        User user = userDao.getByAlias(principal.getName());
        newProfile.setUserId(user.getId());
        newProfile.setId(user.getProfileId());
        profileDao.update(newProfile);
        Profile profile = profileDao.getById(newProfile.getId());
        final List<Link> links = new ArrayList<>(1);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withConsumes(MediaType.APPLICATION_JSON)
                            .withMethod("GET")
                            .withRel("get current")
                            .withHref(uriBuilder.clone().path(UserProfileService.class, "getCurrent").build().toString()));
        profile.setLinks(links);
        return profile;
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "get by id")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getById(@PathParam("id") String profileId) throws OrganizationServiceException {
        Profile profile = profileDao.getById(profileId);
        final List<Link> links = new ArrayList<>(1);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withConsumes(MediaType.APPLICATION_JSON)
                            .withMethod("POST")
                            .withRel("update by id")
                            .withHref(uriBuilder.clone().path(UserProfileService.class, "updateById").build(profileId).toString()));
        profile.setLinks(links);
        return profile;
    }

    @POST
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateById(@PathParam("id") String profileId, Profile newProfile)
            throws OrganizationServiceException {
        Profile profile = profileDao.getById(profileId);
        final List<Link> links = new ArrayList<>(1);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withProduces(MediaType.APPLICATION_JSON)
                            .withMethod("GET")
                            .withRel("get by id")
                            .withHref(uriBuilder.clone().path(UserProfileService.class, "getById").build(profileId).toString()));
        profile.setLinks(links);
        return profile;
    }


}