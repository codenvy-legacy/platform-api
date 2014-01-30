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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.*;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.util.*;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * Tests cases for  {@link UserProfileService}.
 */
@Listeners(value = {MockitoTestNGListener.class})
public class UserProfileTest {

    protected final String BASE_URI     = "http://localhost/service";
    private final   String SERVICE_PATH = BASE_URI + "/profile";

    private final String PROFILE_ID = "profile123abc456def";

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
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldBeAbleToGETCurrentProfile() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class);

        when(userDao.getByAlias(anyString())).thenReturn(user);
        when(user.getProfileId()).thenReturn(PROFILE_ID);
        when(userProfileDao.getById(PROFILE_ID)).thenReturn(profile);
        prepareSecurityContext("user");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH, BASE_URI, null, JsonHelper.toJson(profile).getBytes(), null,
                                 environmentContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Profile responseProfile = (Profile)response.getEntity();
        verifyLinksRel(responseProfile.getLinks(), Constants.LINK_REL_GET_CURRENT_USER_PROFILE,
                       Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE);
    }


    @Test
    public void shouldBeAbleToGETProfileById() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID);

        when(userDao.getByAlias(anyString())).thenReturn(user);
        when(user.getProfileId()).thenReturn(PROFILE_ID);
        when(userProfileDao.getById(PROFILE_ID)).thenReturn(profile);

        prepareSecurityContext("system/admin");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "/" + PROFILE_ID, BASE_URI, null,
                                 JsonHelper.toJson(profile).getBytes(), null,
                                 environmentContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Profile responseProfile = (Profile)response.getEntity();
        verifyLinksRel(responseProfile.getLinks(), Constants.LINK_REL_GET_USER_PROFILE_BY_ID,
                       Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID);
    }


    @Test
    public void shouldBeAbleToUpdateCurrentProfile() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class);

        when(userDao.getByAlias(anyString())).thenReturn(user);
        prepareSecurityContext("user");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH, BASE_URI, headers, JsonHelper.toJson(profile).getBytes(), null,
                                 environmentContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(userProfileDao, times(1)).update(any(Profile.class));
        Profile responseProfile = (Profile)response.getEntity();
        verifyLinksRel(responseProfile.getLinks(), Constants.LINK_REL_GET_CURRENT_USER_PROFILE,
                       Constants.LINK_REL_UPDATE_CURRENT_USER_PROFILE);
    }

    @Test
    public void shouldBeAbleToUpdateProfileByID() throws Exception {
        // given
        Profile profile = DtoFactory.getInstance().createDto(Profile.class).withId(PROFILE_ID);
        prepareSecurityContext("system/admin");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/" + PROFILE_ID, BASE_URI, headers, JsonHelper.toJson(profile).getBytes(), null,
                                 environmentContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(userProfileDao, times(1)).update(any(Profile.class));
        Profile responseProfile = (Profile)response.getEntity();
        verifyLinksRel(responseProfile.getLinks(), Constants.LINK_REL_GET_USER_PROFILE_BY_ID,
                       Constants.LINK_REL_UPDATE_USER_PROFILE_BY_ID);
    }


    protected void verifyLinksRel(List<Link> links, String... rels) {
        assertEquals(links.size(), rels.length);
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

    protected void prepareSecurityContext(String role) {
        when(securityContext.isUserInRole(role)).thenReturn(true);
        when(securityContext.isUserInRole(not(Matchers.eq(role)))).thenReturn(false);
    }
}
