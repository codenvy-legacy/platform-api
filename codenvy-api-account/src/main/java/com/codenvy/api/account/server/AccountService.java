/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.api.account.server;


import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.user.User;
import com.codenvy.dto.server.DtoFactory;

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
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
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
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Account newAccount) {
        Account account = null;
        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_ID)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getById(@PathParam("id") String id) {
        Account account = null;
        return account;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getMembers(@Context SecurityContext securityContext) {
        //todo when user in role "account/owner" do not forget to add validation that account with {id} has owner = current user
        final List<User> users = null;
        return users;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void addMember(@Context SecurityContext securityContext) {
        //todo when user in role "account/owner" do not forget to add validation that account with {id} has owner = current user
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeMember(@Context SecurityContext securityContext) {
        //todo when user in role "account/owner" do not forget to add validation that account with {id} has owner = current user
    }


    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getByName(@QueryParam("name") String name) {
        Account account = null;
        return account;
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ACCOUNT)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Account update(@PathParam("id") String id, Account newAccount) {
        Account account = null;
        return account;
    }

    @GET
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed("account/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptions(@Context SecurityContext securityContext) {
        List<Subscription> subscriptions = null;
        return subscriptions;
    }

    @GET
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS_OF_SPECIFIC_ACCOUNT)
    @RolesAllowed({"system/admin", "system/manager"})
    public List<Subscription> getSubscriptionsOfSpecificAccount() {
        List<Subscription> subscriptions = null;
        return subscriptions;
    }

    @POST
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void addSubscription(@PathParam("id") String id, Subscription subscription) {
    }

    @DELETE
    @Path("{id}/subscriptions/{subscription}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") String accountId, @PathParam("subscription") String subscriptionId) {
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ACCOUNT)
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) {
    }

    private void injectLinks(SecurityContext securityContext, Account account) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        account.setLinks(links);
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