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


import sun.security.acl.PrincipalImpl;

import com.codenvy.api.account.server.AccountService;
import com.codenvy.api.account.server.Constants;
import com.codenvy.api.account.server.PaymentService;
import com.codenvy.api.account.server.SubscriptionService;
import com.codenvy.api.account.server.SubscriptionServiceRegistry;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.account.shared.dto.SubscriptionHistoryEvent;
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
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
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

import static com.codenvy.api.account.shared.dto.Subscription.State.ACTIVE;
import static com.codenvy.api.account.shared.dto.Subscription.State.WAIT_FOR_PAYMENT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
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
    private final User   user            = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);
    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

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

    private ArrayList<AccountMembership> memberships;

    protected ProviderBinder     providers;
    protected ResourceBinderImpl resources;
    protected RequestHandlerImpl requestHandler;
    protected ResourceLauncher   launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        resources = new ResourceBinderImpl();
        providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        dependencies.addComponent(SubscriptionServiceRegistry.class, serviceRegistry);
        dependencies.addComponent(PaymentService.class, paymentService);
        resources.addResource(AccountService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);
        account = DtoFactory.getInstance().createDto(Account.class)
                            .withId(ACCOUNT_ID)
                            .withName(ACCOUNT_NAME)
                            .withAttributes(new ArrayList<>(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                                    .withName("secret")
                                                                                    .withValue("big secret")
                                                                                    .withDescription(
                                                                                            "DON'T TELL ANYONE ABOUT IT!"))));
//        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));
        memberships = new ArrayList<>(1);
        AccountMembership ownerMembership = DtoFactory.getInstance().createDto(AccountMembership.class);
        ownerMembership.setName(account.getName());
        ownerMembership.setId(account.getId());
        ownerMembership.setRoles(Arrays.asList("account/owner"));
        memberships.add(ownerMembership);
