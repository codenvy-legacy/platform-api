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
package com.codenvy.api.account;

import com.codenvy.api.account.server.AccountService;
import com.codenvy.api.account.server.Constants;
import com.codenvy.api.account.server.PaymentService;
import com.codenvy.api.account.server.SubscriptionService;
import com.codenvy.api.account.server.SubscriptionServiceRegistry;
import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Member;
import com.codenvy.api.account.server.dao.PlanDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.shared.dto.AccountDescriptor;
import com.codenvy.api.account.shared.dto.AccountUpdate;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.NewSubscription;
import com.codenvy.api.account.shared.dto.Plan;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.everrest.core.tools.SimplePrincipal;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * Tests for Account Service
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 * @see com.codenvy.api.account.server.AccountService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class AccountServiceTest {

    private final String BASE_URI        = "http://localhost/service";
    private final String SERVICE_PATH    = BASE_URI + "/account";
    private final String USER_ID         = "user123abc456def";
    private final String ACCOUNT_ID      = "account0xffffffffff";
    private final String SUBSCRIPTION_ID = "subscription0xffffffffff";
    private final String ACCOUNT_NAME    = "codenvy";
    private final String SERVICE_ID      = "IDE_SERVICE";
    private final String USER_EMAIL      = "account@mail.com";
    private final String PLAN_ID         = "planId";
    private final User   user            = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private PlanDao planDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionServiceRegistry serviceRegistry;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EnvironmentContext environmentContext;

    private Account account;
    private Plan    plan;

    private   ArrayList<Member>  memberships;
    protected ProviderBinder     providers;
    protected ResourceBinderImpl resources;
    protected ResourceLauncher   launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        resources = new ResourceBinderImpl();
        providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(PlanDao.class, planDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        dependencies.addComponent(SubscriptionServiceRegistry.class, serviceRegistry);
        dependencies.addComponent(PaymentService.class, paymentService);
        resources.addResource(AccountService.class, null);
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("secret", "bit secret");
        account = new Account().withId(ACCOUNT_ID)
                               .withName(ACCOUNT_NAME)
                               .withAttributes(attributes);
        plan = DtoFactory.getInstance().createDto(Plan.class).withId(PLAN_ID).withPaid(true).withServiceId(SERVICE_ID)
                         .withProperties(Collections.singletonMap("key", "value"));
//        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));
        memberships = new ArrayList<>(1);
        //TODO:
        Member ownerMembership = new Member();
        ownerMembership.setAccountId(account.getId());
        ownerMembership.setUserId(USER_ID);
        ownerMembership.setRoles(Arrays.asList("account/owner"));
        memberships.add(ownerMembership);
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new SimplePrincipal(USER_EMAIL));

        com.codenvy.commons.env.EnvironmentContext.getCurrent().setUser(new com.codenvy.commons.user.User() {
            @Override
            public String getName() {
                return user.getEmail();
            }

            @Override
            public boolean isMemberOf(String role) {
                return false;
            }

            @Override
            public String getToken() {
                return "token";
            }

            @Override
            public String getId() {
                return user.getId();
            }

            @Override
            public boolean isTemporary() {
                return false;
            }
        });
    }

    @AfterMethod
    public void tearDown() throws Exception {
        com.codenvy.commons.env.EnvironmentContext.reset();
    }

    @Test
    public void shouldBeAbleToCreateAccount() throws Exception {
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(accountDao.getByName(account.getName())).thenThrow(new NotFoundException("Account not found"));
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.<Account>emptyList());
        String role = "user";
        prepareSecurityContext(role);

        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH, MediaType.APPLICATION_JSON, account);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        AccountDescriptor created = (AccountDescriptor)response.getEntity();
        verifyLinksRel(created.getLinks(), generateRels(role));
        verify(accountDao).create(any(Account.class));
        Member expected = new Member().withAccountId(created.getId())
                                      .withUserId(USER_ID)
                                      .withRoles(Arrays.asList("account/owner"));
        verify(accountDao).addMember(expected);
    }

    @Test
    public void shouldNotBeAbleToCreateAccountWithNotValidAttributes() throws Exception {
        account.getAttributes().put("codenvy:god_mode", "true");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, account);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldNotBeAbleToCreateAccountIfUserAlreadyHasOne() throws Exception {
        prepareSecurityContext("user");
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));

        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH, MediaType.APPLICATION_JSON, account);
        assertEquals(response.getEntity().toString(), "Account which owner is " + USER_ID + " already exists");
    }

    @Test
    public void shouldNotBeAbleToCreateAccountWithoutName() throws Exception {
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(accountDao.getByName(account.getName())).thenReturn(null);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.<Account>emptyList());
        account.setName(null);
        String role = "user";
        prepareSecurityContext(role);

        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH, MediaType.APPLICATION_JSON, account);

        assertEquals(response.getEntity().toString(), "Account name required");
    }

    @Test
    public void shouldBeAbleToGetMemberships() throws Exception {
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked") List<MemberDescriptor> currentAccounts = (List<MemberDescriptor>)response.getEntity();
        assertEquals(currentAccounts.size(), 1);
        assertEquals(currentAccounts.get(0).getRoles().get(0), "account/owner");
        verify(accountDao).getByMember(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetMembershipsOfSpecificUser() throws Exception {
        when(accountDao.getById("fake_id")).thenReturn(new Account().withId("fake_id").withName("fake_name"));
        User user = DtoFactory.getInstance().createDto(User.class).withId("ANOTHER_USER_ID").withEmail("ANOTHER_USER_EMAIL");
        ArrayList<Member> memberships = new ArrayList<>(1);
        Member am = new Member().withAccountId("fake_id")
                                .withUserId("ANOTHER_USER_ID")
                                .withRoles(Arrays.asList("account/member"));
        memberships.add(am);
        when(userDao.getById("ANOTHER_USER_ID")).thenReturn(user);
        when(accountDao.getByMember("ANOTHER_USER_ID")).thenReturn(memberships);

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/memberships?userid=" + "ANOTHER_USER_ID", null, null);
        @SuppressWarnings("unchecked") List<MemberDescriptor> currentAccounts = (List<MemberDescriptor>)response.getEntity();
        assertEquals(currentAccounts.size(), 1);
        assertEquals(currentAccounts.get(0).getAccountReference().getId(), am.getAccountId());
        assertEquals(currentAccounts.get(0).getAccountReference().getName(), "fake_name");
        assertEquals(currentAccounts.get(0).getRoles(), am.getRoles());
    }

    @Test
    public void shouldBeAbleToGetAccountById() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        String[] roles = getRoles(AccountService.class, "getById");

        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + ACCOUNT_ID, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            AccountDescriptor actual = (AccountDescriptor)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(accountDao, times(roles.length)).getById(ACCOUNT_ID);
    }

    @Test
    public void shouldBeAbleToUpdateAccount() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        AccountUpdate toUpdate = DtoFactory.getInstance().createDto(AccountUpdate.class)
                                           .withName("newName")
                                           .withAttributes(Collections.singletonMap("newAttribute", "someValue"));
        prepareSecurityContext("account/owner");

        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        AccountDescriptor actual = (AccountDescriptor)response.getEntity();
        assertEquals(actual.getAttributes().size(), 2);
        assertEquals(actual.getName(), "newName");
    }

    @Test
    public void shouldBeAbleToRewriteAttributesWhenUpdatingAccount() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("newAttribute", "someValue");
        attributes.put("oldAttribute", "oldValue");
        account.setAttributes(attributes);

        Map<String, String> updates = new HashMap<>();
        updates.put("newAttribute", "OTHER_VALUE");
        updates.put("newAttribute2", "someValue2");
        AccountDescriptor toUpdate = DtoFactory.getInstance().createDto(AccountDescriptor.class).withAttributes(updates);

        prepareSecurityContext("account/owner");
        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        AccountDescriptor actual = (AccountDescriptor)response.getEntity();
        assertEquals(actual.getName(), ACCOUNT_NAME);
        assertEquals(actual.getAttributes().size(), 3);
        assertEquals(actual.getAttributes().get("newAttribute"), "OTHER_VALUE");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("test", "test");
        account.setAttributes(attributes);

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute?name=test", null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertNull(attributes.get("test"));
    }

    @Test
    public void shouldNotBeAbleToUpdateAccountWithAlreadyExistedName() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        when(accountDao.getByName("TO_UPDATE")).thenReturn(new Account().withName("TO_UPDATE"));
        AccountDescriptor toUpdate = DtoFactory.getInstance().createDto(AccountDescriptor.class).withName("TO_UPDATE");
        prepareSecurityContext("account/owner");

        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);
        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity().toString(), "Account with name TO_UPDATE already exists");
    }

    @Test
    public void shouldBeAbleToGetAccountByName() throws Exception {
        when(accountDao.getByName(ACCOUNT_NAME)).thenReturn(account);
        String[] roles = getRoles(AccountService.class, "getByName");
        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/find?name=" + ACCOUNT_NAME, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            AccountDescriptor actual = (AccountDescriptor)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(accountDao, times(roles.length)).getByName(ACCOUNT_NAME);
    }

    @Test
    public void shouldBeAbleToGetSubscriptionsOfSpecificAccount() throws Exception {
        when(accountDao.getSubscriptions(ACCOUNT_ID)).thenReturn(Arrays.asList(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                              ));
        prepareSecurityContext("system/admin");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked") List<SubscriptionDescriptor> subscriptions = (List<SubscriptionDescriptor>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        assertEqualsNoOrder(subscriptions.get(0).getLinks().toArray(), new Link[]{
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID).withProduces(MediaType.APPLICATION_JSON),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_BILLING_PROPERTIES).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID + "/billing")
                          .withProduces(MediaType.APPLICATION_JSON)});
        verify(accountDao).getSubscriptions(ACCOUNT_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionBySystemAdmin() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("system/admin");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        assertEqualsNoOrder(subscription.getLinks().toArray(), new Link[]{
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID).withProduces(MediaType.APPLICATION_JSON),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_BILLING_PROPERTIES).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID + "/billing")
                          .withProduces(MediaType.APPLICATION_JSON)});
        verify(accountDao).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionByAccountOwner() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withAccountId("ANOTHER_ACCOUNT_ID")
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        when(accountDao.getByMember(USER_ID)).thenReturn(Arrays.asList(new Member().withRoles(Arrays.asList("account/owner"))
                                                                                   .withAccountId("ANOTHER_ACCOUNT_ID")
                                                                                   .withUserId(USER_ID)));
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        assertEqualsNoOrder(subscription.getLinks().toArray(), new Link[]{
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID).withProduces(MediaType.APPLICATION_JSON),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_BILLING_PROPERTIES).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID + "/billing")
                          .withProduces(MediaType.APPLICATION_JSON)});
        verify(accountDao).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionByAccountMember() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withAccountId("ANOTHER_ACCOUNT_ID")
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        when(accountDao.getByMember(USER_ID)).thenReturn(Arrays.asList(new Member().withRoles(Arrays.asList("account/member"))
                                                                                   .withAccountId("ANOTHER_ACCOUNT_ID")
                                                                                   .withUserId(USER_ID)));
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        assertEquals(subscription.getLinks(), Arrays.asList(DtoFactory.getInstance().createDto(Link.class).withRel(
                Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(
                HttpMethod.GET).withHref(
                SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID).withProduces(MediaType.APPLICATION_JSON)));
        verify(accountDao).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldRespondForbiddenIfUserIsNotMemberOrOwnerOfAccountOnGetSubscriptionById() throws Exception {
        ArrayList<Member> memberships = new ArrayList<>();
        Member am = new Member();
        am.withRoles(Arrays.asList("account/owner")).withAccountId("fake_id");
        memberships.add(am);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withAccountId(ACCOUNT_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertNotEquals(Response.Status.OK, response.getStatus());
        assertEquals(response.getEntity(), "Access denied");
    }

    @Test
    public void shouldRespondAccessDeniedIfUserIsNotAccountOwnerOnAddSubscription() throws Exception {
        ArrayList<Member> memberships = new ArrayList<>(2);
        Member am = new Member();
        am.withRoles(Arrays.asList("account/owner")).withAccountId("fake_id");
        memberships.add(am);
        Member am2 = new Member();
        am2.withRoles(Arrays.asList("account/member")).withAccountId(ACCOUNT_ID);
        memberships.add(am2);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        NewSubscription newSubscription = DtoFactory.getInstance().createDto(NewSubscription.class)
                                                    .withAccountId(ACCOUNT_ID).withPlanId(PLAN_ID);
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, newSubscription);

        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddSubscriptionWithAddSubscriptionOnBT() throws Exception {
        final NewSubscription newSubscription = DtoFactory.getInstance().createDto(NewSubscription.class)
                                                          .withAccountId(ACCOUNT_ID).withPlanId(PLAN_ID)
                                                          .withBillingProperties(Collections.singletonMap("billingKey", "billingValue"));
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(planDao.getPlanById(PLAN_ID)).thenReturn(plan);
        when(accountDao.getByMember(USER_ID)).thenReturn(Arrays.asList(new Member().withRoles(Arrays.asList("account/owner"))
                                                                                   .withAccountId(ACCOUNT_ID)
                                                                                   .withUserId(USER_ID)));
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, newSubscription);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        assertEqualsNoOrder(subscription.getLinks().toArray(), new Link[]{
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE)
                          .withHref(SERVICE_PATH + "/subscriptions/" + subscription.getId()),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + subscription.getId()).withProduces(MediaType.APPLICATION_JSON),
                DtoFactory.getInstance().createDto(Link.class).withRel(Constants.LINK_REL_GET_BILLING_PROPERTIES).withMethod(HttpMethod.GET)
                          .withHref(SERVICE_PATH + "/subscriptions/" + subscription.getId() + "/billing")
                          .withProduces(MediaType.APPLICATION_JSON)});
        verify(accountDao).addSubscription(argThat(new ArgumentMatcher<Subscription>() {
            @Override
            public boolean matches(Object argument) {
                Subscription actual = (Subscription)argument;
                if (actual.getId() == null || actual.getId().isEmpty()) {
                    return false;
                }

                return SERVICE_ID.equals(actual.getServiceId()) && ACCOUNT_ID.equals(actual.getAccountId()) &&
                       PLAN_ID.equals(actual.getPlanId()) && Collections.singletonMap("key", "value").equals(actual.getProperties());
            }
        }));
        verify(paymentService).addSubscription(any(Subscription.class), anyMap());
        verify(serviceRegistry).get(SERVICE_ID);
        verify(accountDao).saveBillingProperties(subscription.getId(), Collections.singletonMap("billingKey", "billingValue"));
        verify(subscriptionService).beforeCreateSubscription(any(Subscription.class));
        verify(subscriptionService).afterCreateSubscription(any(Subscription.class));
    }

    @Test
    public void shouldBeAbleToAddSubscriptionWithoutAddSubscriptionOnBTIfUserIsSystemAdmin() throws Exception {
        final NewSubscription newSubscription = DtoFactory.getInstance().createDto(NewSubscription.class)
                                                          .withAccountId(ACCOUNT_ID).withPlanId(PLAN_ID)
                                                          .withBillingProperties(Collections.singletonMap("billingKey", "billingValue"));
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(planDao.getPlanById(PLAN_ID)).thenReturn(plan);

        prepareSecurityContext("system/admin");

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, newSubscription);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        verify(accountDao).addSubscription(any(Subscription.class));
        verify(paymentService, never()).addSubscription(any(Subscription.class), anyMap());
        verify(accountDao).saveBillingProperties(subscription.getId(), Collections.singletonMap("billingKey", "billingValue"));
    }

    @Test
    public void shouldBeAbleToAddSubscriptionWithoutAddSubscriptionOnBTIfSubscriptionIsNotPaid() throws Exception {
        final NewSubscription newSubscription = DtoFactory.getInstance().createDto(NewSubscription.class)
                                                          .withAccountId(ACCOUNT_ID).withPlanId(PLAN_ID)
                                                          .withBillingProperties(Collections.singletonMap("billingKey", "billingValue"));
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(planDao.getPlanById(PLAN_ID)).thenReturn(plan.withPaid(false));
        when(accountDao.getByMember(USER_ID)).thenReturn(Arrays.asList(new Member().withRoles(Arrays.asList("account/owner"))
                                                                                   .withAccountId(ACCOUNT_ID)
                                                                                   .withUserId(USER_ID)));
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, newSubscription);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        SubscriptionDescriptor subscription = (SubscriptionDescriptor)response.getEntity();
        verify(accountDao).addSubscription(any(Subscription.class));
        verify(paymentService, never()).addSubscription(any(Subscription.class), anyMap());
        verify(accountDao).saveBillingProperties(subscription.getId(), Collections.singletonMap("billingKey", "billingValue"));
    }

    @Test
    public void shouldNotBeAbleToAddSubscriptionIfServiceIdIsUnknown() throws Exception {
        Subscription newSubscription = new Subscription().withAccountId(ACCOUNT_ID)
                                                         .withPlanId(PLAN_ID)
                                                         .withServiceId("UNKNOWN_SERVICE_ID")
                                                         .withProperties(Collections.singletonMap("TariffPlan", "monthly"));
        when(serviceRegistry.get("UNKNOWN_SERVICE_ID")).thenReturn(null);
        when(planDao.getPlanById(PLAN_ID)).thenReturn(plan.withPaid(false));

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, newSubscription);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity().toString(), "Unknown serviceId is used");
        verify(accountDao, times(0)).addSubscription(any(Subscription.class));
    }

    @Test
    public void shouldNotBeAbleToAddSubscriptionIfNoDataSent() throws Exception {
        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, null);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity().toString(), "New subscription required");
        verifyZeroInteractions(accountDao, subscriptionService, serviceRegistry);
    }

    @Test
    public void shouldBeAbleToRemoveSubscriptionBySystemAdmin() throws Exception {
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );

        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry).get(SERVICE_ID);
        verify(accountDao).removeSubscription(SUBSCRIPTION_ID);
        verify(accountDao).removeBillingProperties(SUBSCRIPTION_ID);
        verify(subscriptionService).onRemoveSubscription(any(Subscription.class));
    }

    @Test
    public void shouldBeAbleToRemoveSubscriptionByAccountOwner() throws Exception {
        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withAccountId(ACCOUNT_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry).get(SERVICE_ID);
        verify(accountDao).removeSubscription(SUBSCRIPTION_ID);
        verify(accountDao).removeBillingProperties(SUBSCRIPTION_ID);
        verify(subscriptionService).onRemoveSubscription(any(Subscription.class));
    }

    @Test
    public void shouldRespondAccessDeniedIfUserIsNotAccountOwnerOnRemoveSubscription() throws Exception {
        ArrayList<Member> memberships = new ArrayList<>(2);
        Member am = new Member().withRoles(Arrays.asList("account/owner"))
                                .withAccountId("fake_id");
        memberships.add(am);
        Member am2 = new Member().withRoles(Arrays.asList("account/member"))
                                 .withAccountId(ACCOUNT_ID);
        memberships.add(am2);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                new Subscription().withId(SUBSCRIPTION_ID)
                                  .withServiceId(SERVICE_ID)
                                  .withAccountId(ACCOUNT_ID)
                                  .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity(), "Access denied");
    }

    @Test
    public void shouldBeAbleToGetAccountMembers() throws Exception {
        when(accountDao.getById(account.getId())).thenReturn(account);
        when(accountDao.getMembers(account.getId()))
                .thenReturn(Arrays.asList(new Member().withRoles(Collections.<String>emptyList())
                                                      .withUserId(USER_ID)
                                                      .withAccountId(account.getId())));

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + account.getId() + "/members", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(accountDao).getMembers(account.getId());
        @SuppressWarnings("unchecked") List<MemberDescriptor> members = (List<MemberDescriptor>)response.getEntity();
        assertEquals(members.size(), 1);
        MemberDescriptor member = members.get(0);
        assertEquals(member.getLinks().size(), 2);
    }

    @Test
    public void shouldBeAbleToAddMember() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + account.getId() + "/members?userid=" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(accountDao).addMember(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveMember() throws Exception {
        Member accountMember = new Member().withUserId(USER_ID)
                                           .withAccountId(ACCOUNT_ID)
                                           .withRoles(Arrays.asList("account/member"));
        Member accountOwner = new Member().withUserId("owner_holder")
                                          .withAccountId(ACCOUNT_ID)
                                          .withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountMember, accountOwner));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao).removeMember(accountMember);
    }

    @Test
    public void shouldNotBeAbleToRemoveLastAccountOwner() throws Exception {
        Member accountOwner = new Member().withUserId(USER_ID)
                                          .withAccountId(ACCOUNT_ID)
                                          .withRoles(Arrays.asList("account/owner"));
        Member accountMember = new Member().withUserId("member_holder")
                                           .withAccountId(ACCOUNT_ID)
                                           .withRoles(Arrays.asList("account/member"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountOwner, accountMember));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getEntity().toString(), "Account should have at least 1 owner");
    }

    @Test
    public void shouldBeAbleToRemoveAccountOwnerIfOtherOneExists() throws Exception {
        Member accountOwner = new Member().withUserId(USER_ID)
                                          .withAccountId(ACCOUNT_ID)
                                          .withRoles(Arrays.asList("account/owner"));
        Member accountOwner2 = new Member().withUserId("owner_holder")
                                           .withAccountId(ACCOUNT_ID)
                                           .withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountOwner, accountOwner2));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao).removeMember(accountOwner);
    }

    protected void verifyLinksRel(List<Link> links, List<String> rels) {
        assertEquals(links.size(), rels.size());
        for (String rel : rels) {
            boolean linkPresent = false;
            int i = 0;
            for (; i < links.size() && !linkPresent; i++) {
                linkPresent = links.get(i).getRel().equals(rel);
            }
            if (!linkPresent) {
                fail(String.format("Given links do not contain link with rel = %s", rel));
            }
        }
    }

    private String[] getRoles(Class<? extends Service> clazz, String methodName) {
        for (Method one : clazz.getMethods()) {
            if (one.getName().equals(methodName)) {
                if (one.isAnnotationPresent(RolesAllowed.class)) {
                    return one.getAnnotation(RolesAllowed.class).value();
                } else {
                    return new String[0];
                }
            }
        }
        throw new IllegalArgumentException(String.format("Class %s does not have method with name %s", clazz.getName(), methodName));
    }

    private List<String> generateRels(String role) {
        final List<String> rels = new LinkedList<>();
        rels.add(Constants.LINK_REL_GET_MEMBERS);
        rels.add(Constants.LINK_REL_GET_ACCOUNTS);
        rels.add(Constants.LINK_REL_GET_SUBSCRIPTIONS);
        rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_ID);
        switch (role) {
            case "system/admin":
                rels.add(Constants.LINK_REL_REMOVE_ACCOUNT);
            case "system/manager":
                rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_NAME);
                break;
        }
        return rels;
    }

    protected ContainerResponse makeRequest(String method, String path, String contentType, Object toSend) throws Exception {
        Map<String, List<String>> headers = null;
        if (contentType != null) {
            headers = new HashMap<>();
            headers.put("Content-Type", Arrays.asList(contentType));
        }
        byte[] data = null;
        if (toSend != null) {
            data = JsonHelper.toJson(toSend).getBytes();
        }
        return launcher.service(method, path, BASE_URI, headers, data, null, environmentContext);
    }

    protected void prepareSecurityContext(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        if (!role.equals("system/admin") && !role.equals("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}