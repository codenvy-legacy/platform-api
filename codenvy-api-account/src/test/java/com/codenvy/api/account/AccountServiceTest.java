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
import com.codenvy.api.account.server.SubscriptionEvent;
import com.codenvy.api.account.server.SubscriptionService;
import com.codenvy.api.account.server.SubscriptionServiceRegistry;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.AccountMembership;
import com.codenvy.api.account.shared.dto.Attribute;
import com.codenvy.api.account.shared.dto.Member;
import com.codenvy.api.account.shared.dto.Subscription;
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
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.security.RolesAllowed;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests for Account Service
 *
 * @author Eugene Voevodin
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

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private SubscriptionServiceRegistry serviceRegistry;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EnvironmentContext environmentContext;

    private Account account;

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
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));
        ArrayList<AccountMembership> memberships = new ArrayList<>(1);
        AccountMembership ownerMembership = DtoFactory.getInstance().createDto(AccountMembership.class);
        ownerMembership.setName(account.getName());
        ownerMembership.setId(account.getId());
        ownerMembership.setRoles(Arrays.asList("account/owner"));
        memberships.add(ownerMembership);
        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);
        String USER_EMAIL = "account@mail.com";
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
        when(userDao.getById(USER_ID)).thenReturn(user);
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        when(accountDao.getByName(ACCOUNT_NAME)).thenReturn(account);
    }

    @Test
    public void shouldBeAbleToCreateAccount() throws Exception {
        when(accountDao.getByName(account.getName())).thenThrow(new NotFoundException("Account not found"));
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.<Account>emptyList());
        String role = "user";
        prepareSecurityContext(role);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, account);

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
    public void shouldNotBeAbleToCreateAccountIfUserAlreadyHasOne() throws Exception {
        prepareSecurityContext("account/owner");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, account);
        assertEquals(response.getEntity().toString(), "Account which owner is " + USER_ID + " already exists");
    }

    @Test
    public void shouldNotBeAbleToCreateAccountWithoutName() throws Exception {
        when(accountDao.getByName(account.getName())).thenReturn(null);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.<Account>emptyList());
        account.setName(null);
        String role = "user";
        prepareSecurityContext(role);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, account);

        assertEquals(response.getEntity().toString(), "Account name required");
    }

    @Test
    public void shouldBeAbleToGetMemberships() throws Exception {
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));

        ContainerResponse response = makeRequest("GET", SERVICE_PATH, null, null);

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
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(account));
        ArrayList<AccountMembership> memberships = new ArrayList<>();
        AccountMembership am = DtoFactory.getInstance().createDto(AccountMembership.class);
        am.setId("fake_id");
        am.setName("fake_name");
        am.setRoles(Arrays.asList("account/member"));
        //get default owner membership
        memberships.addAll(accountDao.getByMember(USER_ID));
        memberships.add(am);
        when(accountDao.getByMember(USER_ID)).thenReturn(memberships);

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/list?userid=" + USER_ID, null, null);
        //safe cast cause AccountService#getMembershipsOfSpecificUser always returns List<AccountMembership>
        @SuppressWarnings("unchecked") List<AccountMembership> currentAccounts =
                (List<AccountMembership>)response.getEntity();
        assertEquals(currentAccounts.size(), 2);
        assertEquals(currentAccounts.get(1).getRoles().get(0), "account/member");
        assertEquals(currentAccounts.get(0).getRoles().get(0), "account/owner");
    }

    @Test
    public void shouldBeAbleToGetAccountById() throws Exception {
        String[] roles = getRoles(AccountService.class, "getById");

        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ACCOUNT_ID, null, null);

            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Account actual = (Account)response.getEntity();
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(accountDao, times(roles.length)).getById(ACCOUNT_ID);
    }

    @Test
    public void shouldBeAbleToUpdateAccount() throws Exception {
        List<Attribute> attributes = Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                             .withName("newAttribute")
                                                             .withValue("someValue")
                                                             .withDescription("Description"));
        Account toUpdate = DtoFactory.getInstance().createDto(Account.class)
                                     .withName("newName")
                                     .withAttributes(attributes);

        prepareSecurityContext("user");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Account actual = (Account)response.getEntity();
        assertEquals(actual.getAttributes().size(), 2);
        assertEquals(actual.getName(), "newName");
    }

    @Test
    public void shouldBeAbleToRewriteAttributesWhenUpdatingAccount() throws Exception {
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

        prepareSecurityContext("user");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);

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
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        int countBefore = account.getAttributes().size();

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(account.getAttributes().size(), countBefore + 1);
        verify(accountDao, times(1)).update(account);
    }

    @Test
    public void shouldNotBeAbleToAddAttributeWithIncorrectName() throws Exception {
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("codenvy_newAttribute")
                                           .withValue("someValue")
                                           .withDescription("Description");
        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy_newAttribute' is not valid");

        newAttribute.setName("");

        response = makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name '' is not valid");

        newAttribute.setName(null);

        response = makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertNotEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(response.getEntity().toString(), "Attribute name 'null' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        int countBefore = account.getAttributes().size();
        assertTrue(countBefore > 0);
        Attribute existed = account.getAttributes().get(0);
        ContainerResponse response =
                makeRequest("DELETE", SERVICE_PATH + "/" + ACCOUNT_ID + "/attribute?name=" + existed.getName(), null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(account.getAttributes().size(), countBefore - 1);
    }

    @Test
    public void shouldNotBeAbleToUpdateAccountWithAlreadyExistedName() throws Exception {
        when(accountDao.getByName("TO_UPDATE"))
                .thenReturn(DtoFactory.getInstance().createDto(Account.class).withName("TO_UPDATE"));

        prepareSecurityContext("user");

        Account toUpdate = DtoFactory.getInstance().createDto(Account.class).withName("TO_UPDATE");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID, MediaType.APPLICATION_JSON, toUpdate);
        assertNotEquals(response.getStatus(), Response.Status.OK);
        assertEquals(response.getEntity().toString(), "Account with name TO_UPDATE already exists");
    }

    @Test
    public void shouldBeAbleToGetAccountByName() throws Exception {
        String[] roles = getRoles(AccountService.class, "getByName");
        for (String role : roles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/find?name=" + ACCOUNT_NAME, null, null);

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

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        //safe cast cause AccountService#getSubscriptions always returns List<Subscription>
        @SuppressWarnings("unchecked") List<Subscription> subscriptions = (List<Subscription>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        assertEquals(subscriptions.get(0).getLinks().size(), 1);
        Link removeSubscription = subscriptions.get(0).getLinks().get(0);
        assertEquals(removeSubscription, DtoFactory.getInstance().createDto(Link.class)
                                                   .withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION)
                                                   .withMethod("DELETE")
                                                   .withHref(SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID));
        verify(accountDao, times(1)).getSubscriptions(ACCOUNT_ID);
    }

    @Test
    public void shouldNotBeAbleToGetSubscriptionsFromAccountWhereCurrentUserIsNotMember() throws Exception {
        when(accountDao.getByMember(USER_ID)).thenReturn(new ArrayList<AccountMembership>());
        when(accountDao.getByOwner(USER_ID))
                .thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Account.class).withId("NOT_SAME")));

        prepareSecurityContext("user");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", null, null);

        assertNotEquals(Response.Status.OK, response.getStatus());
    }

    @Test
    public void shouldBeAbleToAddSubscription() throws Exception {
        Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                              .withAccountId(ACCOUNT_ID)
                                              .withServiceId(SERVICE_ID)
                                              .withStartDate(System.currentTimeMillis())
                                              .withEndDate(System.currentTimeMillis())
                                              .withProperties(Collections.<String, String>emptyMap());
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(accountDao, times(1)).addSubscription(any(Subscription.class));
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(subscriptionService, times(1)).notifyHandlers(any(SubscriptionEvent.class));
    }

    @Test
    public void shouldBeAbleToRemoveSubscription() throws Exception {
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
                makeRequest("DELETE", SERVICE_PATH + "/subscriptions/" + SUBSCRIPTION_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(serviceRegistry, times(1)).get(SERVICE_ID);
        verify(accountDao, times(1)).removeSubscription(SUBSCRIPTION_ID);
        verify(subscriptionService, times(1)).notifyHandlers(any(SubscriptionEvent.class));
    }

    @Test
    public void shouldBeAbleToGetAccountMembers() throws Exception {
        when(accountDao.getMembers(account.getId()))
                .thenReturn(Arrays.asList(
                        DtoFactory.getInstance().createDto(Member.class).withRoles(Collections.<String>emptyList()).withUserId(USER_ID)
                                  .withAccountId(account.getId())
                                         ));

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + account.getId() + "/members", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(accountDao, times(1)).getMembers(account.getId());
        //safe cast cause AccountService#getMembers always returns List<Member>
        @SuppressWarnings("unchecked") List<Member> members = (List<Member>)response.getEntity();
        assertEquals(members.size(), 1);
        Member member = members.get(0);
        assertEquals(member.getLinks().size(), 1);
        Link removeMember = members.get(0).getLinks().get(0);
        assertEquals(removeMember, DtoFactory.getInstance().createDto(Link.class)
                                             .withRel(Constants.LINK_REL_REMOVE_MEMBER)
                                             .withHref(SERVICE_PATH + "/" + member.getAccountId() + "/members/" + member.getUserId())
                                             .withMethod("DELETE"));
    }

    @Test
    public void shouldBeAbleToAddMember() throws Exception {
        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + account.getId() + "/members?userid=" + USER_ID, null, null);

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

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

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

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

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

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + ACCOUNT_ID + "/members/" + USER_ID, null, null);

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