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

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.Profile;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
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
    private static final String ACCOUNT_ID     = "account0xffffffffffffff";
    private static final String PRINCIPAL_NAME = "Yoda@starwars.com";

    @Mock
    private WorkspaceDao workspaceDao;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

    @Mock
    AccountDao accountDao;

    @Mock
    private UserProfileDao userProfileDao;

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
        dependencies.addComponent(UserProfileDao.class, userProfileDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        resources.addResource(WorkspaceService.class, null);
        requestHandler = new RequestHandlerImpl(new RequestDispatcher(resources),
                                                providers, dependencies, new EverrestConfiguration());
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        launcher = new ResourceLauncher(requestHandler);
        workspace = DtoFactory.getInstance().createDto(Workspace.class)
                              .withId(WS_ID)
                              .withName(WS_NAME)
                              .withAccountId(ACCOUNT_ID);
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(PRINCIPAL_NAME));
    }

    @Test
    public void shouldBeAbleToCreateNewWorkspace() throws Exception {
        User current = DtoFactory.getInstance().createDto(User.class)
                                 .withId(USER_ID);
        Account acc = DtoFactory.getInstance().createDto(Account.class).withId(ACCOUNT_ID).withOwner(USER_ID);
        when(userDao.getByAlias(PRINCIPAL_NAME)).thenReturn(current);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(acc);

        String[] roles = getRoles(WorkspaceService.class, "create");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);
            assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
            Workspace created = (Workspace)response.getEntity();
            assertFalse(created.isTemporary());
            verifyLinksRel(created.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).create(any(Workspace.class));
        verify(memberDao, times(roles.length)).create(any(Member.class));
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWithExistedUser() throws Exception {
        User current = DtoFactory.getInstance().createDto(User.class).withId(USER_ID);
        when(userDao.getByAlias(PRINCIPAL_NAME)).thenReturn(current);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        Workspace created = (Workspace)response.getEntity();
        assertTrue(created.isTemporary());
        verify(userDao, times(0)).create(any(User.class));
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verify(memberDao, times(1)).create(any(Member.class));
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWhenUserDoesNotExist() throws Exception {
        when(securityContext.getUserPrincipal()).thenReturn(null);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        Workspace created = (Workspace)response.getEntity();
        assertTrue(created.isTemporary());
        verify(userDao, times(1)).create(any(User.class));
        verify(userProfileDao, times(1)).create(any(Profile.class));
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verify(memberDao, times(1)).create(any(Member.class));
    }

    @Test
    public void shouldBeAbleToGetWorkspaceById() throws Exception {
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);

        String[] roles = getRoles(WorkspaceService.class, "getById");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            verifyLinksRel(((Workspace)response.getEntity()).getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getById(WS_ID);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByName() throws Exception {
        when(workspaceDao.getByName(WS_NAME)).thenReturn(workspace);

        String[] roles = getRoles(WorkspaceService.class, "getByName");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "?name=" + WS_NAME, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            verifyLinksRel(((Workspace)response.getEntity()).getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getByName(WS_NAME);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspaceById() throws Exception {
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        Workspace workspaceToUpdate = DtoFactory.getInstance().createDto(Workspace.class)
                                                .withName("ws2");

        String[] roles = getRoles(WorkspaceService.class, "update");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID, MediaType.APPLICATION_JSON, workspaceToUpdate);
            Workspace result = (Workspace)response.getEntity();
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            verifyLinksRel(result.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).update(any(Workspace.class));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToGetWorkspacesOfCurrentUser() throws Exception {
        User current = DtoFactory.getInstance().createDto(User.class)
                                 .withId(USER_ID);
        when(userDao.getByAlias(PRINCIPAL_NAME)).thenReturn(current);
        when(memberDao.getUserRelationships(current.getId())).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                                 .withUserId(current.getId())
                                                                                                 .withWorkspaceId(workspace.getId())));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);

        String[] roles = getRoles(WorkspaceService.class, "getWorkspacesOfCurrentUser");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/all", null, null);
            List<Workspace> workspaces = (List<Workspace>)response.getEntity();
            assertEquals(workspaces.size(), 1);
            assertEquals(workspaces.get(0).getId(), WS_ID);
            verifyLinksRel(workspaces.get(0).getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getById(WS_ID);
        verify(memberDao, times(roles.length)).getUserRelationships(current.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToGetWorkspacesOfConcreteUser() throws Exception {
        User concrete = DtoFactory.getInstance().createDto(User.class)
                                  .withId(USER_ID);
        when(memberDao.getUserRelationships(concrete.getId())).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                                  .withUserId(concrete.getId())
                                                                                                  .withWorkspaceId(workspace.getId())));
        when(userDao.getById(USER_ID)).thenReturn(concrete);
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);

        String[] roles = getRoles(WorkspaceService.class, "getWorkspacesOfConcreteUser");
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/find?userid=" + USER_ID, null, null);
            List<Workspace> workspaces = (List<Workspace>)response.getEntity();
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            assertEquals(workspaces.size(), 1);
            assertEquals(workspaces.get(0).getId(), WS_ID);
            verifyLinksRel(workspaces.get(0).getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getById(workspace.getId());
        verify(memberDao, times(roles.length)).getUserRelationships(concrete.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                      .withWorkspaceId(WS_ID)
                                                                                      .withUserId(USER_ID)));
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID + "/members", null, null);
        List<Member> members = (List<Member>)response.getEntity();

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(members.size(), 1);
        verify(memberDao, times(1)).getWorkspaceMembers(WS_ID);
        verifyLinksRel(members.get(0).getLinks(),
                       Arrays.asList(Constants.LINK_REL_GET_WORKSPACE_MEMBERS, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
    }

    @Test
    public void shouldBeAbleToAddWorkspaceMember() throws Exception {
        Membership membership = DtoFactory.getInstance().createDto(Membership.class)
                                          .withRoles(Arrays.asList("workspace/developer"))
                                          .withUserId(USER_ID);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);
        Member member = (Member)response.getEntity();

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(member.getRoles(), membership.getRoles());
        assertEquals(member.getUserId(), USER_ID);
        assertEquals(member.getWorkspaceId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(member.getLinks(),
                       Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(memberDao, times(1)).remove(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspace() throws Exception {
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                      .withWorkspaceId(WS_ID)
                                                                                      .withUserId(USER_ID)));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(memberDao, times(1)).remove(any(Member.class));
        verify(workspaceDao, times(1)).remove(WS_ID);
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
        List<String> result = new ArrayList<>();
        result.add(Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES);
        result.add(com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS);
        switch (role) {
            case "workspace/admin":
                result.add(Constants.LINK_REL_ADD_WORKSPACE_MEMBER);
                result.add(Constants.LINK_REL_GET_WORKSPACE_MEMBERS);
            case "system/admin":
                result.add(Constants.LINK_REL_REMOVE_WORKSPACE);
                result.add(Constants.LINK_REL_UPDATE_WORKSPACE_BY_ID);
            case "system/manager":
            case "workspace/developer":
                result.add(Constants.LINK_REL_GET_WORKSPACE_BY_ID);
                result.add(Constants.LINK_REL_GET_WORKSPACE_BY_NAME);
        }
        return result;
    }

    protected void prepareSecurityContext(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        when(securityContext.isUserInRole("user")).thenReturn(true);
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
