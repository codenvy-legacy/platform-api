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


import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.exception.AccountIllegalAccessException;
import com.codenvy.api.account.server.exception.AccountException;
import com.codenvy.api.account.server.exception.AccountNotFoundException;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.LinkParameter;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO
 * Account API
 *
 * @author Eugene Voevodin
 */
@Path("/account")
public class AccountService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private final AccountDao accountDao;
    private final UserDao    userDao;

    @Inject
    public AccountService(AccountDao accountDao, UserDao userDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @Required @Description("New account") Account newAccount)
            throws AccountException, UserException {
        String accountId = NameGenerator.generate(Account.class.getSimpleName(), Constants.ID_LENGTH);
        newAccount.setId(accountId);
        final Principal principal = securityContext.getUserPrincipal();
        //account should have owner
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        accountDao.create(newAccount);
        injectLinks(newAccount, securityContext);
        return Response.status(Response.Status.CREATED).entity(newAccount).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_ACCOUNT)
    @RolesAllowed("account/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public Account getCurrent(@Context SecurityContext securityContext) {
        return null;
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_ID)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getById(@Context SecurityContext securityContext, @PathParam("id") String id) throws AccountException, UserException {
        final Account account = accountDao.getById(id);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        if (securityContext.isUserInRole("account/owner")) {
            User owner = userDao.getByAlias(securityContext.getUserPrincipal().getName());
            if (owner == null) {
                throw new UserNotFoundException(securityContext.getUserPrincipal().getName());
            }
            if (!account.getOwner().equals(owner.getId())) {
                throw new AccountIllegalAccessException(account.getId());
            }
        }
        injectLinks(account, securityContext);
        return account;
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getByName(@Context SecurityContext securityContext,
                             @Required @Description("Account name to search") @QueryParam("name") String name)
            throws AccountException, UserException {
        Account account = accountDao.getByName(name);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithName(name);
        }
        injectLinks(account, securityContext);
        return account;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getMembersOfSpecificAccount(@PathParam("id") String id) throws AccountException, UserException {
//        final Account account = accountDao.getById(id);
//        if (account == null) {
//            throw AccountNotFoundException.doesNotExistWithId(id);
//        }
//        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
//        final List<User> members = new ArrayList<>(memberIdentifiers.size());
//        for (String userid : memberIdentifiers) {
//            User member = userDao.getById(userid);
//            if (member == null) {
//                LOG.error(String.format("User with id %s is account member, but he doesn't exist!", userid));
//                continue;
//            }
//            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
//                                                     uriBuilder.clone().path(getClass(), "removeMember").build(account.getId(), userid)
//                                                               .toString())));
//            members.add(member);
//        }
        return null;
    }

    @GET
    @Path("members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getMembers(@Context SecurityContext securityContext) throws UserException, AccountException {
//        final Principal principal = securityContext.getUserPrincipal();
//        final User current = userDao.getByAlias(principal.getName());
//        if (current == null) {
//            throw new UserNotFoundException(principal.getName());
//        }
//        final Account account = accountDao.getByOwner(current.getId());
//        if (account == null) {
//            throw AccountNotFoundException.doesNotExistWithOwner(current.getId());
//        }
//        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
//        final List<String> memberIdentifiers = account.();
//        final List<User> members = new ArrayList<>(memberIdentifiers.size());
//        for (String userid : memberIdentifiers) {
//            User member = userDao.getById(userid);
//            if (member == null) {
//                LOG.error(String.format("User with id %s is account member, but he doesn't exist!", userid));
//                continue;
//            }
//            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
//                                                     uriBuilder.clone().path(getClass(), "removeMember").build(account.getId(), userid)
//                                                               .toString())));
//            members.add(member);
//        }
//        return members;
        return null;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void addMember(@Context SecurityContext securityContext, @PathParam("id") String accountId,
                          @Required @Description("New account member") @QueryParam("userid") String userId)
            throws UserException, AccountException {
        final Account account = accountDao.getById(accountId);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        if (securityContext.isUserInRole("account/owner")) {
            User owner = userDao.getByAlias(securityContext.getUserPrincipal().getName());
            if (owner == null) {
                throw new UserNotFoundException(securityContext.getUserPrincipal().getName());
            }
            if (!account.getOwner().equals(owner.getId())) {
                throw new AccountIllegalAccessException(account.getId());
            }
        }
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        accountDao.addMember(accountId, userId);
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_MEMBER)
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void removeMember(@Context SecurityContext securityContext, @PathParam("id") String accountId,
                             @PathParam("userid") String userid) throws AccountException, UserException {
        final Account account = accountDao.getById(accountId);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        accountDao.removeMember(accountId, userid);
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ACCOUNT)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Account update(@PathParam("id") String id, @Required @Description("Account to update") Account accountToUpdate)
            throws AccountException {
        Account actual = accountDao.getById(id);
        if (actual == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        accountToUpdate.setId(id);
        //todo append attributes
        return actual;
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
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
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

    private void injectLinks(Account account, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();

        if (securityContext.isUserInRole("account/owner")) {
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_ACCOUNT, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "update").build(account.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withName("accountToUpdate")
                                                                      .withRequired(true)
                                                                      .withDescription("Account to update"))));
            links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptions").build().toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembers").build().toString()));
        }
        if (securityContext.isUserInRole("account/owner") || securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_ACCOUNT_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(account.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_ACCOUNT_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", account.getName()).build()
                                           .toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembersOfSpecificAccount").build(account.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionsOfSpecificAccount").build(account.getId())
                                           .toString()));

        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink("DELETE", Constants.LINK_REL_REMOVE_ACCOUNT, null, null,
                                 uriBuilder.clone().path(getClass(), "remove").build(account.getId()).toString()));
        }
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