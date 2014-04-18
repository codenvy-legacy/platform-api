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
import com.codenvy.api.account.server.exception.AccountException;
import com.codenvy.api.account.server.exception.AccountIllegalAccessException;
import com.codenvy.api.account.server.exception.AccountNotFoundException;
import com.codenvy.api.account.server.exception.ServiceNotFoundException;
import com.codenvy.api.account.server.exception.SubscriptionNotFoundException;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Account API
 *
 * @author Eugene Voevodin
 */
@Path("account")
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
    public Response create(@Context SecurityContext securityContext,
                           @Required @Description("Account to create") Account newAccount)
            throws AccountException, UserException {
        if (newAccount == null) {
            throw new AccountException("Missed account to create");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        if (accountDao.getByOwner(current.getId()).size() != 0) {
            throw AccountAlreadyExistsException.existsWithOwner(current.getId());
        }
        if (newAccount.getName() == null) {
            throw new AccountException("Account name required");
        }
        if (accountDao.getByName(newAccount.getName()) != null) {
            throw AccountAlreadyExistsException.existsWithName(newAccount.getName());
        }
        if (newAccount.getAttributes() != null) {
            for (Attribute attribute : newAccount.getAttributes()) {
                validateAttributeName(attribute.getName());
            }
        }
        String accountId = NameGenerator.generate(Account.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        newAccount.setId(accountId);
        //account should have owner
        Member owner = DtoFactory.getInstance().createDto(Member.class)
                                 .withAccountId(accountId)
                                 .withUserId(current.getId())
                                 .withRoles(Arrays.asList("account/owner"));
        accountDao.addMember(owner);
        accountDao.create(newAccount);
        injectLinks(newAccount, securityContext);
        return Response.status(Response.Status.CREATED).entity(newAccount).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountMembership> getMemberships(@Context SecurityContext securityContext)
            throws UserException, AccountException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        if (current == null) {
            throw new UserNotFoundException(principal.getName());
        }
        final List<AccountMembership> result = accountDao.getByMember(current.getId());
        for (Account account : result) {
            injectLinks(account, securityContext);
        }
        return result;
    }

    @GET
    @Path("list")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountMembership> getMembershipsOfSpecificUser(
            @Required @Description("User id to get accounts") @QueryParam("userid") String userId,
            @Context SecurityContext securityContext)
            throws UserException, AccountException {
        if (userId == null) {
            throw new AccountException("Missed userid to search");
        }
        final User user = userDao.getById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        final List<AccountMembership> result = accountDao.getByMember(user.getId());
        for (Account account : result) {
            injectLinks(account, securityContext);
        }
        return result;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addAttribute(@PathParam("id") String accountId, @Required @Description("New attribute") Attribute newAttribute,
                             @Context SecurityContext securityContext)
            throws AccountException, UserException {
        final Account account = accountDao.getById(accountId);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        if (newAttribute == null) {
            throw new AccountException("Attribute required");
        }
        ensureCurrentUserIsAccountOwner(account, securityContext);
        validateAttributeName(newAttribute.getName());
        final List<Attribute> actual = account.getAttributes();
        removeAttribute(actual, newAttribute.getName());
        actual.add(newAttribute);
        accountDao.update(account);
    }

    @DELETE
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String accountId,
                                @Required @Description("The name of attribute") @QueryParam("name") String attributeName,
                                @Context SecurityContext securityContext)
            throws AccountException, UserException {
        final Account account = accountDao.getById(accountId);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        ensureCurrentUserIsAccountOwner(account, securityContext);
        validateAttributeName(attributeName);
        removeAttribute(account.getAttributes(), attributeName);
        accountDao.update(account);
    }

    @GET
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_ID)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Account getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws AccountException, UserException {
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
    @RolesAllowed({"user", "system/admin", "system/manager"})
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
        ensureCurrentUserIsAccountOwner(account, securityContext);
        if (userDao.getById(userId) == null) {
            throw new UserNotFoundException(userId);
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class).withAccountId(accountId).withUserId(userId);
        newMember.setRoles(Arrays.asList("account/member"));
        accountDao.addMember(newMember);
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String id, @Context SecurityContext securityContext)
            throws AccountException, UserException {
        final Account account = accountDao.getById(id);
        if (account == null) {
            throw AccountNotFoundException.doesNotExistWithId(id);
        }
        ensureCurrentUserIsAccountOwner(account, securityContext);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = accountDao.getMembers(id);
        for (Member member : members) {
            member.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                     uriBuilder.clone().path(getClass(), "removeMember")
                                                               .build(id, member.getUserId()).toString()
                                                    )));
        }
        return members;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_MEMBER)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeMember(@Context SecurityContext securityContext, @PathParam("id") String accountId,
                             @PathParam("userid") String userid) throws AccountException, UserException {
        final Account account = accountDao.getById(accountId);
        if (accountDao.getById(accountId) == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        ensureCurrentUserIsAccountOwner(account, securityContext);
        accountDao.removeMember(accountId, userid);
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ACCOUNT)
    @RolesAllowed({"user"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Account update(@PathParam("id") String accountId,
                          @Required @Description("Account to update") Account accountToUpdate,
                          @Context SecurityContext securityContext)
            throws AccountException, UserException {
        if (accountToUpdate == null) {
            throw new AccountException("Missed account to update");
        }
        final Account actual = accountDao.getById(accountId);
        if (actual == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        //current user should be account owner to update it
        ensureCurrentUserIsAccountOwner(actual, securityContext);
        //if account name changed
        if (accountToUpdate.getName() != null) {
            if (!actual.getName().equals(accountToUpdate.getName()) && accountDao.getByName(accountToUpdate.getName()) != null) {
                throw AccountAlreadyExistsException.existsWithName(accountToUpdate.getName());
            } else {
                actual.setName(accountToUpdate.getName());
            }
        }
        //add new attributes and rewrite existed with same name
        if (accountToUpdate.getAttributes() != null) {
            Map<String, Attribute> updates = new LinkedHashMap<>(accountToUpdate.getAttributes().size());
            for (Attribute toUpdate : accountToUpdate.getAttributes()) {
                validateAttributeName(toUpdate.getName());
                updates.put(toUpdate.getName(), toUpdate);
            }
            for (Iterator<Attribute> it = actual.getAttributes().iterator(); it.hasNext(); ) {
                Attribute attribute = it.next();
                if (updates.containsKey(attribute.getName())) {
                    it.remove();
                }
            }
            actual.getAttributes().addAll(accountToUpdate.getAttributes());
        }
        accountDao.update(actual);
        injectLinks(actual, securityContext);
        return actual;
    }

    @GET
    @Path("{id}/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptions(@PathParam("id") String accountId,
                                               @Context SecurityContext securityContext)
            throws AccountException, UserException {
        if (accountDao.getById(accountId) == null) {
            throw AccountNotFoundException.doesNotExistWithId(accountId);
        }
        if (securityContext.isUserInRole("user")) {
            List<AccountMembership> currentUserAccounts = getMemberships(securityContext);
            boolean isAccountExists = false;
            for (int i = 0; i < currentUserAccounts.size() && !isAccountExists; ++i) {
                isAccountExists = currentUserAccounts.get(i).getId().equals(accountId);
            }
            if (!isAccountExists) {
                throw new AccountException("Access denied");
            }
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        for (Subscription subscription : subscriptions) {
            subscription.setLinks(Arrays.asList(createLink("DELETE", Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                                           uriBuilder.clone().path(getClass(), "removeSubscription")
                                                                     .build(subscription.getId()).toString()
                                                          )));
        }
        return subscriptions;
    }

    @POST
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addSubscription(@Required @Description("subscription to add") Subscription subscription)
            throws AccountException {
        if (subscription == null) {
            throw new AccountException("Missed subscription");
        }
        if (accountDao.getById(subscription.getAccountId()) == null) {
            throw AccountNotFoundException.doesNotExistWithId(subscription.getAccountId());
        }
        SubscriptionService service = registry.get(subscription.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(subscription.getServiceId());
        }
        String subscriptionId = NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        subscription.setId(subscriptionId);
        accountDao.addSubscription(subscription);
        service.notifyHandlers(new SubscriptionEvent(subscription, SubscriptionEvent.EventType.CREATE));
    }

    @DELETE
    @Path("subscriptions/{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") @Description("Subscription identifier") String subscriptionId)
            throws AccountException {
        Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (toRemove == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }
        SubscriptionService service = registry.get(toRemove.getServiceId());
        if (service == null) {
            throw new ServiceNotFoundException(toRemove.getServiceId());
        }
        accountDao.removeSubscription(subscriptionId);
        service.notifyHandlers(new SubscriptionEvent(toRemove, SubscriptionEvent.EventType.REMOVE));
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

    private void validateAttributeName(String attributeName) throws AccountException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new AccountException(String.format("Attribute name '%s' is not valid", attributeName));
        }
    }

    private void removeAttribute(List<Attribute> src, String attributeName) {
        for (Iterator<Attribute> it = src.iterator(); it.hasNext(); ) {
            Attribute current = it.next();
            if (current.getName().equals(attributeName)) {
                it.remove();
                break;
            }
        }
    }

    private void ensureCurrentUserIsAccountOwner(Account account, SecurityContext securityContext)
            throws UserException, AccountException {
        if (securityContext.isUserInRole("user")) {
            final Principal principal = securityContext.getUserPrincipal();
            final User current = userDao.getByAlias(principal.getName());
            List<Account> accounts = accountDao.getByOwner(current.getId());
            boolean isCurrentUserAccountOwner = false;
            for (int i = 0; i < accounts.size() && !isCurrentUserAccountOwner; ++i) {
                isCurrentUserAccountOwner = accounts.get(i).getId().equals(account.getId());
            }
            if (!isCurrentUserAccountOwner) {
                throw new AccountIllegalAccessException(current.getId());
            }
        }
    }

    private List<AccountMembership> convertToMembership(List<Account> accounts, String role) {
        final List<AccountMembership> result = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            AccountMembership membership = DtoFactory.getInstance().createDto(AccountMembership.class);
            membership.setName(account.getName());
            membership.setId(account.getId());
            membership.setAttributes(account.getAttributes());
            membership.setRoles(Arrays.asList(role));
            result.add(membership);
        }
        return result;
    }

    private void injectLinks(Account account, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink("GET", Constants.LINK_REL_GET_ACCOUNTS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMemberships").build().toString()));
        links.add(createLink("GET", Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getSubscriptions").build(account.getId())
                                       .toString()
                            ));
        links.add(createLink("GET", Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMembers").build(account.getId())
                                       .toString()
                            ));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink("GET", Constants.LINK_REL_GET_ACCOUNT_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(account.getId()).toString()));
            links.add(createLink("GET", Constants.LINK_REL_GET_ACCOUNT_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", account.getName()).build()
                                           .toString()
                                ));

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