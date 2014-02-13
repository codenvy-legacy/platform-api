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
package com.codenvy.api.account;

import sun.security.acl.PrincipalImpl;

import com.codenvy.api.account.server.AccountService;
import com.codenvy.api.account.server.Constants;
import com.codenvy.api.account.server.SubscriptionService;
import com.codenvy.api.account.server.SubscriptionServiceRegistry;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.account.shared.dto.Subscription;
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
import static org.testng.Assert.fail;

/**
 * TODO
 * Tests for Account Service
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.account.server.AccountService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class AccountServiceTest {

    private final String BASE_URI     = "http://localhost/service";
    private final String SERVICE_PATH = BASE_URI + "/account";
    private final String USER_ID      = "user123abc456def";
    private final String USER_EMAIL   = "Cooper@mail.com";
    private final String ACCOUNT_ID   = "account0xffffffffff";
    private final String ACCOUNT_NAME = "Sheldon";
    private final String SERVICE_ID   = "IDE_SERVICE";

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
        account = DtoFactory.getInstance().createDto(Account.class).withId(ACCOUNT_ID).withOwner(USER_ID).withName(ACCOUNT_NAME);
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
        when(userDao.getById(USER_ID)).thenReturn(user);
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
    }

    @Test
    public void shouldBeAbleToCreateAccount() throws Exception {
        String[] availableRoles = new String[]{"account/owner", "system/manager", "system/admin"};

        for (String role : availableRoles) {
            prepareSecurityContext(role);

            ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, account);

            assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
            Account created = (Account)response.getEntity();
            verifyLinksRel(created.getLinks(), generateRels(role));
        }
        verify(accountDao, times(availableRoles.length)).create(any(Account.class));
    }

    @Test
    public void shouldBeAbleToGetCurrentAccount() throws Exception {
        when(accountDao.getByOwner(USER_ID)).thenReturn(account);
        prepareSecurityContext("account/owner");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Account current = (Account)response.getEntity();
        verifyLinksRel(current.getLinks(), generateRels("account/owner"));
        verify(accountDao, times(1)).getByOwner(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetAccountById() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
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
    public void shouldBeAbleToGetAccountByName() throws Exception {
        when(accountDao.getByName(ACCOUNT_NAME)).thenReturn(account);

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
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToGetSubscriptionsOfCurrentAccount() throws Exception {
        when(accountDao.getByOwner(USER_ID)).thenReturn(account);
        when(accountDao.getSubscriptions(ACCOUNT_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Subscription.class).withStartDate("22-12-13").withEndDate("22-01-13")
                          .withServiceId(SERVICE_ID).withProperties(Collections.EMPTY_MAP)));
        prepareSecurityContext("account/owner");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Subscription> subscriptions = (List<Subscription>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        //account owner should not have possibility to remove any subscriptions
        assertEquals(subscriptions.get(0).getLinks().size(), 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToGetSubscriptionsOfSpecificAccount() throws Exception {
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        when(accountDao.getSubscriptions(ACCOUNT_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Subscription.class).withStartDate("22-12-13").withEndDate("22-01-13")
                          .withServiceId(SERVICE_ID).withProperties(Collections.EMPTY_MAP)));

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        List<Subscription> subscriptions = (List<Subscription>)response.getEntity();
        assertEquals(subscriptions.size(), 1);
        assertEquals(subscriptions.get(0).getLinks().size(), 1);
        Link removeSubscription = subscriptions.get(0).getLinks().get(0);
        assertEquals(removeSubscription, DtoFactory.getInstance().createDto(Link.class)
                                                   .withRel(Constants.LINK_REL_REMOVE_SUBSCRIPTION)
                                                   .withMethod("DELETE")
                                                   .withHref(SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions/" + SERVICE_ID));
        verify(accountDao, times(1)).getSubscriptions(ACCOUNT_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToAddSubscription() throws Exception {
        Subscription subscription = DtoFactory.getInstance().createDto(Subscription.class)
                                              .withServiceId(SERVICE_ID)
                                              .withStartDate("22-12-13")
                                              .withEndDate("22-01-14")
                                              .withProperties(Collections.EMPTY_MAP);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(account);
        when(serviceRegistry.get(SERVICE_ID)).thenReturn(subscriptionService);

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + ACCOUNT_ID + "/subscriptions", MediaType.APPLICATION_JSON, subscription);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
    }

    protected void verifyLinksRel(List<Link> links, List<String> rels) {
        assertEquals(links.size(), rels.size());
        for (String rel : rels) {
            boolean linkPresent = false;
            for (int i = 0; i < links.size() && !linkPresent; i++) {
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
        switch (role) {
            case "account/owner":
                rels.add(Constants.LINK_REL_GET_CURRENT_ACCOUNT);
                rels.add(Constants.LINK_REL_UPDATE_ACCOUNT);
                rels.add(Constants.LINK_REL_GET_MEMBERS);
                rels.add(Constants.LINK_REL_GET_SUBSCRIPTIONS);
                break;
            case "system/admin":
                rels.add(Constants.LINK_REL_REMOVE_ACCOUNT);
            case "system/manager":
                rels.add(Constants.LINK_REL_GET_MEMBERS);
                rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_ID);
                rels.add(Constants.LINK_REL_GET_ACCOUNT_BY_NAME);
                rels.add(Constants.LINK_REL_GET_SUBSCRIPTIONS);
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
        when(securityContext.isUserInRole("user")).thenReturn(true);
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}