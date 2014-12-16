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
package com.codenvy.api.user.server;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.ApiExceptionMapper;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.commons.json.JsonHelper;

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
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link UserService}
 *
 * @author Eugene Veovodin
 * @author Max Shaposhnik
 */
@Listeners(value = {MockitoTestNGListener.class})
public class UserServiceTest {

    private final String BASE_URI     = "http://localhost/service";
    private final String SERVICE_PATH = BASE_URI + "/user";

    @Mock
    private UserProfileDao     userProfileDao;
    @Mock
    private UserDao            userDao;
    @Mock
    private TokenValidator     tokenValidator;
    @Mock
    private UriInfo            uriInfo;
    @Mock
    private EnvironmentContext environmentContext;
    @Mock
    private SecurityContext    securityContext;
    private ResourceLauncher   launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        ResourceBinderImpl resources = new ResourceBinderImpl();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserProfileDao.class, userProfileDao);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(TokenValidator.class, tokenValidator);
        resources.addResource(UserService.class, null);
        EverrestProcessor processor = new EverrestProcessor(resources,
                                                            new ApplicationProviderBinder(),
                                                            dependencies,
                                                            new EverrestConfiguration(),
                                                            null);
        launcher = new ResourceLauncher(processor);
        ProviderBinder providerBinder = ProviderBinder.getInstance();
        providerBinder.addExceptionMapper(ApiExceptionMapper.class);
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, providerBinder));
        //set up user
        final User user = createUser();
        com.codenvy.commons.env.EnvironmentContext.getCurrent().setUser(new com.codenvy.commons.user.User() {

            @Override
            public String getName() {
                return user.getEmail();
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public String getToken() {
                return null;
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

    @Test
    public void shouldBeAbleToCreateNewUser() throws Exception {
        final String userEmail = "test@email.com";
        final String token = "test_token";
        when(tokenValidator.validateToken(token)).thenReturn(userEmail);

        final ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/create?token=" + token, null);

        assertEquals(response.getStatus(), CREATED.getStatusCode());
        final UserDescriptor user = (UserDescriptor)response.getEntity();
        assertEquals(user.getEmail(), userEmail);
    }

    @Test
    public void shouldBeAbleToGetCurrentUser() throws Exception {
        final User user = createUser();

        final ContainerResponse response = makeRequest("GET", SERVICE_PATH, null);

        assertEquals(response.getStatus(), OK.getStatusCode());
        final UserDescriptor descriptor = (UserDescriptor)response.getEntity();
        assertEquals(descriptor.getId(), user.getId());
        assertEquals(descriptor.getEmail(), user.getEmail());
        assertEquals(descriptor.getAliases(), user.getAliases());
    }

    @Test
    public void shouldBeAbleToGetUserById() throws Exception {
        final User user = createUser();

        final ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + user.getId(), null);

        assertEquals(response.getStatus(), OK.getStatusCode());
        final UserDescriptor descriptor = (UserDescriptor)response.getEntity();
        assertEquals(descriptor.getId(), user.getId());
        assertEquals(descriptor.getEmail(), user.getEmail());
        assertEquals(descriptor.getAliases(), user.getAliases());
    }

    @Test
    public void shouldBeAbleToGetUserByEmail() throws Exception {
        final User user = createUser();

        final ContainerResponse response = makeRequest("GET", SERVICE_PATH + "?email=" + user.getEmail(), null);

        assertEquals(response.getStatus(), OK.getStatusCode());
        final UserDescriptor descriptor = (UserDescriptor)response.getEntity();
        assertEquals(descriptor.getId(), user.getId());
        assertEquals(descriptor.getEmail(), user.getEmail());
        assertEquals(descriptor.getAliases(), user.getAliases());
    }

    @Test
    public void shouldBeAbleToUpdateUserPassword() throws Exception {
        final User user = createUser();
        final String newPassword = "validPass123";
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/x-www-form-urlencoded"));

        final ContainerResponse response = launcher.service("POST",
                                                            SERVICE_PATH + "/password",
                                                            BASE_URI,
                                                            headers,
                                                            ("password=" + newPassword).getBytes(),
                                                            null,
                                                            environmentContext);

        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        verify(userDao).update(user.withPassword(newPassword));
    }

    @Test
    public void shouldFailUpdatePasswordContainsOnlyLetters() throws Exception {
        final User user = createUser();
        final String newPassword = "password";
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/x-www-form-urlencoded"));

        final ContainerResponse response = launcher.service("POST",
                                                            SERVICE_PATH + "/password",
                                                            BASE_URI,
                                                            headers,
                                                            ("password=" + newPassword).getBytes(),
                                                            null,
                                                            environmentContext);

        assertEquals(response.getStatus(), CONFLICT.getStatusCode());
        verify(userDao, times(0)).update(user.withPassword(newPassword));
    }

    @Test
    public void shouldFailUpdatePasswordContainsOnlyDigits() throws Exception {
        final User user = createUser();
        final String newPassword = "12345678";
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/x-www-form-urlencoded"));

        final ContainerResponse response = launcher.service("POST",
                                                            SERVICE_PATH + "/password",
                                                            BASE_URI,
                                                            headers,
                                                            ("password=" + newPassword).getBytes(),
                                                            null,
                                                            environmentContext);

        assertEquals(response.getStatus(), CONFLICT.getStatusCode());
        verify(userDao, times(0)).update(user.withPassword(newPassword));
    }

    @Test
    public void shouldFailUpdatePasswordWhichLessEightChar() throws Exception {
        final User user = createUser();
        final String newPassword = "abc123";
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/x-www-form-urlencoded"));

        final ContainerResponse response = launcher.service("POST",
                                                            SERVICE_PATH + "/password",
                                                            BASE_URI,
                                                            headers,
                                                            ("password=" + newPassword).getBytes(),
                                                            null,
                                                            environmentContext);

        assertEquals(response.getStatus(), CONFLICT.getStatusCode());
        verify(userDao, times(0)).update(user.withPassword(newPassword));
    }

    @Test
    public void shouldBeAbleToRemoveUser() throws Exception {
        final User testUser = createUser();

        final ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + testUser.getId(), null);

        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        verify(userDao).remove(testUser.getId());
    }

    private User createUser() throws NotFoundException, ServerException {
        final User testUser = new User().withId("test_id")
                                        .withEmail("test@email");
        when(userDao.getById(testUser.getId())).thenReturn(testUser);
        when(userDao.getByAlias(testUser.getEmail())).thenReturn(testUser);
        return testUser;
    }

    private ContainerResponse makeRequest(String method, String path, Object entity) throws Exception {
        Map<String, List<String>> headers = null;
        byte[] data = null;
        if (entity != null) {
            headers = new HashMap<>();
            headers.put("Content-Type", singletonList("application/json"));
            data = JsonHelper.toJson(entity).getBytes();
        }
        return launcher.service(method, path, BASE_URI, headers, data, null, environmentContext);
    }
}
