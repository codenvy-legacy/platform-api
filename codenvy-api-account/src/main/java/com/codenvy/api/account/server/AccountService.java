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

import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Member;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.server.dao.SubscriptionHistoryEvent;
import com.codenvy.api.account.shared.dto.AccountDescriptor;
import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.AccountUpdate;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.NewAccount;
import com.codenvy.api.account.shared.dto.NewCreditCard;
import com.codenvy.api.account.shared.dto.NewSubscription;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionHistoryEventDescriptor;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.codenvy.api.account.server.dao.Subscription.State.WAIT_FOR_PAYMENT;

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
            for (String attributeName : newAccount.getAttributes().keySet()) {
                validateAttributeName(attributeName);
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
        final Account account = new Account().withId(accountId)
                                             .withName(newAccount.getName())
                                             .withAttributes(newAccount.getAttributes());
        //account should have owner
        final Member owner = new Member().withAccountId(accountId)
                                         .withUserId(current.getId())
                                         .withRoles(Arrays.asList("account/owner"));
        accountDao.create(account);
        accountDao.addMember(owner);
        return Response.status(Response.Status.CREATED).entity(toDescriptor(account, securityContext)).build();
    }

    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMemberships(@Context SecurityContext securityContext)
            throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final List<Member> memberships = accountDao.getByMember(current.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    @GET
    @Path("memberships")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(
            @Required @Description("User id to get accounts") @QueryParam("userid") String userId,
            @Context SecurityContext securityContext)
            throws ConflictException, NotFoundException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed userid to search");
        }
        final User user = userDao.getById(userId);
        final List<Member> memberships = accountDao.getByMember(user.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    @DELETE
    @Path("{id}/attribute")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String accountId, @QueryParam("name") String attributeName)
            throws ConflictException, NotFoundException, ForbiddenException, ServerException {
        final Account account = accountDao.getById(accountId);
        validateAttributeName(attributeName);
        account.getAttributes().remove(attributeName);
        accountDao.update(account);
    }

    //TODO: add method for removing list of attributes
    @GET
    @Path("{id}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getById(@Context SecurityContext securityContext, @PathParam("id") String id)
            throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        return toDescriptor(account, securityContext);
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
        return toDescriptor(account, securityContext);
    }

    @POST
    @Path("{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public void addMember(@PathParam("id") String accountId,
                          @QueryParam("userid") String userId,
                          @Context SecurityContext securityContext)
            throws ConflictException, NotFoundException, ForbiddenException, ServerException {
        if (userId == null) {
            throw new ConflictException("Missed user id");
        }
        final Member newMember = new Member().withAccountId(accountId)
                                             .withUserId(userId)
                                             .withRoles(Arrays.asList("account/member"));
        accountDao.addMember(newMember);
    }

    @GET
    @Path("{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@PathParam("id") String id, @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
        final Account account = accountDao.getById(id);
        final List<Member> members = accountDao.getMembers(id);
        final List<MemberDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            result.add(toDescriptor(member, account, securityContext));
        }
        return result;
    }

    @DELETE
    @Path("{id}/members/{userid}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@PathParam("id") String accountId, @PathParam("userid") String userId)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {
        final List<Member> accMembers = accountDao.getMembers(accountId);
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
                    final Member current = mIt.next();
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
        if (update.getAttributes() != null) {
            account.getAttributes().putAll(update.getAttributes());
        }
        accountDao.update(account);
        return toDescriptor(account, securityContext);
    }

    @GET
    @Path("{id}/subscriptions")
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionDescriptor> getSubscriptions(@PathParam("id") String accountId,
                                                         @Context SecurityContext securityContext)
            throws NotFoundException, ForbiddenException, ServerException {
        final List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        final List<SubscriptionDescriptor> result = new ArrayList<>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            result.add(toDescriptor(subscription, securityContext, null));
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

        Set<String> roles = null;
        if (securityContext.isUserInRole("user")) {
            roles = resolveRolesForSpecificAccount(subscription.getAccountId());
            if (!roles.contains("account/owner") && !roles.contains("account/member")) {
                throw new ForbiddenException("Access denied");
            }
        }

        return toDescriptor(subscription, securityContext, roles);
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
        if (newSubscription.getProperties() == null || newSubscription.getProperties().isEmpty()) {
            throw new ConflictException("Missed subscription properties");
        }
        Set<String> roles = null;
        if (securityContext.isUserInRole("user")) {
            roles = resolveRolesForSpecificAccount(newSubscription.getAccountId());
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }
        final SubscriptionService service = registry.get(newSubscription.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }
        final Subscription subscription = new Subscription().withAccountId(newSubscription.getAccountId())
                                                            .withServiceId(newSubscription.getServiceId())
                                                            .withProperties(newSubscription.getProperties());
        subscription.setId(NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH));

        final Calendar calendar = Calendar.getInstance();
        subscription.setStartDate(calendar.getTimeInMillis());
        if (Boolean.parseBoolean(subscription.getProperties().get("codenvy:trial"))) {
            String userId = EnvironmentContext.getCurrent().getUser().getId();
            final List<SubscriptionHistoryEvent> events = accountDao.getSubscriptionHistoryEvents(
                    new SubscriptionHistoryEvent().withUserId(userId).withType(SubscriptionHistoryEvent.Type.CREATE).withSubscription(
                            new Subscription().withServiceId(subscription.getServiceId())
                                              .withProperties(Collections.singletonMap("codenvy:trial", "true"))
                                                                                                                                     )
                                                                                                 );
            if (events.size() > 0) {
                throw new ConflictException("You can't use trial twice, please contact support");
            }
            try {
                paymentService.getCreditCard(userId);
            } catch (NotFoundException e) {
                throw new ConflictException("You have no credit card to pay for subscription after trial");
            }
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            subscription.setState(Subscription.State.ACTIVE);
        } else {
            String tariffPlan;
            if (null == (tariffPlan = subscription.getProperties().get("TariffPlan"))) {
                throw new ConflictException("TariffPlan property not found");
            }
            switch (tariffPlan) {
                case "yearly":
                    calendar.add(Calendar.YEAR, 1);
                    break;
                case "monthly":
                    calendar.add(Calendar.MONTH, 1);
                    break;
                default:
                    throw new ConflictException("Unknown TariffPlan is used " + tariffPlan);
            }

            final double amount = service.tarifficate(subscription);
            if (0D == amount || securityContext.isUserInRole("system/admin")) {
                subscription.setState(Subscription.State.ACTIVE);
            } else {
                subscription.setState(Subscription.State.WAIT_FOR_PAYMENT);
            }
        }
        subscription.setEndDate(calendar.getTimeInMillis());

        service.beforeCreateSubscription(subscription);
        accountDao.addSubscription(subscription);
        accountDao.addSubscriptionHistoryEvent(
                createSubscriptionHistoryEvent(subscription, SubscriptionHistoryEvent.Type.CREATE,
                                               EnvironmentContext.getCurrent().getUser().getId())
                                              );
        service.afterCreateSubscription(subscription);

        Response response;
        if (subscription.getState() == WAIT_FOR_PAYMENT) {
            response = Response.status(402).entity(toDescriptor(subscription, securityContext, roles)).build();
        } else {
            response = Response.noContent().build();
        }

        return response;
    }

    /**
     * Removes subscription by id
     *
     * @param subscriptionId
     *         id of the subscription to remove
     * @throws NotFoundException
     *         if subscription with such id is not found
     * @throws ServerException
     *         if internal server error occurs
     * @throws ForbiddenException
     *         if user hasn't permissions
     * @throws ApiException
     */
    @DELETE
    @Path("subscriptions/{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeSubscription(@PathParam("id") String subscriptionId, @Context SecurityContext securityContext) throws ApiException {
        final Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") && !resolveRolesForSpecificAccount(toRemove.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        final SubscriptionService service = registry.get(toRemove.getServiceId());
        accountDao.removeSubscription(subscriptionId);
        accountDao.addSubscriptionHistoryEvent(createSubscriptionHistoryEvent(toRemove, SubscriptionHistoryEvent.Type.DELETE,
                                                                              EnvironmentContext.getCurrent().getUser().getId()));
        service.onRemoveSubscription(toRemove);
    }

    /**
     * Returns list of {@link SubscriptionHistoryEvent}s filtered by provided pattern event. User's id is set if used by user.
     *
     * @param pattern
     *         filter events by filled fields of pattern
     * @return list of {@link SubscriptionHistoryEvent}
     * @throws ForbiddenException
     *         if provided userId isn't equal to current userId
     * @throws ServerException
     *         if internal server error occurs
     */
    @POST
    @Path("subscriptions/history")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionHistoryEventDescriptor> getSubscriptionHistory(SubscriptionHistoryEvent pattern,
                                                                           @Context SecurityContext securityContext)
            throws ServerException, ForbiddenException {
        if (securityContext.isUserInRole("user")) {
            if (pattern.getUserId() == null) {
                pattern.setUserId(EnvironmentContext.getCurrent().getUser().getId());
            } else if (!EnvironmentContext.getCurrent().getUser().getId().equals(pattern.getUserId())) {
                throw new ForbiddenException("You can't get subscription history for user " + pattern.getUserId());
            }
        }

        final List<SubscriptionHistoryEvent> subscriptionHistoryEvents = accountDao.getSubscriptionHistoryEvents(pattern);
        final List<SubscriptionHistoryEventDescriptor> result = new ArrayList<>(subscriptionHistoryEvents.size());
        for (SubscriptionHistoryEvent event : subscriptionHistoryEvents) {
            result.add(toDescriptor(event));
        }
        return result;
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    /**
     * Purchase certain subscription.
     *
     * @param subscriptionId
     *         id of the subscription
     * @throws ConflictException
     *         if the subscription is not found; payment is not required; if user has no stored credit card
     * @throws ServerException
     *         if internal server error occurs
     */
    @POST
    @Path("subscriptions/{id}/purchase")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void purchaseSubscription(@PathParam("id") String subscriptionId) throws ServerException, ConflictException {
        paymentService.purchase(EnvironmentContext.getCurrent().getUser().getId(), subscriptionId);
    }

    /**
     * Saves user's credit card. User can have only 1 credit card at once.
     *
     * @param creditCard
     *         credit card to save
     * @throws ConflictException
     *         if user has credit card already
     * @throws ServerException
     *         if internal server error occurs
     */
    @POST
    @Path("credit-card")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveCreditCard(NewCreditCard creditCard) throws ServerException, ConflictException {
        paymentService.saveCreditCard(EnvironmentContext.getCurrent().getUser().getId(), DtoFactory.getInstance().clone(creditCard));
    }

    /**
     * Returns user's credit card
     *
     * @return the user's credit card, if available
     * @throws NotFoundException
     *         if user's credit card is not found
     * @throws ServerException
     *         if internal server error occurs
     */
    @GET
    @Path("credit-card")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public NewCreditCard getUserCreditCard() throws NotFoundException, ServerException {
        return paymentService.getCreditCard(EnvironmentContext.getCurrent().getUser().getId());
    }

    /**
     * Removes user credit card
     *
     * @throws ServerException
     *         if internal server error occurs
     */
    @DELETE
    @Path("credit-card")
    @RolesAllowed("user")
    public void removeCreditCard() throws ServerException {
        paymentService.removeCreditCard(EnvironmentContext.getCurrent().getUser().getId());
    }

    /**
     * Create {@link SubscriptionHistoryEvent} object
     *
     * @param subscription
     *         subscription to set in event
     * @param type
     *         event type to create
     * @param userId
     *         id of user that initiate this event
     * @return subscription history event
     */
    private SubscriptionHistoryEvent createSubscriptionHistoryEvent(Subscription subscription, SubscriptionHistoryEvent.Type type,
                                                                    String userId) {
        return new SubscriptionHistoryEvent().withId(
                NameGenerator.generate(SubscriptionHistoryEvent.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH)).withType(
                type).withUserId(userId).withTime(System.currentTimeMillis()).withSubscription(
                subscription);
    }

    /**
     * Can be used only in methods that is restricted with @RolesAllowed. Require "user" role.
     *
     * @param currentAccountId
     *         account id to resolve roles for
     * @return set of user roles
     */
    private Set<String> resolveRolesForSpecificAccount(String currentAccountId) {
        try {
            final String userId = EnvironmentContext.getCurrent().getUser().getId();
            for (Member membership : accountDao.getByMember(userId)) {
                if (membership.getAccountId().equals(currentAccountId)) {
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

    private AccountDescriptor toDescriptor(Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new LinkedList<>();
        links.add(createLink(HttpMethod.GET,
                             Constants.LINK_REL_GET_ACCOUNTS,
                             null,
                             MediaType.APPLICATION_JSON,
                             uriBuilder.clone()
                                       .path(getClass(), "getMemberships")
                                       .build()
                                       .toString()
                            ));
        links.add(createLink(HttpMethod.GET,
                             Constants.LINK_REL_GET_SUBSCRIPTIONS,
                             null,
                             MediaType.APPLICATION_JSON,
                             uriBuilder.clone()
                                       .path(getClass(), "getSubscriptions")
                                       .build(account.getId())
                                       .toString()
                            ));
        links.add(createLink(HttpMethod.GET,
                             Constants.LINK_REL_GET_MEMBERS,
                             null,
                             MediaType.APPLICATION_JSON,
                             uriBuilder.clone()
                                       .path(getClass(), "getMembers")
                                       .build(account.getId())
                                       .toString()
                            ));
        links.add(createLink(HttpMethod.GET,
                             Constants.LINK_REL_GET_ACCOUNT_BY_ID,
                             null,
                             MediaType.APPLICATION_JSON,
                             uriBuilder.clone()
                                       .path(getClass(), "getById")
                                       .build(account.getId())
                                       .toString()
                            ));

        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(createLink(HttpMethod.GET,
                                 Constants.LINK_REL_GET_ACCOUNT_BY_NAME,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getByName")
                                           .queryParam("name", account.getName())
                                           .build()
                                           .toString()
                                ));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(createLink(HttpMethod.DELETE,
                                 Constants.LINK_REL_REMOVE_ACCOUNT,
                                 null,
                                 null,
                                 uriBuilder.clone().path(getClass(), "remove")
                                           .build(account.getId())
                                           .toString()
                                ));
        }
        return DtoFactory.getInstance().createDto(AccountDescriptor.class)
                         .withId(account.getId())
                         .withName(account.getName())
                         .withAttributes(account.getAttributes())
                         .withLinks(links);
    }

    private MemberDescriptor toDescriptor(Member member, Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link removeMember = createLink(HttpMethod.DELETE,
                                             Constants.LINK_REL_REMOVE_MEMBER,
                                             null,
                                             null,
                                             uriBuilder.clone()
                                                       .path(getClass(), "removeMember")
                                                       .build(account.getId(), member.getUserId())
                                                       .toString()
                                            );
        final Link allMembers = createLink(HttpMethod.GET,
                                           Constants.LINK_REL_GET_MEMBERS,
                                           null,
                                           MediaType.APPLICATION_JSON,
                                           uriBuilder.clone()
                                                     .path(getClass(), "getMembers")
                                                     .build(account.getId())
                                                     .toString()
                                          );
        final AccountReference accountRef = DtoFactory.getInstance().createDto(AccountReference.class)
                                                      .withId(account.getId())
                                                      .withName(account.getName());
        if (member.getRoles().contains("account/owner") ||
            securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager")) {
            accountRef.setLinks(Collections.singletonList(createLink(HttpMethod.GET,
                                                                     Constants.LINK_REL_GET_ACCOUNT_BY_ID,
                                                                     null,
                                                                     MediaType.APPLICATION_JSON,
                                                                     uriBuilder.clone()
                                                                               .path(getClass(), "getById")
                                                                               .build(account.getId())
                                                                               .toString()
                                                                    )));
        }
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withRoles(member.getRoles())
                         .withAccountReference(accountRef)
                         .withLinks(Arrays.asList(removeMember, allMembers));
    }

    /**
     * Create {@link SubscriptionDescriptor} form {@link Subscription}.
     * Set with roles should be used if account roles can't be resolved with {@link SecurityContext}
     * (If there is no id of the account in the REST path.)
     *
     * @param subscription
     *         subscription that should be converted to {@link SubscriptionDescriptor}
     * @param resolvedRoles
     *         resolved roles. Do not use if id of the account presents in REST path.
     */
    private SubscriptionDescriptor toDescriptor(Subscription subscription, SecurityContext securityContext, Set<String> resolvedRoles) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new ArrayList<>(3);
        links.add(createLink(HttpMethod.GET, Constants.LINK_REL_GET_SUBSCRIPTION, null, MediaType.APPLICATION_JSON,
                             uriBuilder.clone().path(getClass(), "getSubscriptionById").build(subscription.getId()).toString()));

        if (securityContext.isUserInRole("account/owner") || securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager") ||
            (securityContext.isUserInRole("user") && resolvedRoles != null && resolvedRoles.contains("account/owner"))) {
            links.add(createLink(HttpMethod.DELETE, Constants.LINK_REL_REMOVE_SUBSCRIPTION, null, null,
                                 uriBuilder.clone().path(getClass(), "removeSubscription").build(subscription.getId()).toString()));
            if (subscription.getState() == WAIT_FOR_PAYMENT) {
                links.add(createLink(HttpMethod.POST, Constants.LINK_REL_PURCHASE_SUBSCRIPTION, MediaType.APPLICATION_JSON, null,
                                     uriBuilder.clone().path(getClass(), "purchaseSubscription").build(subscription.getId())
                                               .toString()
                                    ));
            }
        }
        return DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                         .withId(subscription.getId())
                         .withAccountId(subscription.getAccountId())
                         .withStartDate(subscription.getStartDate())
                         .withEndDate(subscription.getEndDate())
                         .withServiceId(subscription.getServiceId())
                         .withProperties(subscription.getProperties())
                         .withState(subscription.getState()).withLinks(links);
    }

    private SubscriptionHistoryEventDescriptor toDescriptor(SubscriptionHistoryEvent event) {
        Subscription subscription = event.getSubscription();
        SubscriptionDescriptor subscriptionDescriptor;
        if (subscription != null) {
            subscriptionDescriptor = DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                                               .withId(subscription.getId())
                                               .withAccountId(subscription.getAccountId())
                                               .withStartDate(subscription.getStartDate())
                                               .withEndDate(subscription.getEndDate())
                                               .withServiceId(subscription.getServiceId())
                                               .withProperties(subscription.getProperties());
        } else {
            subscriptionDescriptor = null;
        }
        return DtoFactory.getInstance().createDto(SubscriptionHistoryEventDescriptor.class).withId(event.getId())
                         .withAmount(event.getAmount()).withTime(event.getTime()).withTransactionId(event.getTransactionId())
                         .withType(event.getType()).withUserId(event.getUserId()).withSubscription(subscriptionDescriptor);
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