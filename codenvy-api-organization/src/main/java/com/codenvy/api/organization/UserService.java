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
import com.codenvy.api.organization.dao.UserDao;
import com.codenvy.api.organization.dao.UserProfileDao;
import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.User;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User API
 *
 * @author Eugene Voevodin
 */
@Path("/user")
public class UserService extends Service {

    private final UserDao        userDao;
    private final UserProfileDao profileDao;

    @Inject
    public UserService(UserDao userDao, UserProfileDao profileDao) {
        this.userDao = userDao;
        this.profileDao = profileDao;
    }

    @POST
    @Path("create")
    @GenerateLink(rel = "create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@QueryParam("token") String token, @QueryParam("temporary") Boolean isTemporary)
            throws OrganizationServiceException {
        //TODO
        User user = null;
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @GET
    @GenerateLink(rel = "self")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User getCurrent() throws OrganizationServiceException {
        //TODO
        User user = null;
        return user;
    }

    @POST
    @Path("password")
    @GenerateLink(rel = "update password")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePassword(String password) throws OrganizationServiceException {
        //TODO
        User user = null;
        return Response.noContent().build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "self by id")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getById(@PathParam("id") String id) throws OrganizationServiceException {
        //TODO
        User user = null;
        return user;
    }

    @GET
    @Path("/find")
    @GenerateLink(rel = "self by email")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public User getByEmail(@QueryParam("email") String email) throws OrganizationServiceException {
        //TODO
        User user = null;
        return user;
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = "remove")
    @RolesAllowed("system/admin")
    public Response removeById(@PathParam("id") String id) throws OrganizationServiceException {
        //TODO
        User user = null;
        return Response.noContent().build();
    }
}
