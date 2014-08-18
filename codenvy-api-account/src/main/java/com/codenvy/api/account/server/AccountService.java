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
import com.codenvy.api.account.server.dao.PlanDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.shared.dto.AccountDescriptor;
import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.AccountUpdate;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.NewAccount;
import com.codenvy.api.account.shared.dto.NewSubscription;
import com.codenvy.api.account.shared.dto.Plan;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Account API
 *
 * @author Eugene Voevodin
 * @author Alex Garagatyi
 */
@Path("account")
public class AccountService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountDao                  accountDao;
    private final UserDao                     userDao;
    private final SubscriptionServiceRegistry registry;
    private final PaymentService              paymentService;
    private final PlanDao                     planDao;

    @Inject
    public AccountService(AccountDao accountDao,
                          UserDao userDao,
                          SubscriptionServiceRegistry registry,
                          PaymentService paymentService,
                          PlanDao planDao) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.registry = registry;
        this.paymentService = paymentService;
        this.planDao = planDao;
    }

    /**
     * Creates new account and adds current user as member to created account
     * with role <i>"account/owner"</i>. Returns status <b>201 CREATED</b>
     * and {@link AccountDescriptor} of created account if account has been created successfully.
     * Each new account should contain at least name.
     *
     * @param newAccount
     *         new account
     * @return descriptor of created account
     * @throws NotFoundException
     *         when some error occurred while retrieving account
     * @throws ConflictException
     *         when new account is {@code null}
     *         or new account name is {@code null}
     *         or when any of new account attributes is not valid
     * @throws ServerException
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     * @see #getByName(String, SecurityContext)
     */
    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required NewAccount newAccount) throws NotFoundException,
                                                                   ConflictException,
                                                                   ServerException {
        requiredNotNull(newAccount, "New account");
        requiredNotNull(newAccount.getName(), "Account name");
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
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(account, securityContext))
                       .build();
    }

    /**
     * Returns all accounts memberships for current user.
     *
     * @return accounts memberships of current user
     * @throws NotFoundException
     *         when any of memberships contains account that doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or memberships
     * @see MemberDescriptor
     */
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMemberships(@Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final List<Member> memberships = accountDao.getByMember(current.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Returns all accounts memberships for user with given identifier.
     *
     * @param userId
     *         user identifier to search memberships
     * @return accounts memberships
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user or memberships
     * @see MemberDescriptor
     */
    @GET
    @Path("memberships")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@Required @QueryParam("userid") String userId,
                                                               @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                                ServerException,
                                                                                                                ConflictException {
        requiredNotNull(userId, "User identifier");
        final User user = userDao.getById(userId);
        final List<Member> memberships = accountDao.getByMember(user.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Removes attribute with given name from certain account.
     *
     * @param accountId
     *         account identifier
     * @param attributeName
     *         attribute name to remove attribute
     * @throws ConflictException
     *         if attribute name is not valid
     * @throws NotFoundException
     *         if account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting/updating account
     */
    @DELETE
    @Path("{id}/attribute")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@PathParam("id") String accountId,
                                @QueryParam("name") String attributeName) throws ConflictException, NotFoundException, ServerException {
        validateAttributeName(attributeName);
        final Account account = accountDao.getById(accountId);
        account.getAttributes().remove(attributeName);
        accountDao.update(account);
    }

    //TODO: add method for removing list of attributes

    /**
     * Searches for account with given identifier and returns {@link AccountDescriptor} for it.
     *
     * @param id
     *         account identifier
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getByName(String, SecurityContext)
     */
    @GET
    @Path("{id}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getById(@PathParam("id") String id,
                                     @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        return toDescriptor(account, securityContext);
    }

    /**
     * Searches for account with given name and returns {@link AccountDescriptor} for it.
     *
     * @param name
     *         account name
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given name doesn't exist
     * @throws ConflictException
     *         when account name is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     */
    @GET
    @Path("find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getByName(@Required @QueryParam("name") String name,
                                       @Context SecurityContext securityContext) throws NotFoundException,
                                                                                        ServerException,
                                                                                        ConflictException {
        requiredNotNull(name, "Account name");
        final Account account = accountDao.getByName(name);
        return toDescriptor(account, securityContext);
    }

    /**
     * Creates new account member with role <i>"account/member"</i>.
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @return descriptor of created member
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting user or adding new account member
     * @see MemberDescriptor
     * @see #removeMember(String, String)
     * @see #getMembers(String, SecurityContext)
     */
    @POST
    @Path("{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response addMember(@PathParam("id") String accountId,
                              @QueryParam("userid") String userId,
                              @Context SecurityContext securityContext) throws ConflictException,
                                                                               NotFoundException,
                                                                               ServerException {
        requiredNotNull(userId, "User identifier");
        userDao.getById(userId);//check user exists
        final Account account = accountDao.getById(accountId);
        final Member newMember = new Member().withAccountId(accountId)
                                             .withUserId(userId)
                                             .withRoles(Arrays.asList("account/member"));
        accountDao.addMember(newMember);
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(newMember, account, securityContext))
                       .build();
    }

    /**
     * Returns all members of certain account.
     *
     * @param id
     *         account identifier
     * @return account members
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or members
     * @see MemberDescriptor
     * @see #addMember(String, String, SecurityContext)
     * @see #removeMember(String, String)
     */
    @GET
    @Path("{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@PathParam("id") String id,
                                             @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        final List<Member> members = accountDao.getMembers(id);
        final List<MemberDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            result.add(toDescriptor(member, account, securityContext));
        }
        return result;
    }

    /**
     * Removes user with given identifier as member from certain account.
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account members or removing certain member
     * @throws ConflictException
     *         when removal member is last <i>"account/owner"</i>
     * @see #addMember(String, String, SecurityContext)
     * @see #getMembers(String, SecurityContext)
     */
    @DELETE
    @Path("{id}/members/{userid}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@PathParam("id") String accountId,
                             @PathParam("userid") String userId) throws NotFoundException, ServerException, ConflictException {
        final List<Member> accMembers = accountDao.getMembers(accountId);
        Member toRemove = null;
        //search for member
        for (Iterator<Member> mIt = accMembers.iterator(); mIt.hasNext() && toRemove == null; ) {
            final Member current = mIt.next();
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
                    isOtherAccOwnerPresent = !current.getUserId().equals(userId) && current.getRoles().contains("account/owner");
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

    /**
     * <p>Updates account.</p>
     * <strong>Note:</strong> existed account attributes with same names as
     * update attributes will be replaced with update attributes.
     *
     * @param accountId
     *         account identifier
     * @param update
     *         account update
     * @return descriptor of updated account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ConflictException
     *         when account update is {@code null}
     *         or when account with given name already exists
     * @throws ServerException
     *         when some error occurred while retrieving/persisting account
     * @see AccountDescriptor
     */
    @POST
    @Path("{id}")
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountDescriptor update(@PathParam("id") String accountId,
                                    AccountUpdate update,
                                    @Context SecurityContext securityContext) throws NotFoundException,
                                                                                     ConflictException,
                                                                                     ServerException {
        requiredNotNull(update, "Account update");
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
            for (String attributeName : update.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
            account.getAttributes().putAll(update.getAttributes());
        }
        accountDao.update(account);
        return toDescriptor(account, securityContext);
    }

    /**
     * Returns list of subscriptions descriptors for certain account.
     *
     * @param accountId
     *         account identifier
     * @return subscriptions descriptors
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving subscriptions
     * @see SubscriptionDescriptor
     */
    @GET
    @Path("{id}/subscriptions")
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionDescriptor> getSubscriptions(@PathParam("id") String accountId,
                                                         @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                          ServerException {
        final List<Subscription> subscriptions = accountDao.getSubscriptions(accountId);
        final List<SubscriptionDescriptor> result = new ArrayList<>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            result.add(toDescriptor(subscription, securityContext, null));
        }
        return result;
    }

    /**
     * Returns {@link SubscriptionDescriptor} for subscription with given identifier.
     *
     * @param subscriptionId
     *         subscription identifier
     * @return descriptor of subscription
     * @throws NotFoundException
     *         when subscription with given identifier doesn't exist
     * @throws ForbiddenException
     *         when user hasn't access to call this method
     * @see SubscriptionDescriptor
     * @see #getSubscriptions(String, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @GET
    @Path("subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionDescriptor getSubscriptionById(@PathParam("subscriptionId") String subscriptionId,
                                                      @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                       ServerException,
                                                                                                       ForbiddenException {
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

    /**
     * <p>Creates new subscription. Returns {@link SubscriptionDescriptor}
     * when subscription has been created successfully.
     * <p>Each new subscription should contain plan id and account id </p>
     *
     * @param newSubscription
     *         new subscription
     * @return descriptor of created subscription
     * @throws ConflictException
     *         when new subscription is {@code null}
     *         or new subscription plan identifier is {@code null}
     *         or new subscription account identifier is {@code null}
     * @throws NotFoundException
     *         if plan with certain identifier is not found
     * @throws com.codenvy.api.core.ApiException
     * @see SubscriptionDescriptor
     * @see #getSubscriptionById(String, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @POST
    @Path("subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscription(@Required NewSubscription newSubscription,
                                    @Context SecurityContext securityContext)
            throws ApiException {
        requiredNotNull(newSubscription, "New subscription");
        requiredNotNull(newSubscription.getAccountId(), "Account identifier");
        requiredNotNull(newSubscription.getPlanId(), "Plan identifier");
        //check user has access to add subscription
        final Set<String> roles = new HashSet<>();
        if (securityContext.isUserInRole("user")) {
            roles.addAll(resolveRolesForSpecificAccount(newSubscription.getAccountId()));
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }

        final Plan plan = planDao.getPlanById(newSubscription.getPlanId());
        //check service exists
        final SubscriptionService service = registry.get(plan.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }
        //create new subscription
        final Subscription subscription = new Subscription().withAccountId(newSubscription.getAccountId())
                                                            .withServiceId(plan.getServiceId())
                                                            .withPlanId(plan.getId())
                                                            .withProperties(plan.getProperties());
        subscription.setId(NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH));

        service.beforeCreateSubscription(subscription);

        LOG.info("Add subscription# id#{}# userId#{}# accountId#{}# planId#{}#", subscription.getId(),
                 EnvironmentContext.getCurrent().getUser().getId(), subscription.getAccountId(), subscription.getPlanId());

        if (plan.isPaid() && !securityContext.isUserInRole("system/admin")) {
            paymentService.addSubscription(subscription, newSubscription.getBillingProperties());
        }
        try {
            accountDao.addSubscription(subscription);
            // prevents NPE if admin adds subscription w/o billing properties
            Map<String, String> billingProperties;
            if (null == (billingProperties = newSubscription.getBillingProperties())) {
                billingProperties = Collections.emptyMap();
            }
            accountDao.saveBillingProperties(subscription.getId(), billingProperties);
        } catch (ApiException e) {
            LOG.error(e.getLocalizedMessage(), e);
            paymentService.removeSubscription(subscription.getId());
            throw e;
        }

        service.afterCreateSubscription(subscription);
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(subscription, securityContext, roles))
                       .build();
    }

    /**
     * Removes subscription by id
     *
     * @param subscriptionId
     *         id of the subscription to remove
     * @throws NotFoundException
     *         if subscription with such id is not found
     * @throws ForbiddenException
     *         if user hasn't permissions
     * @throws ServerException
     *         if internal server error occurs
     * @throws com.codenvy.api.core.ApiException
     * @see #addSubscription(NewSubscription, SecurityContext)
     * @see #getSubscriptions(String, SecurityContext)
     */
    @DELETE
    @Path("subscriptions/{id}")
    @RolesAllowed({"user", "system/admin"})
    public void removeSubscription(@PathParam("id") String subscriptionId, @Context SecurityContext securityContext)
            throws ApiException {
        final Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") && !resolveRolesForSpecificAccount(toRemove.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        try {
            paymentService.removeSubscription(subscriptionId);
        } catch (NotFoundException ignored) {
            LOG.info(ignored.getLocalizedMessage(), ignored);
        }
        accountDao.removeSubscription(subscriptionId);
        accountDao.removeBillingProperties(subscriptionId);
        final SubscriptionService service = registry.get(toRemove.getServiceId());
        service.onRemoveSubscription(toRemove);
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("system/admin")
    public void remove(@PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    /**
     * Returns billing properties of certain subscription
     *
     * @param subscriptionId
     *         identifier of the subscription
     * @return billing properties
     * @throws NotFoundException
     *         if subscription doesn't exist or billing properties are missing
     * @throws ForbiddenException
     *         if user is not allowed to call this method
     * @throws ServerException
     */
    @GET
    @Path("subscriptions/{id}/billing")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Map<String, String> getBillingProperties(@PathParam("id") String subscriptionId, @Context SecurityContext securityContext)
            throws ServerException, NotFoundException, ForbiddenException {
        final Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") && !resolveRolesForSpecificAccount(toRemove.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        return accountDao.getBillingProperties(subscriptionId);
    }

    /**
     * Validates addition of the subscription
     *
     * @param accountId
     *         identifier of account
     * @param planId
     *         identifier of plan of subscription
     * @return {@link com.codenvy.api.account.shared.dto.NewSubscription} with set plan and account identifiers
     * @throws NotFoundException
     *         if requested plan is not found
     * @throws ConflictException
     *         if requested subscription can't be added
     * @throws ServerException
     */
    @GET
    @Path("{accountId}/subscriptions/validate")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public NewSubscription validateSubscriptionAddition(@PathParam("accountId") String accountId, @QueryParam("planId") String planId)
            throws NotFoundException, ServerException, ConflictException {
        if (null == planId) {
            throw new ConflictException("Plan identifier required");
        }
        final Plan plan = planDao.getPlanById(planId);
        final SubscriptionService service = registry.get(plan.getServiceId());
        //create new subscription
        final Subscription subscription = new Subscription().withAccountId(accountId)
                                                            .withServiceId(plan.getServiceId())
                                                            .withPlanId(plan.getId())
                                                            .withProperties(plan.getProperties());
        service.beforeCreateSubscription(subscription);

        return DtoFactory.getInstance().createDto(NewSubscription.class).withPlanId(planId).withAccountId(accountId);
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

    /** Converts {@link Account} to {@link AccountDescriptor} */
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

    /**
     * Converts {@link Member} to {@link MemberDescriptor}
     */
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
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ConflictException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ConflictException {
        if (object == null) {
            throw new ConflictException(subject + " required");
        }
    }

    /**
     * Create {@link SubscriptionDescriptor} from {@link Subscription}.
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
        links.add(createLink(HttpMethod.GET,
                             Constants.LINK_REL_GET_SUBSCRIPTION,
                             null,
                             MediaType.APPLICATION_JSON,
                             uriBuilder.clone()
                                       .path(getClass(), "getSubscriptionById")
                                       .build(subscription.getId())
                                       .toString()
                            ));
        if (securityContext.isUserInRole("account/owner") || securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager") ||
            (securityContext.isUserInRole("user") && resolvedRoles != null && resolvedRoles.contains("account/owner"))) {
            links.add(createLink(HttpMethod.DELETE,
                                 Constants.LINK_REL_REMOVE_SUBSCRIPTION,
                                 null,
                                 null,
                                 uriBuilder.clone()
                                           .path(getClass(), "removeSubscription")
                                           .build(subscription.getId())
                                           .toString()
                                ));
            links.add(createLink(HttpMethod.GET,
                                 Constants.LINK_REL_GET_BILLING_PROPERTIES,
                                 null,
                                 MediaType.APPLICATION_JSON,
                                 uriBuilder.clone()
                                           .path(getClass(), "getBillingProperties")
                                           .build(subscription.getId())
                                           .toString()
                                ));
        }
        return DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                         .withId(subscription.getId())
                         .withAccountId(subscription.getAccountId())
                         .withServiceId(subscription.getServiceId())
                         .withProperties(subscription.getProperties())
                         .withPlanId(subscription.getPlanId())
                         .withLinks(links);
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