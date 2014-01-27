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
import com.codenvy.api.organization.shared.dto.Account;

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
import java.util.List;

/**
 * TODO
 * Account API
 *
 * @author Eugene Voevodin
 */
@Path("/account")
public class AccountService extends Service {

    @POST
    @GenerateLink(rel = "create")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Account newAccount) {
        Account account = null;
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = "get by id")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getById(@PathParam("id") String id) {
        Account account = null;
        return account;
    }

    @GET
    @GenerateLink(rel = "get by name")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getByName(@QueryParam("name") String name) {
        Account account = null;
        return account;
    }

    @GET
    @Path("all")
    @GenerateLink(rel = "all of current user")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Account> getAll(@Context SecurityContext securityContext) {
        List<Account> accounts = null;
        return accounts;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = "all by user id")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Account> getSpecificUserAccounts(@QueryParam("userid") String userId) {
        List<Account> accounts = null;
        return accounts;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = "update")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Account update(@PathParam("id") String id, Account newAccount) {
        Account account = null;
        return account;
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = "remove")
    @RolesAllowed("system/admin")
    public Response remove(@PathParam("id") String id) {
        return Response.noContent().build();
    }
}