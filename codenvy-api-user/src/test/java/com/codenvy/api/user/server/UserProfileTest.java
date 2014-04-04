/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.api.user.server;

import sun.security.acl.PrincipalImpl;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.*;
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
import java.util.*;

import static javax.ws.rs.core.Response.Status;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/** Tests cases for  {@link UserProfileService}. */
@Listeners(value = {MockitoTestNGListener.class})
public class UserProfileTest {

    protected final String BASE_URI     = "http://localhost/service";
    private final   String SERVICE_PATH = BASE_URI + "/profile";

    private final String USER_ID = "user123abc456def";

    @Mock
    private UserProfileDao userProfileDao;

    @Mock
    private UserDao userDao;

    @Mock
    private User user;

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
        resources.addResource(UserProfileService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);

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
        Profile profile = DtoFactory.getInstance().createDto(Profile.class);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);

        String[] s = getRoles(UserProfileService.class, "getCurrent");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH, BASE_URI, null, JsonHelper.toJson(profile).getBytes(), null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            Profile responseProfile = (Profile)response.getEntity();
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }

    @Test
    public void shouldBeAbleToGetCurrentProfileWithFilter() throws Exception {
        Map<String, String> prefs = new HashMap<>();
        prefs.put("first", "first_value");
        prefs.put("____firstASD", "other_first_value");
        prefs.put("second", "second_value");
        prefs.put("other", "other_value");
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withPreferences(prefs);
        when(userProfileDao.getById(USER_ID, "first")).thenReturn(profile);
        prepareSecurityContext("user");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "?filter=first", BASE_URI, null, JsonHelper.toJson(profile).getBytes(), null,
                                 environmentContext);

        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        Profile current = (Profile)response.getEntity();
        verifyLinksRel(current.getLinks(), getRels("user"));
    }

    @Test
    public void shouldBeAbleToUpdateCurrentProfilePrefs() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(USER_ID);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);

        Map<String, List<String>> headers = new HashMap<>();

        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, String> prefsToUpdate = new HashMap<>();
        prefsToUpdate.put("second", "second_value");
        prefsToUpdate.put("other", "other_value");
        String[] s = getRoles(UserProfileService.class, "updatePrefs");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response =
                    launcher.service("POST", SERVICE_PATH + "/prefs", BASE_URI, headers, JsonHelper.toJson(prefsToUpdate).getBytes(),
                                     null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            verify(userProfileDao, times(1)).update(any(Profile.class));
            Profile responseProfile = (Profile)response.getEntity();
            assertEquals(responseProfile.getPreferences(), prefsToUpdate);
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }

    @Test
    public void shouldBeAbleToGETProfileById() throws Exception {
        Profile profile = DtoFactory.getInstance().createDto(Profile.class)
                                    .withId(USER_ID)
                                    .withUserId(USER_ID);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);
        when(userDao.getById(USER_ID)).thenReturn(user);

        String[] s = getRoles(UserProfileService.class, "getById");
        for (String one : s) {
            prepareSecurityContext(one);

            ContainerResponse response =
                    launcher.service("GET", SERVICE_PATH + "/" + USER_ID, BASE_URI, null,
                                     JsonHelper.toJson(profile).getBytes(), null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            Profile responseProfile = (Profile)response.getEntity();
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }


    @Test
    public void shouldBeAbleToUpdateCurrentProfile() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(USER_ID);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        List<Attribute> attributeList = Arrays.asList(
                DtoFactory.getInstance().createDto(Attribute.class).withName("testname").withValue("testValue")
                          .withDescription("testDescription"));
        String[] s = getRoles(UserProfileService.class, "updateCurrent");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response =
                    launcher.service("POST", SERVICE_PATH, BASE_URI, headers, JsonHelper.toJson(attributeList).getBytes(),
                                     null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            verify(userProfileDao, times(1)).update(any(Profile.class));
            Profile responseProfile = (Profile)response.getEntity();
            //assertEquals(responseProfile.getAttributes(), attributeList);
            verifyLinksRel(responseProfile.getLinks(), getRels(one));
        }
    }

    @Test
    public void shouldBeAbleToUpdateProfileByID() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(USER_ID);
        when(userProfileDao.getById(USER_ID)).thenReturn(profile);
        when(userDao.getById(anyString())).thenReturn(user);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        List<Attribute> attributeList = Arrays.asList(
                DtoFactory.getInstance().createDto(Attribute.class).withName("testname").withValue("testValue")
                          .withDescription("testDescription"));

        String[] s = getRoles(UserProfileService.class, "update");
        for (String one : s) {
            prepareSecurityContext(one);
            ContainerResponse response =
                    launcher.service("POST", SERVICE_PATH + "/" + USER_ID, BASE_URI, headers,
                                     JsonHelper.toJson(attributeList).getBytes(), null,
                                     environmentContext);

            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            Profile responseProfile = (Profile)response.getEntity();
            //assertEquals(responseProfile.getAttributes(), attributeList);
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
        result.add(Constants.LINK_REL_UPDATE_PREFS);
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
