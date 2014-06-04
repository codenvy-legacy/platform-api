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
package com.codenvy.api.account.server;


import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Payment;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
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
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Account API
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@Path("account")
public class AccountService extends Service {

    private final AccountDao                  accountDao;
    private final UserDao                     userDao;
    private final SubscriptionServiceRegistry registry;
    private final PaymentService              paymentService;

    @Inject
    public AccountService(AccountDao accountDao, UserDao userDao, SubscriptionServiceRegistry registry, PaymentService paymentService) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.registry = registry;
        this.paymentService = paymentService;
    }

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required @Description("Account to create") Account newAccount)
            throws NotFoundException, ConflictException, ServerException {
        if (newAccount == null) {
            throw new ConflictException("Missed account to create");
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        //for now account <-One to One-> user
        if (accountDao.getByOwner(current.getId()).size() != 0) {
            throw new ConflictException(String.format("Account which owner is %s already exists", current.getId()));
        }
        if (newAccount.getName() == null) {
            throw new ConflictException("Account name required");
        }
        try {
            accountDao.getByName(newAccount.getName());
            throw new ConflictException(String.format("Account with name %s already exists", newAccount.getName()));
        } catch (NotFoundException ignored) {
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
        accountDao.create(newAccount);
        accountDao.addMember(owner);
        injectLinks(newAccount, securityContext);
        return Response.status(Response.Status.CREATED).entity(newAccount).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountMembership> getMemberships(@Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
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
            throws ConflictException, NotFoundException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed userid to search");
        }
        final User user = userDao.getById(userId);
        final List<AccountMembership> result = accountDao.getByMember(user.getId());
        for (Account account : result) {
            injectLinks(account, securityContext);
        }
        return result;
    }

    @POST
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_ADD_ATTRIBUTE)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addAttribute(@PathParam("id") String accountId, @Required @Description("New attribute") Attribute newAttribute)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final Account account = accountDao.getById(accountId);
        if (newAttribute == null) {
            throw new ConflictException("Attribute required");
        }
        validateAttributeName(newAttribute.getName());
        final List<Attribute> actual = account.getAttributes();
        removeAttribute(actual, newAttribute.getName());
        actual.add(newAttribute);
        accountDao.update(account);
    }

    @DELETE
    @Path("{id}/attribute")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ATTRIBUTE)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String accountId,
                                @Required @Description("The name of attribute") @QueryParam("name") String attributeName)
            throws ConflictException, NotFoundException, ForbiddenException, ServerException {
        final Account account = accountDao.getById(accountId);
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
            throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
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
            throws NotFoundException, ConflictException, ServerException {
        if (name == null) {
            throw new ConflictException("Missed account name");
        }
        final Account account = accountDao.getByName(name);
        injectLinks(account, securityContext);
        return account;
    }

    @POST
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_ADD_MEMBER)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void addMember(@PathParam("id") String accountId,
                          @Required @Description("User id to be a new account member") @QueryParam("userid") String userId)
            throws ConflictException, NotFoundException, ForbiddenException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed user id");
        }
        Member newMember = DtoFactory.getInstance().createDto(Member.class).withAccountId(accountId).withUserId(userId);
        newMember.setRoles(Arrays.asList("account/member"));
        accountDao.addMember(newMember);
    }

    @GET
    @Path("{id}/members")
    @GenerateLink(rel = Constants.LINK_REL_GET_MEMBERS)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> getMembers(@PathParam("id") String id, @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
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
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@PathParam("id") String accountId, @PathParam("userid") String userId)
            throws NotFoundException, ForbiddenException,
                   ServerException, ConflictException {
        List<Member> accMembers = accountDao.getMembers(accountId);
        Member toRemove = null;
        //search for member
        for (Iterator<Member> mIt = accMembers.iterator(); mIt.hasNext() && toRemove == null; ) {
            Member current = mIt.next();
            if (current.getUserId().equals(userId)) {
                toRemove = current;
            }
        }
        if (toRemove != null) {
            //account should have at least 1 owner
            //if member that is being removed is account/owner
            //we should check about other account owners existence
            if (toRemove.getRoles().contains("account/owner")) {
                boolean isOtherAccOwnerPresent = false;
                for (Iterator<Member> mIt = accMembers.iterator(); mIt.hasNext() && !isOtherAccOwnerPresent; ) {
                    Member current = mIt.next();
                    isOtherAccOwnerPresent = !current.getUserId().equals(userId)
                                             && current.getRoles().contains("account/owner");
                }
                if (!isOtherAccOwnerPresent) {
                    throw new ConflictException("Account should have at least 1 owner");
                }
            }
            accountDao.removeMember(toRemove);
        } else {
            throw new NotFoundException(String.format("User %s doesn't have membership with account %s", userId, accountId));
        }
    }

    @POST
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_UPDATE_ACCOUNT)
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Account update(@PathParam("id") String accountId,
                          @Required @Description("Account to update") Account accountToUpdate,
                          @Context SecurityContext securityContext)

            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        if (accountToUpdate == null) {
            throw new ConflictException("Missed account to update");
        }
        final Account actual = accountDao.getById(accountId);
        //current user should be account owner to update it
        //if name changed
        if (accountToUpdate.getName() != null) {
            if (!actual.getName().equals(accountToUpdate.getName()) && accountDao.getByName(accountToUpdate.getName()) != null) {
                throw new ConflictException(String.format("Account with name %s already exists", accountToUpdate.getName()));
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
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<Subscription> getSubscriptions(@PathParam("id") String accountId,
                                               @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        for (Subscription subscription : subscriptions) {
            List<Link> links = subscription.getLinks();
            links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_SUBSCRIPTION, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionById").build(subscription.getId()).toString()));

            if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
                links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                     uriBuilder.clone().path(getClass(), "removeSubscription").build(subscription.getId()).toString()));
            }
            subscription.setLinks(links);
        }
        return subscriptions;
    }

    @GET
    @Path("subscriptions/{subscriptionId}")
    @GenerateLink(rel = Constants.LINK_REL_GET_SUBSCRIPTIONS)
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Subscription getSubscriptionById(@PathParam("subscriptionId") String subscriptionId, @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ForbiddenException {
        final Subscription subscription = accountDao.getSubscriptionById(subscriptionId);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            subscription
                    .setLinks(Collections.singletonList(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                                                   uriBuilder.clone().path(getClass(), "removeSubscription")
                                                                             .build(subscription.getId()).toString()
                                                                  )));
        }
        return subscription;
    }

    @POST
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscription(@Required @Description("subscription to add") Subscription subscription)
            throws NotFoundException, ConflictException, ServerException, ForbiddenException {
        if (subscription == null) {
            throw new ConflictException("Missed subscription");
        }
        SubscriptionService service = registry.get(subscription.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }
        String subscriptionId = NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        subscription.setId(subscriptionId);

        BigDecimal amount = new BigDecimal(0);
        //BigDecimal amount = service.tarifficate(subscription);
        Response response;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            subscription.setState(Subscription.State.ACTIVE);
            response = Response.noContent().build();
        } else {
            subscription.setState(Subscription.State.WAIT_FOR_PAYMENT);
            response = Response.status(402).entity(subscription).build();
        }

        accountDao.addSubscription(subscription);
        service.notifyHandlers(new SubscriptionEvent(subscription, SubscriptionEvent.EventType.CREATE));

        return response;
    }

    @DELETE
    @Path("subscriptions/{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_SUBSCRIPTION)
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") @Description("Subscription identifier") String subscriptionId)
            throws NotFoundException, ServerException {
        Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        SubscriptionService service = registry.get(toRemove.getServiceId());
        accountDao.removeSubscription(subscriptionId);
        service.notifyHandlers(new SubscriptionEvent(toRemove, SubscriptionEvent.EventType.REMOVE));
    }

    @DELETE
    @Path("{id}")
    @GenerateLink(rel = Constants.LINK_REL_REMOVE_ACCOUNT)
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    @POST
    @Path("subscriptions/{id}/pay")
    // TODO rework to json
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed("user")
    public void purchaseSubscription(@PathParam("id") String subscriptionId,
                                     @FormParam("cardNumber") String cardNumber,
                                     @FormParam("cvv") String cvv,
                                     @FormParam("expirationMonth") String expirationMonth,
                                     @FormParam("expirationYear") String expirationYear)
            throws ConflictException, NotFoundException, ServerException {
        paymentService.purchase(DtoFactory.getInstance().createDto(Payment.class)
                                          .withCardNumber(cardNumber)
                                          .withCvv(cvv)
                                          .withExpirationMonth(expirationMonth)
                                          .withExpirationYear(expirationYear)
                                          .withSubscriptionId(subscriptionId));
    }

    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute name '%s' is not valid", attributeName));
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

    private void injectLinks(Account account, SecurityContext securityContext) {
        final List<Link> links = new ArrayList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_ACCOUNTS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMemberships").build().toString()));
        links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_SUBSCRIPTIONS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getSubscriptions").build(account.getId())
                                       .toString()
                            ));
        links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_MEMBERS, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getMembers").build(account.getId())
                                       .toString()
                            ));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_ACCOUNT_BY_ID, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getById").build(account.getId()).toString()));
            links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_ACCOUNT_BY_NAME, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getByName").queryParam("name", account.getName())
                                           .build()
                                           .toString()
                                ));

        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_ACCOUNT, null, null,
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