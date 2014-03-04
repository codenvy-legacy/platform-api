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
import com.codenvy.api.account.server.exception.AccountAlreadyExistsException;
import com.codenvy.api.account.server.exception.AccountIllegalAccessException;
import com.codenvy.api.account.server.exception.AccountException;
import com.codenvy.api.account.server.exception.AccountNotFoundException;
import com.codenvy.api.account.server.exception.ServiceNotFoundException;
import com.codenvy.api.account.server.exception.SubscriptionNotFoundException;
import com.codenvy.api.account.shared.dto.Member;
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
 * Account API
 *
 * @author Eugene Voevodin
 */
@Path("/account")
public class AccountService extends Service {

    private final AccountDao                  accountDao;
    private final UserDao                     userDao;
    private final SubscriptionServiceRegistry registry;

    @Inject
    public AccountService(AccountDao accountDao, UserDao userDao, SubscriptionServiceRegistry registry) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.registry = registry;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext, @Required @Description("Account to create") Account newAccount)
            throws AccountException, UserException {
        if (newAccount == null) {
            throw new AccountException("Missed account to create");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        if (accountDao.getByOwner(current.getId()) != null) {
            throw AccountAlreadyExistsException.existsWithOwner(current.getId());
        }
        if (accountDao.getByName(newAccount.getName()) != null) {
            throw AccountAlreadyExistsException.existsWithName(newAccount.getName());
        }
        String accountId = NameGenerator.generate(Account.class.getSimpleName(), Constants.ID_LENGTH);
        newAccount.setId(accountId);
        //account should have owner
        newAccount.setOwner(current.getId());
        accountDao.create(newAccount);
        injectLinks(newAccount, securityContext);
        return Response.status(Response.Status.CREATED).entity(newAccount).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_ACCOUNT)
    @RolesAllowed("account/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public Account getCurrent(@Context SecurityContext securityContext) throws UserException, AccountException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final Account account = accountDao.getByOwner(current.getId());
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithOwner(current.getId());
        }
        injectLinks(account, securityContext);
        return account;
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
        injectLinks(account, securityContext);
        return account;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getByName(@Context SecurityContext securityContext,
                             @Required @Description("Account name to search") @QueryParam("name") String name)
            throws AccountException, UserException {
        if (name == null) {
            throw new AccountException("Missed account name");
        }
        final Account account = accountDao.getByName(name);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithName(name);
        }
        injectLinks(account, securityContext);
        return account;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void addMember(@Context SecurityContext securityContext, @PathParam("id") String accountId,
                          @Required @Description("User id to be a new account member") @QueryParam("userid") String userId)
            throws UserException, AccountException {
        if (userId == null) {
            throw new AccountException("Missed user id");
        }
        final Account account = accountDao.getById(accountId);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        final Principal principal = securityContext.getUserPrincipal();
        if (securityContext.isUserInRole("account/owner")) {
            User owner = userDao.getByAlias(principal.getName());
            if (!account.getOwner().equals(owner.getId())) {
                throw new AccountIllegalAccessException(account.getId());
            }
        }
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class).withAccountId(accountId).withUserId(userId);
        newMember.setRoles(Arrays.asList("account/member"));
        accountDao.addMember(newMember);
    }

    @GET
    @Path("members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembersOfCurrentAccount(@Context SecurityContext securityContext) throws UserException, AccountException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final Account account = accountDao.getByOwner(current.getId());
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithOwner(current.getId());
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = accountDao.getMembers(account.getId());
        //injecting links
        for (Member member : members) {
            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                     uriBuilder.clone().path(getClass(), "removeMember")
                                                               .build(account.getId(), member.getUserId()).toString())));
        }
        return members;
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembersOfSpecificAccount(@PathParam("id") String id) throws AccountException, UserException {
        if (accountDao.getById(id) == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = accountDao.getMembers(id);
        for (Member member : members) {
            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                     uriBuilder.clone().path(getClass(), "removeMember")
                                                               .build(id, member.getUserId()).toString())));
        }
        return members;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@Context SecurityContext securityContext, @PathParam("id") String accountId,
                             @PathParam("userid") String userid) throws AccountException, UserException {
        final Account account = accountDao.getById(accountId);
        if (accountDao.getById(accountId) == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        if (securityContext.isUserInRole("account/owner")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User current = userDao.getByAlias(principal.getName());
            if (!account.getOwner().equals(current.getId())) {
                throw new AccountIllegalAccessException(current.getId());
            }
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
        if (accountToUpdate == null) {
            throw new AccountException("Missed account to update");
        }
        final Account actual = accountDao.getById(id);
        if (actual == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        if (!actual.getOwner().equals(accountToUpdate.getOwner()) && accountDao.getByOwner(accountToUpdate.getOwner()) != null) {
            throw AccountAlreadyExistsException.existsWithOwner(accountToUpdate.getOwner());
        }
        if (!actual.getName().equals(accountToUpdate.getName()) && accountDao.getByName(accountToUpdate.getName()) != null) {
            throw AccountAlreadyExistsException.existsWithName(accountToUpdate.getName());
        }
        accountToUpdate.setId(id);
        accountDao.update(accountToUpdate);
        //todo append attributes!?
        return actual;
    }

    @GET
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed("account/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptionsOfCurrentAccount(@Context SecurityContext securityContext)
            throws UserException, AccountException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final Account account = accountDao.getByOwner(current.getId());
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithOwner(current.getId());
        }
        return accountDao.getSubscriptions(account.getId());
    }

    @GET
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptionsOfSpecificAccount(@PathParam("id") String id) throws AccountException {
        if (accountDao.getById(id) == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = accountDao.getSubscriptions(id);
        for (Subscription subscription : subscriptions) {
            subscription.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                                           uriBuilder.clone().path(getClass(), "removeSubscription")
                                                                     .build(id, subscription.getServiceId()).toString())));
        }
        return subscriptions;
    }

    @POST
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addSubscription(@PathParam("id") String id, @Required @Description("subscription to add") Subscription subscription)
            throws AccountException {
        if (subscription == null) {
            throw new AccountException("Missed subscription");
        }
        if (accountDao.getById(id) == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        SubscriptionService service = registry.get(subscription.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(subscription.getServiceId());
        }
        accountDao.addSubscription(subscription, id);
        service.notifyHandlers(new SubscriptionEvent(subscription, SubscriptionEvent.EventType.CREATE));
    }

    @DELETE
    @Path("{id}/subscriptions/{serviceid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void removeSubscription(@PathParam("serviceid") String serviceId, @PathParam("id") String accountId)
            throws AccountException {
        SubscriptionService service = registry.get(serviceId);
        if (service == null) {
            throw new ServiceNotFoundException(serviceId);
        }
        List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        Subscription needed = null;
        for (Subscription subscription : subscriptions) {
            if (subscription.getServiceId().equals(serviceId)) {
                needed = subscription;
                break;
            }
        }
        if (needed != null) {
            accountDao.removeSubscription(accountId, serviceId);
            service.notifyHandlers(new SubscriptionEvent(needed, SubscriptionEvent.EventType.REMOVE));
        } else {
            throw new SubscriptionNotFoundException(serviceId);
        }
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ACCOUNT)
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws AccountException {
        if (accountDao.getById(id) == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        accountDao.remove(id);
    }

    private void injectLinks(Account account, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("account/owner")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_CURRENT_ACCOUNT, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getCurrent").build().toString()));
            links.add(createLink("POST", Constants.LINK_REL_UPDATE_ACCOUNT, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "update").build(account.getId()).toString())
                              .withParameters(Arrays.asList(DtoFactory.getInstance().createDto(LinkParameter.class)
                                                                      .withName("accountToUpdate")
                                                                      .withRequired(true)
                                                                      .withDescription("Account to update"))));
            links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionsOfCurrentAccount").build().toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getMembersOfCurrentAccount").build().toString()));
        }
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
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