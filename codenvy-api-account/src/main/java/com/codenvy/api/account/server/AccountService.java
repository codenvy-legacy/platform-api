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
import com.codenvy.api.account.shared.dto.AccountDescriptor;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.AccountMembershipDescriptor;
import com.codenvy.api.account.shared.dto.AccountUpdate;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.NewAccount;
import com.codenvy.api.account.shared.dto.NewSubscription;
import com.codenvy.api.account.shared.dto.Payment;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionHistoryEvent;
import com.codenvy.api.core.ApiException;
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
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.api.account.shared.dto.Subscription.State.WAIT_FOR_PAYMENT;
import static com.codenvy.api.account.shared.dto.SubscriptionHistoryEvent.Type;
import static com.codenvy.api.account.shared.dto.SubscriptionHistoryEvent.Type.CREATE;
import static com.codenvy.api.account.shared.dto.SubscriptionHistoryEvent.Type.DELETE;

/**
 * Account API
 *
 * @author Eugene Voevodin
 * @author Alex Garagatyi
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
                           @Required @Description("Account to create") NewAccount newAccount)
            throws NotFoundException, ConflictException, ServerException {
        if (newAccount == null) {
            throw new ConflictException("Missed account to create");
        }
        if (newAccount.getAttributes() != null) {
            for (Attribute attribute : newAccount.getAttributes()) {
                validateAttributeName(attribute.getName());
            }
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
        final String accountId = NameGenerator.generate(Account.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final Account account = DtoFactory.getInstance().createDto(Account.class).withId(accountId).withName(newAccount.getName())
                                          .withAttributes(newAccount.getAttributes());
        //account should have owner
        final Member owner = DtoFactory.getInstance().createDto(Member.class)
                                       .withAccountId(accountId)
                                       .withUserId(current.getId())
                                       .withRoles(Arrays.asList("account/owner"));
        accountDao.create(account);
        accountDao.addMember(owner);
        final AccountDescriptor accountDescriptor = DtoFactory.getInstance().createDto(AccountDescriptor.class).withId(account.getId())
                                                              .withName(account.getName()).withAttributes(account.getAttributes());
        injectLinks(accountDescriptor, securityContext);
        return Response.status(Response.Status.CREATED).entity(accountDescriptor).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountMembershipDescriptor> getMemberships(@Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final List<AccountMembership> memberships = accountDao.getByMember(current.getId());
        final List<AccountMembershipDescriptor> result = new ArrayList<>(memberships.size());
        for (AccountMembership membership : memberships) {
            final AccountMembershipDescriptor membershipDescriptor =
                    (AccountMembershipDescriptor)DtoFactory.getInstance().createDto(AccountMembershipDescriptor.class)
                                                           .withRoles(membership.getRoles()).withId(membership.getId())
                                                           .withName(membership.getName())
                                                           .withAttributes(membership.getAttributes());
            injectLinks(membershipDescriptor, securityContext);
            result.add(membershipDescriptor);
        }
        return result;
    }

    @GET
    @Path("list")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<AccountMembershipDescriptor> getMembershipsOfSpecificUser(
            @Required @Description("User id to get accounts") @QueryParam("userid") String userId,
            @Context SecurityContext securityContext)
            throws ConflictException, NotFoundException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed userid to search");
        }
        final User user = userDao.getById(userId);
        final List<AccountMembership> memberships = accountDao.getByMember(user.getId());
        final List<AccountMembershipDescriptor> result = new ArrayList<>(memberships.size());
        for (AccountMembership membership : memberships) {
            final AccountMembershipDescriptor membershipDescriptor =
                    (AccountMembershipDescriptor)DtoFactory.getInstance().createDto(AccountMembershipDescriptor.class)
                                                           .withRoles(membership.getRoles()).withId(membership.getId())
                                                           .withName(membership.getName())
                                                           .withAttributes(membership.getAttributes());
            injectLinks(membershipDescriptor, securityContext);
            result.add(membershipDescriptor);
        }
        return result;
    }

    @POST
    @Path("{id}/attribute")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void addAttribute(@PathParam("id") String accountId, Attribute newAttribute)
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
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String accountId, @QueryParam("name") String attributeName)
            throws ConflictException, NotFoundException, ForbiddenException, ServerException {
        final Account account = accountDao.getById(accountId);
        validateAttributeName(attributeName);
        removeAttribute(account.getAttributes(), attributeName);
        accountDao.update(account);
    }

    @GET
    @Path("{id}")
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        final AccountDescriptor accountDescriptor = DtoFactory.getInstance().createDto(AccountDescriptor.class).withId(account.getId())
                                                              .withName(account.getName()).withAttributes(account.getAttributes());
        injectLinks(accountDescriptor, securityContext);
        return accountDescriptor;
    }

    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getByName(@Context SecurityContext securityContext,
                                       @Required @Description("Account name to search") @QueryParam("name") String name)
            throws NotFoundException, ConflictException, ServerException {
        if (name == null) {
            throw new ConflictException("Missed account name");
        }
        final Account account = accountDao.getByName(name);
        final AccountDescriptor accountDescriptor = DtoFactory.getInstance().createDto(AccountDescriptor.class).withId(account.getId())
                                                              .withName(account.getName()).withAttributes(account.getAttributes());
        injectLinks(accountDescriptor, securityContext);
        return accountDescriptor;
    }

    @POST
    @Path("{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void addMember(@PathParam("id") String accountId,
                          @QueryParam("userid") String userId)
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
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@PathParam("id") String id, @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Member> members = accountDao.getMembers(id);
        final List<MemberDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            final MemberDescriptor memberDescriptor = DtoFactory.getInstance().createDto(MemberDescriptor.class)
                                                                .withUserId(member.getUserId()).withAccountId(member.getAccountId())
                                                                .withRoles(member.getRoles())
                                                                .withLinks(Arrays.asList(
                                                                        createLink("DELETE", Constants.LINK_REL_REMOVE_MEMBER, null, null,
                                                                                   uriBuilder.clone().path(getClass(), "removeMember")
                                                                                             .build(id, member.getUserId()).toString()
                                                                                  )));
            result.add(memberDescriptor);
        }
        return result;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@PathParam("id") String accountId, @PathParam("userid") String userId)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {
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
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountDescriptor update(@PathParam("id") String accountId,
                                    AccountUpdate update,
                                    @Context SecurityContext securityContext)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        if (update == null) {
            throw new ConflictException("Missed account to update");
        }
        final Account account = accountDao.getById(accountId);
        //current user should be account owner to update it
        if (update.getName() != null) {
            if (!account.getName().equals(update.getName()) && accountDao.getByName(update.getName()) != null) {
                throw new ConflictException(String.format("Account with name %s already exists", update.getName()));
            } else {
                account.setName(update.getName());
            }
        }

        //add new attributes and rewrite existed with same name
        if (update.getAttributes() != null) {
            Map<String, Attribute> updates = new LinkedHashMap<>(update.getAttributes().size());
            for (Attribute toUpdate : update.getAttributes()) {
                validateAttributeName(toUpdate.getName());
                updates.put(toUpdate.getName(), toUpdate);
            }
            for (Iterator<Attribute> it = account.getAttributes().iterator(); it.hasNext(); ) {
                Attribute attribute = it.next();
                if (updates.containsKey(attribute.getName())) {
                    it.remove();
                }
            }
            account.getAttributes().addAll(update.getAttributes());
        }
        accountDao.update(account);
        final AccountDescriptor accountDescriptor = DtoFactory.getInstance().createDto(AccountDescriptor.class).withId(account.getId())
                                                              .withName(account.getName()).withAttributes(account.getAttributes());
        injectLinks(accountDescriptor, securityContext);
        return accountDescriptor;
    }

    @GET
    @Path("{id}/subscriptions")
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionDescriptor> getSubscriptions(@PathParam("id") String accountId,
                                               @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        final List<SubscriptionDescriptor> result = new ArrayList<>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            final List<Link> links = new ArrayList<>(3);
            links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_SUBSCRIPTION, null, MediaType.APPLICATION_JSON,
                                 uriBuilder.clone().path(getClass(), "getSubscriptionById").build(subscription.getId()).toString()));

            if (securityContext.isUserInRole("account/owner") || securityContext.isUserInRole("system/admin") ||
                securityContext.isUserInRole("system/manager")) {
                links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                     uriBuilder.clone().path(getClass(), "removeSubscription").build(subscription.getId()).toString()));
                if (subscription.getState() == WAIT_FOR_PAYMENT) {
                    links.add(createLink(HttpMethod.POST, Constants.LINK_REL_PURCHASE_SUBSCRIPTION, MediaType.APPLICATION_JSON, null,
                                         uriBuilder.clone().path(getClass(), "purchaseSubscription").build(subscription.getId())
                                                   .toString()
                                        ));
                }
            }
            final SubscriptionDescriptor subscriptionDescriptor = DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                                                                            .withId(subscription.getId())
                                                                            .withAccountId(subscription.getAccountId())
                                                                            .withStartDate(subscription.getStartDate())
                                                                            .withEndDate(subscription.getEndDate())
                                                                            .withServiceId(subscription.getServiceId())
                                                                            .withProperties(subscription.getProperties())
                                                                            .withState(subscription.getState()).withLinks(links);
            result.add(subscriptionDescriptor);
        }
        return result;
    }

    @GET
    @Path("subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionDescriptor getSubscriptionById(@PathParam("subscriptionId") String subscriptionId,
                                                      @Context SecurityContext securityContext) throws ApiException {
        final Subscription subscription = accountDao.getSubscriptionById(subscriptionId);

        final Set<String> roles = resolveRolesForSpecificAccount(subscription.getAccountId());
        if (securityContext.isUserInRole("user") && !roles.contains("account/owner") && !roles.contains("account/member")) {
            throw new ForbiddenException("Access denied");
        }

        final SubscriptionDescriptor subscriptionDescriptor = DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                .withId(subscription.getId()).withAccountId(subscription.getAccountId()).withStartDate(subscription.getStartDate())
                .withEndDate(subscription.getEndDate()).withServiceId(subscription.getServiceId())
                .withProperties(subscription.getProperties()).withState(subscription.getState());
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (roles.contains("account/owner") || securityContext.isUserInRole("system/admin")
            || securityContext.isUserInRole("system/manager")) {
            ArrayList<Link> links = new ArrayList<>(2);
            links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                 uriBuilder.clone().path(getClass(), "removeSubscription").build(
                                         subscription.getId()).toString()
                                ));
            if (subscription.getState() == WAIT_FOR_PAYMENT) {
                links.add(createLink(HttpMethod.POST, Constants.LINK_REL_PURCHASE_SUBSCRIPTION, MediaType.APPLICATION_JSON, null,
                                     uriBuilder.clone().path(getClass(), "purchaseSubscription").build(subscription.getId()).toString()));
            }
            subscriptionDescriptor.setLinks(links);
        }
        return subscriptionDescriptor;
    }

    @POST
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscription(@Required @Description("subscription to add") NewSubscription newSubscription,
                                    @Context SecurityContext securityContext)
            throws ApiException {
        if (newSubscription == null) {
            throw new ConflictException("Missed subscription");
        }
        if (securityContext.isUserInRole("user")) {
            final Set<String> roles = resolveRolesForSpecificAccount(newSubscription.getAccountId());
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }
        SubscriptionService service = registry.get(newSubscription.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }
        final Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                                    .withAccountId(newSubscription.getAccountId())
                                                    .withServiceId(newSubscription.getServiceId())
                                                    .withProperties(newSubscription.getProperties());
        subscription.setId(NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH));

        subscription.setStartDate(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        if (null != subscription.getProperties() && "yearly".equalsIgnoreCase(subscription.getProperties().get("TariffPlan"))) {
            calendar.add(Calendar.YEAR, 1);
        } else {
            calendar.add(Calendar.MONTH, 1);
        }
        subscription.setEndDate(calendar.getTimeInMillis());

        double amount = service.tarifficate(subscription);
        Response response;
        if (0D == amount || securityContext.isUserInRole("system/admin")) {
            subscription.setState(Subscription.State.ACTIVE);
            response = Response.noContent().build();
        } else {
            subscription.setState(WAIT_FOR_PAYMENT);
            final SubscriptionDescriptor subscriptionDescriptor = DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                                                                            .withId(subscription.getId())
                                                                            .withAccountId(subscription.getAccountId())
                                                                            .withServiceId(subscription.getServiceId())
                                                                            .withProperties(subscription.getProperties())
                                                                            .withStartDate(subscription.getStartDate())
                                                                            .withEndDate(subscription.getEndDate())
                                                                            .withState(subscription.getState());
            final ArrayList<Link> links = new ArrayList<>(2);
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            links.add(createLink(HttpMethod.POST, Constants.LINK_REL_PURCHASE_SUBSCRIPTION, MediaType.APPLICATION_JSON, null,
                                 uriBuilder.clone().path(getClass(), "purchaseSubscription").build(subscription.getId()).toString()));
            links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                 uriBuilder.clone().path(getClass(), "removeSubscription").build(subscription.getId()).toString()));

            response = Response.status(402).entity(subscriptionDescriptor.withLinks(links)).build();
        }

        service.beforeCreateSubscription(subscription);
        accountDao.addSubscription(subscription);
        accountDao.addSubscriptionHistoryEvent(createSubscriptionHistoryEvent(subscription, CREATE));
        service.afterCreateSubscription(subscription);

        return response;
    }

    @DELETE
    @Path("subscriptions/{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") String subscriptionId, @Context SecurityContext securityContext)
            throws ApiException {
        Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user")) {
            final Set<String> roles = resolveRolesForSpecificAccount(toRemove.getAccountId());
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }
        SubscriptionService service = registry.get(toRemove.getServiceId());
        accountDao.removeSubscription(subscriptionId);
        accountDao.addSubscriptionHistoryEvent(createSubscriptionHistoryEvent(toRemove, DELETE));
        service.onRemoveSubscription(toRemove);
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    @POST
    @Path("subscriptions/{id}/purchase")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void purchaseSubscription(@PathParam("id") String subscriptionId, Payment payment) throws ApiException {
        paymentService.purchase(DtoFactory.getInstance().clone(payment).withSubscriptionId(subscriptionId));
    }

    /**
     * Can be used only in methods that is restricted with @RolesAllowed
     *
     * @param subscription
     *         subscription to set in event
     * @param type
     *         event type to create
     * @return subscription history event
     */
    private SubscriptionHistoryEvent createSubscriptionHistoryEvent(Subscription subscription, Type type) {
        return DtoFactory.getInstance().createDto(SubscriptionHistoryEvent.class)
                         .withId(NameGenerator.generate(
                                 SubscriptionHistoryEvent.class.getSimpleName().toLowerCase(),
                                 Constants.ID_LENGTH))
                         .withType(type)
                         .withUserId(EnvironmentContext.getCurrent().getUser().getId())
                         .withTime(System.currentTimeMillis())
                         .withSubscription(subscription);
    }

    /**
     * Can be used only in methods that is restricted with @RolesAllowed
     *
     * @param currentAccountId
     *         account id to resolve roles for
     * @return set of user roles
     */
    private Set<String> resolveRolesForSpecificAccount(String currentAccountId) {
        try {
            final String userId = EnvironmentContext.getCurrent().getUser().getId();
            for (AccountMembership membership : accountDao.getByMember(userId)) {
                if (membership.getId().equals(currentAccountId)) {
                    return new HashSet<>(membership.getRoles());
                }
            }
        } catch (ApiException ignored) {
        }
        return Collections.emptySet();
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

    private void injectLinks(AccountDescriptor account, SecurityContext securityContext) {
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