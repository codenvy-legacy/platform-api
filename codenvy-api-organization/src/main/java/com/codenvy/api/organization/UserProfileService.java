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
import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Profile;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * User profile API
 *
 * @author Eugene Voevodin
 */
@Path("/profile")
public class UserProfileService extends Service {

    private final UserProfileDao profileDao;

    @Inject
    public UserProfileService(UserProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    @GET
    @RolesAllowed("user")
    @GenerateLink(rel = "current profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getCurrent() throws OrganizationServiceException {
        //TODO
        Profile profile = null;
        return profile;
    }

    @POST
    @RolesAllowed("user")
    @GenerateLink(rel = "update current profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateCurrent(Profile newProfile) throws OrganizationServiceException {
        //TODO
        Profile profile = null;
        return profile;
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "profile by id")
    @Produces(MediaType.APPLICATION_JSON)
    public Profile getById(@PathParam("id") String profileId) throws OrganizationServiceException {
        //TODO
        Profile profile = null;
        return profile;
    }

    @POST
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @GenerateLink(rel = "update by id")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Profile updateById(@PathParam("id") String profileId, Profile newProfile) throws OrganizationServiceException {
        //TODO
        Profile profile = null;
        return profile;
    }
}