//        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
//        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
//        when(accountDao.getByName(ACCOUNT_NAME)).thenReturn(account);

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
        Account created = (Account)response.getEntity();
        verifyLinksRel(created.getLinks(), generateRels(role));
        verify(accountDao, times(1)).create(any(Account.class));
        Member expected = DtoFactory.getInstance().createDto(Member.class)
                                    .withAccountId(created.getId())
                                    .withUserId(USER_ID)
                                    .withRoles(Arrays.asList("account/owner"));
        verify(accountDao, times(1)).addMember(expected);
    }

    @Test
    public void shouldNotBeAbleToCreateAccountWithNotValidAttributes() throws Exception {
        account.getAttributes().add(DtoFactory.getInstance().createDto(Attribute.class).withName("codenvy:god_mode").withValue("true"));

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

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        //safe cast cause AccountService#getMemberships always returns List<AccountMembership
        @SuppressWarnings("unchecked") List<AccountMembership> currentAccounts =
                (List<AccountMembership>)response.getEntity();
        assertEquals(currentAccounts.size(), 1);
        assertEquals(currentAccounts.get(0).getRoles().get(0), "account/owner");
        verify(accountDao, times(1)).getByMember(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetMembershipsOfSpecificUser() throws Exception {
        User user = DtoFactory.getInstance().createDto(User.class).withId("ANOTHER_USER_ID").withEmail("ANOTHER_USER_EMAIL");
        ArrayList<AccountMembership> memberships = new ArrayList<>();
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.setId("fake_id");
        am.setName("fake_name");
        am.setRoles(Arrays.asList("account/member"));
        memberships.add(am);

        when(userDao.getById("ANOTHER_USER_ID")).thenReturn(user);
        when(accountDao.getByMember("ANOTHER_USER_ID")).thenReturn(memberships);

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/list?userid=" + "ANOTHER_USER_ID", null, null);
        @SuppressWarnings("unchecked") List<AccountMembership> currentAccounts =
                (List<AccountMembership>)response.getEntity();
        assertEquals(currentAccounts.size(), 1);
        assertEquals(currentAccounts.get(0), am);
    }

    @Test
    public void shouldBeAbleToGetAccountById() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        String[] roles = getRoles(AccountService.class, "getById");

        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + ACCOUNT_ID, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Account actual = (Account)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(accountDao, times(roles.length)).getById(ACCOUNT_ID);
    }

    @Test
    public void shouldBeAbleToUpdateAccount() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        List<Attribute> attributes = Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                             .withName("newAttribute")
                                                             .withValue("someValue")
                                                             .withDescription("Description"));
        Account toUpdate = DtoFactory.getInstance().createDto(Account.class)
                                     .withName("newName")
                                     .withAttributes(attributes);

        prepareSecurityContext("account/owner");
        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Account actual = (Account)response.getEntity();
        assertEquals(actual.getAttributes().size(), 2);
        assertEquals(actual.getName(), "newName");
    }

    @Test
    public void shouldBeAbleToRewriteAttributesWhenUpdatingAccount() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        List<Attribute> currentAttributes = new ArrayList<>();
        currentAttributes.add(DtoFactory.getInstance().createDto(Attribute.class)
                                        .withName("newAttribute")
                                        .withValue("someValue")
                                        .withDescription("Description"));
        currentAttributes.add(DtoFactory.getInstance().createDto(Attribute.class)
                                        .withName("oldAttribute")
                                        .withValue("oldValue")
                                        .withDescription("Description"));
        account.setAttributes(currentAttributes);

        List<Attribute> updates = new ArrayList<>(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                          .withName("newAttribute")
                                                                          .withValue("OTHER_VALUE")
                                                                          .withDescription("Description"),
                                                                DtoFactory.getInstance().createDto(Attribute.class)
                                                                          .withName("newAttribute2")
                                                                          .withValue("someValue2")
                                                                          .withDescription("Description2")
                                                               ));
        Account toUpdate = DtoFactory.getInstance().createDto(Account.class).withAttributes(updates);

        prepareSecurityContext("account/owner");
        ContainerResponse response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Account actual = (Account)response.getEntity();
        assertEquals(actual.getName(), ACCOUNT_NAME);
        assertEquals(actual.getAttributes().size(), 3);
        for (Attribute attribute : actual.getAttributes()) {
            if (attribute.getName().equals("newAttribute") && !attribute.getValue().equals("OTHER_VALUE")) {
                fail("Attribute should be replaced");
            }
        }
    }

    @Test
    public void shouldBeAbleToAddNewAttribute() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        int countBefore = account.getAttributes().size();

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(account.getAttributes().size(), countBefore + 1);
        verify(accountDao, times(1)).update(account);
    }

    @Test
    public void shouldNotBeAbleToAddAttributeWithIncorrectName() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("codenvy_newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy_newAttribute' is not valid");

        newAttribute.setName("");

        response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name '' is not valid");

        newAttribute.setName(null);

        response = makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'null' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        int countBefore = account.getAttributes().size();
        assertTrue(countBefore > 0);
        Attribute existed = account.getAttributes().get(0);
        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute?name=" + existed.getName(), null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(account.getAttributes().size(), countBefore - 1);
    }

    @Test
    public void shouldNotBeAbleToUpdateAccountWithAlreadyExistedName() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        when(accountDao.getByName("TO_UPDATE"))
                .thenReturn(DtoFactory.getInstance().createDto(Account.class).withName("TO_UPDATE"));

        prepareSecurityContext("account/owner");

        Account toUpdate = DtoFactory.getInstance().createDto(Account.class).withName("TO_UPDATE");

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
            Account actual = (Account)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(accountDao, times(roles.length)).getByName(ACCOUNT_NAME);
    }

    @Test
    public void shouldBeAbleToGetSubscriptionsOfSpecificAccount() throws Exception {
        when(accountDao.getSubscriptions(ACCOUNT_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withProperties(Collections.<String, String>emptyMap())
                                                                              ));

        prepareSecurityContext("system/admin");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        //safe cast cause AccountService#getSubscriptions always returns List<Subscription>
        @SuppressWarnings("unchecked") List<Subscription> subscriptions = (List<Subscription>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        assertEquals(subscriptions.get(0).getLinks().size(), 2);
        List<Link> actualLinks = subscriptions.get(0).getLinks();
        assertEqualsNoOrder(actualLinks.toArray(), new Link[]{DtoFactory.getInstance().createDto(Link.class).withRel(
                Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE).withHref(
                SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID), DtoFactory.getInstance().createDto(Link.class).withRel(
                Constants.LINK_REL_GET_SUBSCRIPTION).withMethod(
                HttpMethod.GET).withHref(
                SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID).withProduces(MediaType.APPLICATION_JSON)});

        verify(accountDao, times(1)).getSubscriptions(ACCOUNT_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionBySystemAdmin() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("system/admin");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Subscription subscription = (Subscription)response.getEntity();
        assertEquals(subscription.getLinks(), Arrays.asList(DtoFactory.getInstance().createDto(Link.class).withRel(
                Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE).withHref(
                SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID)));
        verify(accountDao, times(1)).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionByAccountOwner() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withAccountId("ANOTHER_ACCOUNT_ID")
                          .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        when(accountDao.getByMember(USER_ID))
                .thenReturn(Arrays.asList(
                        (AccountMembership)DtoFactory.getInstance().createDto(AccountMembership.class)
                                                     .withRoles(Arrays.asList("account/owner")).withId("ANOTHER_ACCOUNT_ID")
                                         ));

        prepareSecurityContext("user");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Subscription subscription = (Subscription)response.getEntity();
        assertEquals(subscription.getLinks(), Arrays.asList(DtoFactory.getInstance().createDto(Link.class).withRel(
                Constants.LINK_REL_REMOVE_SUBSCRIPTION).withMethod(HttpMethod.DELETE).withHref(
                SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID)));
        verify(accountDao, times(1)).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldBeAbleToGetSpecificSubscriptionByAccountMember() throws Exception {
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withAccountId("ANOTHER_ACCOUNT_ID")
                          .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        when(accountDao.getByMember(USER_ID))
                .thenReturn(Arrays.asList(
                        (AccountMembership)DtoFactory.getInstance().createDto(AccountMembership.class)
                                                     .withRoles(Arrays.asList("account/member")).withId("ANOTHER_ACCOUNT_ID")
                                         ));

        prepareSecurityContext("user");

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Subscription subscription = (Subscription)response.getEntity();
        assertTrue(subscription.getLinks().isEmpty());
        verify(accountDao, times(1)).getSubscriptionById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldRespondForbiddenIfUserIsNotMemberOrOwnerOfAccountOnGetSubscriptionById() throws Exception {
        ArrayList<AccountMembership> memberships = new ArrayList<>();
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.withRoles(Arrays.asList("account/owner")).withId("fake_id").withName("fake_name");
        memberships.add(am);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
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
        ArrayList<AccountMembership> memberships = new ArrayList<>();
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.withRoles(Arrays.asList("account/owner")).withId("fake_id").withName("fake_name");
        memberships.add(am);
        AccountMembership am2 = DtoFactory.getInstance().createDto(AccountMembership.class);
        am2.withRoles(Arrays.asList("account/member")).withId(ACCOUNT_ID).withName(ACCOUNT_NAME);
        memberships.add(am2);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                              .withAccountId(ACCOUNT_ID)
                                              .withServiceId(SERVICE_ID)
                                              .withStartDate(System.currentTimeMillis())
                                              .withEndDate(System.currentTimeMillis())
                                              .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);

        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity(), "Access denied");
    }

    @Test
    public void shouldRespondPaymentRequiredIfAmountBiggerThan0OnAddSubscription() throws Exception {
        final Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                                    .withAccountId(ACCOUNT_ID)
                                                    .withServiceId(SERVICE_ID)
                                                    .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(subscriptionService.tarifficate(any(Subscription.class))).thenReturn(1000D);

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), 402);
        Subscription actualSubscription = DtoFactory.getInstance().clone((Subscription)response.getEntity());
        verifyLinksRel(actualSubscription.getLinks(),
                       Arrays.asList(Constants.LINK_REL_PURCHASE_SUBSCRIPTION, Constants.LINK_REL_REMOVE_SUBSCRIPTION));


        assertEquals(DtoFactory.getInstance().clone(actualSubscription).withEndDate(0).withStartDate(0).withLinks(null).withId(null),
                     subscription.withState(Subscription.State.WAIT_FOR_PAYMENT));
        verify(accountDao, times(1)).addSubscription(argThat(new ArgumentMatcher<Subscription>() {
            @Override
            public boolean matches(Object argument) {
                Subscription actualSubscription = DtoFactory.getInstance().clone((Subscription)argument);
                Subscription expectedSubscription =
                        DtoFactory.getInstance().clone(subscription).withState(WAIT_FOR_PAYMENT).withId(actualSubscription.getId())
                                  .withStartDate(actualSubscription.getStartDate()).withEndDate(actualSubscription.getEndDate());
                return expectedSubscription.equals(actualSubscription);

            }
        }));
        verify(accountDao).addSubscriptionHistoryEvent(any(SubscriptionHistoryEvent.class));
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(subscriptionService, times(1)).beforeCreateSubscription(any(Subscription.class));
        verify(subscriptionService, times(1)).beforeCreateSubscription(any(Subscription.class));
    }

    @Test
    public void shouldBeAbleToAddSubscription() throws Exception {
        final Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                                    .withAccountId(ACCOUNT_ID)
                                                    .withServiceId(SERVICE_ID)
                                                    .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao, times(1)).addSubscription(argThat(new ArgumentMatcher<Subscription>() {
            @Override
            public boolean matches(Object argument) {
                Subscription actualSubscription = DtoFactory.getInstance().clone((Subscription)argument);
                Subscription expectedSubscription =
                        DtoFactory.getInstance().clone(subscription).withState(ACTIVE).withId(actualSubscription.getId())
                                  .withStartDate(actualSubscription.getStartDate()).withEndDate(actualSubscription.getEndDate());
                return expectedSubscription.equals(actualSubscription);

            }
        }));
        verify(accountDao, times(1)).addSubscriptionHistoryEvent(any(SubscriptionHistoryEvent.class));
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(subscriptionService, times(1)).beforeCreateSubscription(any(Subscription.class));
        verify(subscriptionService, times(1)).afterCreateSubscription(any(Subscription.class));
    }

    @Test
    public void shouldNotBeAbleToAddSubscriptionIfServiceIdIsUnknown() throws Exception {
        Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                              .withAccountId(ACCOUNT_ID)
                                              .withServiceId("UNKNOWN_SERVICE_ID")
                                              .withStartDate(System.currentTimeMillis())
                                              .withEndDate(System.currentTimeMillis())
                                              .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get("UNKNOWN_SERVICE_ID")).thenReturn(null);

        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity().toString(), "Unknown serviceId is used");
        verify(accountDao, times(0)).addSubscription(any(Subscription.class));
    }

    @Test
    public void shouldNotBeAbleToAddSubscriptionIfNoDataSent() throws Exception {
        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, null);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity().toString(), "Missed subscription");
        verifyZeroInteractions(accountDao, subscriptionService, serviceRegistry);
    }

    @Test
    public void shouldBeAbleToRemoveSubscriptionBySystemAdmin() throws Exception {
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withProperties(Collections.<String, String>emptyMap())
                                                                        );

        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(accountDao, times(1)).removeSubscription(SUBSCRIPTION_ID);
        verify(accountDao, times(1)).addSubscriptionHistoryEvent(any(SubscriptionHistoryEvent.class));
        verify(subscriptionService, times(1)).onRemoveSubscription(any(Subscription.class));
    }

    @Test
    public void shouldBeAbleToRemoveSubscriptionByAccountOwner() throws Exception {
        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
                          .withServiceId(SERVICE_ID)
                          .withAccountId(ACCOUNT_ID)
                          .withProperties(Collections.<String, String>emptyMap())
                                                                        );
        prepareSecurityContext("user");

        ContainerResponse response =
                makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(accountDao, times(1)).removeSubscription(SUBSCRIPTION_ID);
        verify(accountDao, times(1)).addSubscriptionHistoryEvent(any(SubscriptionHistoryEvent.class));
        verify(subscriptionService, times(1)).onRemoveSubscription(any(Subscription.class));
    }

    @Test
    public void shouldRespondAccessDeniedIfUserIsNotAccountOwnerOnRemoveSubscription() throws Exception {
        ArrayList<AccountMembership> memberships = new ArrayList<>();
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.withRoles(Arrays.asList("account/owner")).withId("fake_id").withName("fake_name");
        memberships.add(am);
        AccountMembership am2 = DtoFactory.getInstance().createDto(AccountMembership.class);
        am2.withRoles(Arrays.asList("account/member")).withId(ACCOUNT_ID).withName(ACCOUNT_NAME);
        memberships.add(am2);

        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);
        when(accountDao.getSubscriptionById(SUBSCRIPTION_ID)).thenReturn(
                DtoFactory.getInstance().createDto(Subscription.class)
                          .withId(SUBSCRIPTION_ID)
                          .withStartDate(System.currentTimeMillis())
                          .withEndDate(System.currentTimeMillis())
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
        when(accountDao.getMembers(account.getId()))
                .thenReturn(Arrays.asList(
                        DtoFactory.getInstance().createDto(Member.class).withRoles(Collections.<String>emptyList()).withUserId(USER_ID)
                                  .withAccountId(account.getId())
                                         ));

        ContainerResponse response = makeRequest(HttpMethod.GET, SERVICE_PATH + "/" + account.getId() + "/members", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(accountDao, times(1)).getMembers(account.getId());
        @SuppressWarnings("unchecked") List<Member> members = (List<Member>)response.getEntity();
        assertEquals(members.size(), 1);
        Member member = members.get(0);
        assertEquals(member.getLinks().size(), 1);
        Link removeMember = members.get(0).getLinks().get(0);
        assertEquals(removeMember, DtoFactory.getInstance().createDto(Link.class)
                                             .withRel(Constants.LINK_REL_REMOVE_MEMBER)
                                             .withHref(SERVICE_PATH + "/" + member.getAccountId() + "/members/" + member.getUserId())
                                             .withMethod(HttpMethod.DELETE));
    }

    @Test
    public void shouldBeAbleToAddMember() throws Exception {
        ContainerResponse response =
                makeRequest(HttpMethod.POST, SERVICE_PATH + "/" + account.getId() + "/members?userid=" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao, times(1)).addMember(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveMember() throws Exception {
        Member accountMember = DtoFactory.getInstance().createDto(Member.class)
                                         .withUserId(USER_ID)
                                         .withAccountId(ACCOUNT_ID)
                                         .withRoles(Arrays.asList("account/member"));
        Member accountOwner = DtoFactory.getInstance().createDto(Member.class)
                                        .withUserId("owner_holder")
                                        .withAccountId(ACCOUNT_ID)
                                        .withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountMember, accountOwner));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao, times(1)).removeMember(accountMember);
    }

    @Test
    public void shouldNotBeAbleToRemoveLastAccountOwner() throws Exception {
        Member accountOwner = DtoFactory.getInstance().createDto(Member.class)
                                        .withUserId(USER_ID)
                                        .withAccountId(ACCOUNT_ID)
                                        .withRoles(Arrays.asList("account/owner"));
        Member accountMember = DtoFactory.getInstance().createDto(Member.class)
                                         .withUserId("member_holder")
                                         .withAccountId(ACCOUNT_ID)
                                         .withRoles(Arrays.asList("account/member"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountOwner, accountMember));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getEntity().toString(), "Account should have at least 1 owner");
    }

    @Test
    public void shouldBeAbleToRemoveAccountOwnerIfOtherOneExists() throws Exception {
        Member accountOwner = DtoFactory.getInstance().createDto(Member.class)
                                        .withUserId(USER_ID)
                                        .withAccountId(ACCOUNT_ID)
                                        .withRoles(Arrays.asList("account/owner"));
        Member accountOwner2 = DtoFactory.getInstance().createDto(Member.class)
                                         .withUserId("owner_holder")
                                         .withAccountId(ACCOUNT_ID)
                                         .withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(ACCOUNT_ID)).thenReturn(Arrays.asList(accountOwner, accountOwner2));

        ContainerResponse response = makeRequest(HttpMethod.DELETE, SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao, times(1)).removeMember(accountOwner);
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
        switch (role) {
            case "system/admin":
                rels.add(Constants.LINK_REL_REMOVE_ACCOUNT);
            case "system/manager":
                rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_NAME);
                rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_ID);
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