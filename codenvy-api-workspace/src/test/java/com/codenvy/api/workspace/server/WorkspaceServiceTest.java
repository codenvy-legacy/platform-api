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
package com.codenvy.api.workspace.server;

import sun.security.acl.PrincipalImpl;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.MemberException;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.commons.json.JsonHelper;
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
import org.everrest.core.tools.ByteArrayContainerResponseWriter;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.net.ResponseCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Test for Workspace service
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    private static final String BASE_URI      = "http://localhost/service";
    private static final String SERVICE_PATH  = BASE_URI + "/workspace";
    private static final String WS_ID         = "workspace12asd123asdasd1f";
    private static final String WS_NAME       = "ws1";
    private static final String PRICIPAL_NAME = "Yoda";

    @Mock
    private WorkspaceDao workspaceDao;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private EnvironmentContext environmentContext;

    private Workspace workspace;

    protected ProviderBinder providers;

    protected ResourceBinderImpl resources;

    protected RequestHandlerImpl requestHandler;

    protected ResourceLauncher launcher;


    @BeforeMethod
    public void setUp() throws Exception {
        resources = new ResourceBinderImpl();
        providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(WorkspaceDao.class, workspaceDao);
        dependencies.addComponent(MemberDao.class, memberDao);
        dependencies.addComponent(UserDao.class, userDao);
        resources.addResource(WorkspaceService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);
        workspace = DtoFactory.getInstance().createDto(Workspace.class)
                              .withId(WS_ID)
                              .withName(WS_NAME)
                              .withAttributes(Collections.EMPTY_LIST);
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(PRICIPAL_NAME));

    }

    @Test
    public void shouldBeAbleToCreateNewWorkspace() throws Exception {
        //given
        prepareSecurityContext("user");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        //when, then
        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH, BASE_URI, headers, JsonHelper.toJson(workspace).getBytes(), null,
                                 environmentContext);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verifyLinksRel(((Workspace)response.getEntity()).getLinks(), Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES,
                       com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceById() throws Exception {
        //given
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("system/admin");
        //when, then
        ContainerResponse response = launcher.service("GET", SERVICE_PATH + "/" + WS_ID, BASE_URI, null, null, null, environmentContext);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(workspaceDao, times(1)).getById(WS_ID);
        verifyLinksRel(((Workspace)response.getEntity()).getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                       Constants.LINK_REL_GET_WORKSPACE_BY_NAME, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID,
                       Constants.LINK_REL_REMOVE_WORKSPACE);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByName() throws Exception {
        //given
        when(workspaceDao.getByName(WS_NAME)).thenReturn(workspace);
        prepareSecurityContext("system/manager");
        //when, then
        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "?name=" + WS_NAME, BASE_URI, null, null, null, environmentContext);
        verify(workspaceDao, times(1)).getByName(WS_NAME);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verifyLinksRel(((Workspace)response.getEntity()).getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                       Constants.LINK_REL_GET_WORKSPACE_BY_ID);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspaceById() throws Exception {
        //given
        Workspace workspaceToUpdate = DtoFactory.getInstance().createDto(Workspace.class)
                                                .withName("ws2")
                                                .withAttributes(Collections.EMPTY_LIST);
        prepareSecurityContext("workspace/admin");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        //when
        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/" + WS_ID, BASE_URI, headers, JsonHelper.toJson(workspaceToUpdate).getBytes(),
                                 null, environmentContext);
        Workspace result = (Workspace)response.getEntity();
        //then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(workspaceDao, times(1)).update(any(Workspace.class));
        assertEquals(WS_ID, result.getId());
        verifyLinksRel(result.getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_NAME, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID,
                       Constants.LINK_REL_ADD_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE,
                       Constants.LINK_REL_GET_WORKSPACE_MEMBERS, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID);
    }

    @Test
    public void shouldBeAbleToGetWorkspacesOfCurrentUser() throws Exception {
        //given
        User user = DtoFactory.getInstance().createDto(User.class)
                              .withId("user1234567891011121");
        when(userDao.getByAlias(PRICIPAL_NAME)).thenReturn(user);
        when(memberDao.getUserRelationships(user.getId())).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                              .withUserId(user.getId())
                                                                                              .withWorkspaceId(workspace.getId())));
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("user");
        //when
        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "/all", BASE_URI, null, null, null, environmentContext);
        List<Workspace> workspaces = (List<Workspace>)response.getEntity();
        //then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(workspaces.size(), 1);
        verify(workspaceDao, times(1)).getById(WS_ID);
        verifyLinksRel(workspaces.get(0).getLinks(), com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS,
                       Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES);
    }

    //TODO add test

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
