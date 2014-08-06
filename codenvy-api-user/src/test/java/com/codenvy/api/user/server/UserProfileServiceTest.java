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
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.ProfileDescriptor;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Tests for {@link UserProfileService}
 *
 * @author Max Shaposhnik
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class UserProfileServiceTest {

    private static final String BASE_URI     = "http://localhost/service";
    private static final String SERVICE_PATH = BASE_URI + "/profile";
    private static final String USER_ID      = "user123abc456def";

    @Mock
    private UserProfileDao     userProfileDao;
    @Mock
    private UserDao            userDao;
    @Mock
    private User               user;
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
        ProviderBinder providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(UserProfileDao.class, userProfileDao);
        dependencies.addComponent(UserDao.class, userDao);
        resources.addResource(UserProfileService.class, null);
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl("user@testuser.com"));
        when(userDao.getByAlias(anyString())).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldBeAbleToGETCurrentProfile() throws Exception {
        // given
        when(userProfileDao.getById(USER_ID)).thenReturn(new Profile());

        String[] s = getRoles(UserProfileService.class, "getCurrent");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response = launcher.service("GET",
                                                          SERVICE_PATH,
                                                          BASE_URI,
                                                          null,
                                                          JsonHelper.toJson(DtoFactory.getInstance().createDto(ProfileDescriptor.class))
                                                                    .getBytes(),
                                                          null,
                                                          environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            ProfileDescriptor responseProfile = (ProfileDescriptor)response.getEntity();
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }

    @Test
    public void shouldBeAbleToGetCurrentProfileWithFilter() throws Exception {
        Map<String, String> prefs = new HashMap<>(4);
        prefs.put("first", "first_value");
        prefs.put("____firstASD", "other_first_value");
        prefs.put("second", "second_value");
        prefs.put("other", "other_value");
        when(userProfileDao.getById(USER_ID, "first")).thenReturn(new Profile().withPreferences(prefs));
        prepareSecurityContext("user");

        ContainerResponse response = launcher.service("GET",
                                                      SERVICE_PATH + "?filter=first",
                                                      BASE_URI,
                                                      null,
                                                      null,
                                                      null,
                                                      environmentContext);

        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        ProfileDescriptor current = (ProfileDescriptor)response.getEntity();
        verifyLinksRel(current.getLinks(), getRels("user"));
    }

    @Test
    public void shouldBeAbleToRemovePreferences() throws Exception {
        Map<String, String> prefs = new HashMap<>(Collections.singletonMap("ssh_key", "value"));
        Profile profile = new Profile().withId(USER_ID)
                                       .withPreferences(prefs);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);
        Map<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response = launcher.service("DELETE",
                                                      SERVICE_PATH + "/prefs",
                                                      BASE_URI,
                                                      headers,
                                                      JsonHelper.toJson(Arrays.asList("ssh_key")).getBytes(),
                                                      null,
                                                      environmentContext);

        assertEquals(response.getStatus(), Status.NO_CONTENT.getStatusCode());
        verify(userProfileDao, times(1)).update(any(Profile.class));
        assertEquals(prefs.size(), 0);
    }

    @Test
    public void shouldBeAbleToRemoveAttributes() throws Exception {
        Map<String, String> attributes = new HashMap<>(3);
        attributes.put("test", "test");
        attributes.put("test1", "test");
        attributes.put("test2", "test");
        Profile profile = new Profile().withId(USER_ID)
                                       .withAttributes(attributes);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);
        Map<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response = launcher.service("DELETE",
                                                      SERVICE_PATH + "/attributes",
                                                      BASE_URI,
                                                      headers,
                                                      JsonHelper.toJson(Arrays.asList("test", "test2")).getBytes(),
                                                      null,
                                                      environmentContext);

        assertEquals(response.getStatus(), Status.NO_CONTENT.getStatusCode());
        verify(userProfileDao, times(1)).update(any(Profile.class));
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get("test1"), "test");
    }

    @Test
    public void shouldBeAbleToUpdateCurrentProfilePrefs() throws Exception {
        // given
        when(userProfileDao.getById(USER_ID)).thenReturn(new Profile().withUserId(USER_ID));

        Map<String, List<String>> headers = new HashMap<>();

        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, String> prefsToUpdate = new HashMap<>();
        prefsToUpdate.put("second", "second_value");
        prefsToUpdate.put("other", "other_value");
        String[] s = getRoles(UserProfileService.class, "updatePreferences");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response = launcher.service("POST",
                                                          SERVICE_PATH + "/prefs",
                                                          BASE_URI,
                                                          headers,
                                                          JsonHelper.toJson(prefsToUpdate).getBytes(),
                                                          null,
                                                          environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            ProfileDescriptor responseProfile = (ProfileDescriptor)response.getEntity();
            assertEquals(responseProfile.getPreferences(), prefsToUpdate);
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
        verify(userProfileDao, times(s.length)).update(any(Profile.class));
    }

    @Test
    public void shouldBeAbleToGETProfileById() throws Exception {
        ProfileDescriptor profile = DtoFactory.getInstance().createDto(ProfileDescriptor.class)
                                              .withId(USER_ID)
                                              .withUserId(USER_ID);
        when(userProfileDao.getById(USER_ID)).thenReturn(new Profile().withId(USER_ID)
                                                                      .withUserId(USER_ID));
        when(userDao.getById(USER_ID)).thenReturn(user);

        String[] s = getRoles(UserProfileService.class, "getById");
        for (String one : s) {
            prepareSecurityContext(one);

            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH + "/" + USER_ID, BASE_URI, null,
                                     JsonHelper.toJson(profile).getBytes(), null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            ProfileDescriptor responseProfile = (ProfileDescriptor)response.getEntity();
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }


    @Test
    public void shouldBeAbleToUpdateCurrentProfile() throws Exception {
        // given
        when(userProfileDao.getById(USER_ID)).thenReturn(new Profile().withId(USER_ID));
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("test", "test");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        String[] s = getRoles(UserProfileService.class, "updateCurrent");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response = launcher.service("POST",
                                                          SERVICE_PATH,
                                                          BASE_URI,
                                                          headers,
                                                          JsonHelper.toJson(attributes).getBytes(),
                                                          null,
                                                          environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            verify(userProfileDao, times(1)).update(any(Profile.class));
            ProfileDescriptor responseProfile = (ProfileDescriptor)response.getEntity();
            assertEquals(responseProfile.getAttributes(), attributes);
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }

    @Test
    public void shouldBeAbleToUpdateProfileByID() throws Exception {
        // given
        when(userProfileDao.getById(USER_ID)).thenReturn(new Profile().withId(USER_ID));
        when(userDao.getById(anyString())).thenReturn(user);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("test", "test");

        String[] s = getRoles(UserProfileService.class, "update");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response =
                    launcher.service("POST", SERVICE_PATH + "/" + USER_ID, BASE_URI, headers,
                                     JsonHelper.toJson(attributes).getBytes(), null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            ProfileDescriptor responseProfile = (ProfileDescriptor)response.getEntity();
            assertEquals(responseProfile.getAttributes(), attributes);
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
        verify(userProfileDao, times(s.length)).update(any(Profile.class));
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
        result.add(Constants.LINK_REL_GET_CURRENT_USER_PROFILE);
        result.add(Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE);
        result.add(Constants.LINK_REL_UPDATE_PREFERENCES);
        switch (role) {
            case "system/admin":
            case "system/manager":
                result.add(Constants.LINK_REL_GET_USER_PROFILE_BY_ID);
                result.add(Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID);
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
