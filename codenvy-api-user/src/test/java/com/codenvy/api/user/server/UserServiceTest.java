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
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.lang.NameGenerator;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
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
    private final String PROFILE_ID = "profile123abc456def";

    @Mock
    private UserProfileDao userProfileDao;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

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
        dependencies.addComponent(MemberDao.class, memberDao);
        resources.addResource(UserService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);

        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(USER_EMAIL));
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }


    @Test
    public void shouldBeAbleToCreateNewUser() throws Exception {
        prepareSecurityContext("user");
        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/create", BASE_URI, null, null, null,
                                 environmentContext);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(userDao, times(1)).create(any(User.class));
        verify(userProfileDao, times(1)).create(any(Profile.class));
        verifyLinksRel(((User)response.getEntity()).getLinks(), Constants.LINK_REL_GET_CURRENT_USER,
                       Constants.LINK_REL_UPDATE_PASSWORD);
    }


    @Test
    public void shouldBeAbleToGetCurrentUser() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL).withProfileId(PROFILE_ID);

        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        prepareSecurityContext("user");

        ContainerResponse response = launcher.service("GET", SERVICE_PATH, BASE_URI, null, null, null, environmentContext);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(userDao, times(1)).getByAlias(USER_EMAIL);
        verifyLinksRel(((User)response.getEntity()).getLinks(), Constants.LINK_REL_GET_CURRENT_USER,
                       Constants.LINK_REL_UPDATE_PASSWORD);
    }

    @Test
    public void shouldBeAbleToGetCurrentUserByID() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL).withProfileId(PROFILE_ID);

        when(userDao.getById(USER_ID)).thenReturn(user);
        prepareSecurityContext("system/admin");

        ContainerResponse response = launcher.service("GET", SERVICE_PATH + "/" + USER_ID, BASE_URI, null, null, null, environmentContext);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(userDao, times(1)).getById(USER_ID);
        verifyLinksRel(((User)response.getEntity()).getLinks(), Constants.LINK_REL_GET_USER_BY_ID,
                       Constants.LINK_REL_GET_USER_BY_EMAIL, Constants.LINK_REL_REMOVE_USER_BY_ID);
    }

    @Test
    public void shouldBeAbleToGetUserByEmail() throws Exception {

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL).withProfileId(PROFILE_ID);

        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);
        prepareSecurityContext("system/admin");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "?email=" + USER_EMAIL, BASE_URI, null, null, null, environmentContext);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(userDao, times(1)).getByAlias(USER_EMAIL);
        verifyLinksRel(((User)response.getEntity()).getLinks(), Constants.LINK_REL_GET_USER_BY_ID,
                       Constants.LINK_REL_GET_USER_BY_EMAIL, Constants.LINK_REL_REMOVE_USER_BY_ID);
    }

    @Test
    public void shouldBeAbleToUpdateUserPassword() throws Exception {
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID).withEmail(USER_EMAIL).withProfileId(PROFILE_ID);
        String newPassword = NameGenerator.generate(User.class.getSimpleName(), Constants.ID_LENGTH);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        prepareSecurityContext("user");
        when(userDao.getByAlias(USER_EMAIL)).thenReturn(user);

        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/password", BASE_URI, headers, JsonHelper.toJson(newPassword).getBytes(), null,
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
        verify(userDao, times(1)).removeById(USER_ID);
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
