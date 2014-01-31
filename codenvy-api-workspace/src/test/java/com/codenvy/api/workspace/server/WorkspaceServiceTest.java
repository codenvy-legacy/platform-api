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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Membership;
import com.codenvy.api.workspace.shared.dto.Workspace;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.util.Arrays;
import java.util.Collections;
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
 * Tests for Workspace service
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.workspace.server.WorkspaceService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    private static final String BASE_URI       = "http://localhost/service";
    private static final String SERVICE_PATH   = BASE_URI + "/workspace";
    private static final String WS_ID          = "workspace0xffffffffffffff";
    private static final String WS_NAME        = "ws1";
    private static final String USER_ID        = "user0xffffffffffffff";
    private static final String PRINCIPAL_NAME = "Yoda@starwars.com";

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

    protected Workspace workspace;

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
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(PRINCIPAL_NAME));

    }

    @Test
    public void shouldBeAbleToCreateNewWorkspace() throws Exception {
        prepareSecurityContext("user");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

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
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("system/admin");

        ContainerResponse response = launcher.service("GET", SERVICE_PATH + "/" + WS_ID, BASE_URI, null, null, null, environmentContext);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(workspaceDao, times(1)).getById(WS_ID);
        verifyLinksRel(((Workspace)response.getEntity()).getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                       Constants.LINK_REL_GET_WORKSPACE_BY_NAME, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID,
                       Constants.LINK_REL_REMOVE_WORKSPACE);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByName() throws Exception {
        when(workspaceDao.getByName(WS_NAME)).thenReturn(workspace);
        prepareSecurityContext("system/manager");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "?name=" + WS_NAME, BASE_URI, null, null, null, environmentContext);

        verify(workspaceDao, times(1)).getByName(WS_NAME);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verifyLinksRel(((Workspace)response.getEntity()).getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                       Constants.LINK_REL_GET_WORKSPACE_BY_ID);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspaceById() throws Exception {
        Workspace workspaceToUpdate = DtoFactory.getInstance().createDto(Workspace.class)
                                                .withName("ws2")
                                                .withAttributes(Collections.EMPTY_LIST);
        prepareSecurityContext("workspace/admin");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/" + WS_ID, BASE_URI, headers, JsonHelper.toJson(workspaceToUpdate).getBytes(),
                                 null, environmentContext);
        Workspace result = (Workspace)response.getEntity();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(workspaceDao, times(1)).update(any(Workspace.class));
        assertEquals(WS_ID, result.getId());
        verifyLinksRel(result.getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_NAME, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID,
                       Constants.LINK_REL_ADD_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE,
                       Constants.LINK_REL_GET_WORKSPACE_MEMBERS, Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID);
    }

    @Test
    public void shouldBeAbleToGetWorkspacesOfCurrentUser() throws Exception {
        User current = DtoFactory.getInstance().createDto(User.class)
                                 .withId(USER_ID);
        when(userDao.getByAlias(PRINCIPAL_NAME)).thenReturn(current);
        when(memberDao.getUserRelationships(current.getId())).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                                 .withUserId(current.getId())
                                                                                                 .withWorkspaceId(workspace.getId())));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("user");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "/all", BASE_URI, null, null, null, environmentContext);
        List<Workspace> workspaces = (List<Workspace>)response.getEntity();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(workspaces.size(), 1);
        verify(workspaceDao, times(1)).getById(WS_ID);
        verify(memberDao, times(1)).getUserRelationships(current.getId());
        verifyLinksRel(workspaces.get(0).getLinks(), com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS,
                       Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES);
    }

    @Test
    public void shouldBeAbleToGetWorkspacesOfConcreteUser() throws Exception {
        User concrete = DtoFactory.getInstance().createDto(User.class)
                                  .withId(USER_ID);
        when(memberDao.getUserRelationships(concrete.getId())).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                                  .withUserId(concrete.getId())
                                                                                                  .withWorkspaceId(workspace.getId())));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("system/manager");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "/find?userid=" + USER_ID, BASE_URI, null, null, null, environmentContext);
        List<Workspace> workspaces = (List<Workspace>)response.getEntity();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(workspaces.size(), 1);
        verify(workspaceDao, times(1)).getById(workspace.getId());
        verify(memberDao, times(1)).getUserRelationships(concrete.getId());
        verifyLinksRel(workspaces.get(0).getLinks(), Constants.LINK_REL_GET_WORKSPACE_BY_ID, Constants.LINK_REL_GET_WORKSPACE_BY_NAME);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                      .withWorkspaceId(WS_ID)
                                                                                      .withUserId(USER_ID)));

        prepareSecurityContext("workspace/admin");

        ContainerResponse response =
                launcher.service("GET", SERVICE_PATH + "/" + WS_ID + "/members", BASE_URI, null, null, null, environmentContext);
        List<Member> members = (List<Member>)response.getEntity();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(members.size(), 1);
        verify(memberDao, times(1)).getWorkspaceMembers(WS_ID);
        verifyLinksRel(members.get(0).getLinks(), Constants.LINK_REL_GET_WORKSPACE_MEMBERS, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER);
    }

    @Test
    public void shouldBeAbleToAddWorkspaceMember() throws Exception {
        Membership membership = DtoFactory.getInstance().createDto(Membership.class)
                                          .withRoles(Arrays.asList("workspace/developer"))
                                          .withUserId(USER_ID);
        prepareSecurityContext("workspace/admin");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ContainerResponse response =
                launcher.service("POST", SERVICE_PATH + "/" + WS_ID + "/members", BASE_URI, headers,
                                 JsonHelper.toJson(membership).getBytes(), null, environmentContext);
        Member member = (Member)response.getEntity();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(member.getRoles(), membership.getRoles());
        assertEquals(member.getUserId(), USER_ID);
        assertEquals(member.getWorkspaceId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(member.getLinks(), Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER);
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        ContainerResponse response =
                launcher.service("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, BASE_URI, null, null, null,
                                 environmentContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(memberDao, times(1)).remove(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspace() throws Exception {
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                      .withWorkspaceId(WS_ID)
                                                                                      .withUserId(USER_ID)));

        ContainerResponse response =
                launcher.service("DELETE", SERVICE_PATH + "/" + WS_ID, BASE_URI, null, null, null, environmentContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(memberDao, times(1)).remove(any(Member.class));
        verify(workspaceDao, times(1)).remove(WS_ID);
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
