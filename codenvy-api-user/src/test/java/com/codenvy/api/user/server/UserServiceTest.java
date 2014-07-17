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

import sun.security.acl.PrincipalImpl;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.NameGenerator;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * Tests for User Service
 *
 * @author Eugene Veovodin
 * @author Max Shaposhnik
 * @see com.codenvy.api.user.server.UserService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class UserServiceTest {

    protected final String BASE_URI     = "http://localhost/service";
    private final   String SERVICE_PATH = BASE_URI + "/user";

    private final String USER_ID    = "user123abc456def";
    private final String USER_EMAIL = "user@text.com";

    @Mock
    private UserProfileDao userProfileDao;

    @Mock
    private UserDao userDao;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private EnvironmentContext environmentContext;

    @Mock
    private SecurityContext securityContext;

    protected ProviderBinder     providers;
    protected ResourceBinderImpl resources;
    protected RequestHandlerImpl requestHandler;
    protected ResourceLauncher   launcher;

    @BeforeMethod
    public void setUp() throws Exception {
        resources = new ResourceBinderImpl();
        providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserProfileDao.class, userProfileDao);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(TokenValidator.class, tokenValidator);
        resources.addResource(UserService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
        when(userDao.getById(USER_ID))
                .thenReturn(DtoFactory.getInstance().createDto(User.class).withId(USER_ID));
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }


    @Test
    public void shouldBeAbleToCreateNewUser() throws Exception {
        String[] s = getRoles(UserService.class, "create");
        for (String role : s) {

            prepareSecurityContext(role);
            ContainerResponse response =
                    launcher.service("POST", SERVICE_PATH + "/create", BASE_URI, null, null, null,
                                     environmentContext);

            assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
            verifyLinksRel(((User)response.getEntity()).getLinks(), getRels(role));
        }
        verify(userDao, times(s.length)).create(any(User.class));
        verify(userProfileDao, times(s.length)).create(any(Profile.class));
    }


    @Test
    public void shouldBeAbleToGetCurrentUser() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);

        String[] s = getRoles(UserService.class, "getCurrent");
        for (String role : s) {
            prepareSecurityContext(role);
            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH, BASE_URI, null, null, null, environmentContext);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            verifyLinksRel(((User)response.getEntity()).getLinks(), getRels(role));
        }
        verify(userDao, times(s.length)).getByAlias(USER_EMAIL);
    }

    @Test
    public void shouldBeAbleToGetUserByID() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);
        when(userDao.getById(USER_ID)).thenReturn(user);
        String[] s = getRoles(UserService.class, "getById");
        for (String role : s) {
            prepareSecurityContext(role);

            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH + "/" + USER_ID, BASE_URI, null, null, null,
                                     environmentContext);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            verifyLinksRel(((User)response.getEntity()).getLinks(), getRels(role));
        }
        verify(userDao, times(s.length)).getById(USER_ID);
    }

    @Test
    public void shouldBeAbleToGetUserByEmail() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        when(userDao.getById(USER_ID)).thenReturn(user);
        String[] s = getRoles(UserService.class, "getByEmail");
        for (String role : s) {
            prepareSecurityContext(role);

            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH + "?email=" + USER_EMAIL, BASE_URI, null, null, null,
                                     environmentContext);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            verifyLinksRel(((User)response.getEntity()).getLinks(), getRels(role));
        }
        verify(userDao, times(s.length)).getByAlias(USER_EMAIL);
    }

    @Test
    public void shouldBeAbleToUpdateUserPassword() throws Exception {
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL);
        String newPassword = NameGenerator.generate(User.class.getSimpleName(), Constants.ID_LENGTH);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/x-www-form-urlencoded"));
        prepareSecurityContext("user");
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);

        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/password", BASE_URI, headers, ("password=" + newPassword).getBytes(), null,
                                 environmentContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(userDao, times(1)).getByAlias(USER_EMAIL);
        verify(userDao, times(1)).update(any(User.class));
    }

    @Test
    public void shouldBeAbleToRemoveUser() throws Exception {
        prepareSecurityContext("system/admin");

        ContainerResponse response =
                launcher.service("DELETE", SERVICE_PATH + "/" + USER_ID, BASE_URI, null, null, null, environmentContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(userDao, times(1)).remove(USER_ID);
    }


    protected void verifyLinksRel(List<Link> links, List<String> rels) {
        assertEquals(links.size(), rels.size());
        for (String rel : rels) {
            boolean linkPresent = false;
            for (Link link : links) {
                linkPresent |= link.getRel().equals(rel);
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
        throw new IllegalArgumentException(
                String.format("Class %s does not have method with name %s", clazz.getName(), methodName));
    }

    private List<String> getRels(String role) {
        List<String> result = new ArrayList<>();
        result.add(Constants.LINK_REL_GET_CURRENT_USER);
        result.add(Constants.LINK_REL_UPDATE_PASSWORD);
        result.add(Constants.LINK_REL_GET_CURRENT_USER_PROFILE);
        switch (role) {
            case "system/admin":
                result.add(Constants.LINK_REL_REMOVE_USER_BY_ID);
            case "system/manager":
                result.add(Constants.LINK_REL_GET_USER_BY_ID);
                result.add(Constants.LINK_REL_GET_USER_BY_EMAIL);
                result.add(Constants.LINK_REL_GET_USER_PROFILE_BY_ID);
                break;

            default:
                break;
        }
        return result;
    }

    protected void prepareSecurityContext(String... roles) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        when(securityContext.isUserInRole("user")).thenReturn(true);
        for (String role : roles)
            when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
