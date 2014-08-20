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
package com.codenvy.api.workspace.server;

import sun.security.acl.PrincipalImpl;

import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.MemberDescriptor;
import com.codenvy.api.workspace.shared.dto.NewMembership;
import com.codenvy.api.workspace.shared.dto.NewWorkspace;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.api.workspace.shared.dto.WorkspaceUpdate;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.user.UserImpl;
import com.codenvy.dto.server.DtoFactory;

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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * Tests for {@link com.codenvy.api.workspace.server.WorkspaceService}
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
    private WorkspaceDao       workspaceDao;
    @Mock
    private UserDao            userDao;
    @Mock
    private MemberDao          memberDao;
    @Mock
    private AccountDao         accountDao;
    @Mock
    private UserProfileDao     userProfileDao;
    @Mock
    private SecurityContext    securityContext;
    @Mock
    private EnvironmentContext environmentContext;
    private Workspace          workspace;
    private ResourceLauncher   launcher;
    private String             isOrgAddonEnabledByDefault;

    @BeforeMethod
    public void setUp() throws Exception {
        isOrgAddonEnabledByDefault = "false";
        final ResourceBinderImpl resources = new ResourceBinderImpl();
        final ProviderBinder providers = new ApplicationProviderBinder();
        DependencySupplierImpl dependencies = new DependencySupplierImpl() {


            @Override
            public Object getComponentByName(String name) {
                if ("subscription.orgaddon.enabled".equals(name)) {
                    return isOrgAddonEnabledByDefault;
                }
                return super.getComponentByName(name);
            }
        };
        dependencies.addComponent(WorkspaceDao.class, workspaceDao);
        dependencies.addComponent(MemberDao.class, memberDao);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(UserProfileDao.class, userProfileDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        resources.addResource(WorkspaceService.class, null);
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);
        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
        //count of attributes should be > 0
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("default_attribute", "default_value");
        workspace = new Workspace().withId(WS_ID)
                                   .withName(WS_NAME)
                                   .withTemporary(false)
                                   .withAccountId(ACCOUNT_ID)
                                   .withAttributes(attributes);
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(PRINCIPAL_NAME));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        when(workspaceDao.getByName(WS_NAME)).thenReturn(workspace);
        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID);
        when(userDao.getById(USER_ID)).thenReturn(user);
        when(userDao.getByAlias(PRINCIPAL_NAME)).thenReturn(user);
        com.codenvy.commons.env.EnvironmentContext.getCurrent().setUser(new UserImpl(PRINCIPAL_NAME, USER_ID, null));
    }

    @Test
    public void shouldBeAbleToGetWorkspaceById() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin", "workspace/developer"))));
        String[] roles = new String[]{"workspace/admin", "workspace/developer", "system/admin", "system/manager"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            WorkspaceDescriptor ws = (WorkspaceDescriptor)response.getEntity();
            assertEquals(ws.getId(), workspace.getId());
            assertEquals(ws.getName(), workspace.getName());
            assertEquals(ws.isTemporary(), workspace.isTemporary());
            assertEquals(ws.getAccountId(), workspace.getAccountId());
            assertEquals(ws.getAttributes(), workspace.getAttributes());
            verifyLinksRel(ws.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getById(WS_ID);
    }

    @Test
    public void shouldBeAbleToCreateNewWorkspace() throws Exception {
        Account acc = new Account().withId(ACCOUNT_ID);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(acc);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(acc));

        String[] roles = new String[]{"user", "system/admin"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);
            assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
            WorkspaceDescriptor created = (WorkspaceDescriptor)response.getEntity();
            assertFalse(created.isTemporary());
            verifyLinksRel(created.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).create(any(Workspace.class));
        verify(memberDao, times(roles.length)).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToCreateNewWorkspaceWithNotValidAttribute() throws Exception {
        workspace.getAttributes().put("codenvy:god_mode", "true");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspaces() throws Exception {
        when(workspaceDao.getByAccount("test")).thenReturn(Collections.singletonList(new Workspace().withId("test1")
                                                                                                    .withName("test1")
                                                                                                    .withTemporary(false)
                                                                                                    .withAccountId("test1")));
        final Account test = new Account().withId("test")
                                          .withName("test")
                                          .withAttributes(Collections.singletonMap("codenvy:multi-ws", "true"));
        when(accountDao.getById(test.getId())).thenReturn(test);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.singletonList(test));
        prepareSecurityContext("user");

        final NewWorkspace newWorkspace = DtoFactory.getInstance().createDto(NewWorkspace.class)
                                                    .withName("test2")
                                                    .withAccountId("test");
        ContainerResponse response = makeRequest("POST",
                                                 SERVICE_PATH,
                                                 MediaType.APPLICATION_JSON,
                                                 newWorkspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(workspaceDao).create(any(Workspace.class));
        verify(memberDao).create(any(Member.class));
        final WorkspaceDescriptor descriptor = (WorkspaceDescriptor)response.getEntity();
        assertEquals(descriptor.getName(), "test2");
        assertEquals(descriptor.getAccountId(), "test");
    }

    @Test
    public void shouldNotBeAbleToCreateMultiWorkspaces() throws Exception {
        when(workspaceDao.getByAccount("test")).thenReturn(Collections.singletonList(new Workspace().withId("test1")
                                                                                                    .withName("test1")
                                                                                                    .withTemporary(false)
                                                                                                    .withAccountId("test1")));
        final Account test = new Account().withId("test")
                                          .withName("test")
                                          .withAttributes(Collections.singletonMap("codenvy:multi-ws", "false"));
        when(accountDao.getById(test.getId())).thenReturn(test);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.singletonList(test));
        prepareSecurityContext("user");

        final NewWorkspace newWorkspace = DtoFactory.getInstance().createDto(NewWorkspace.class)
                                                    .withName("test2")
                                                    .withAccountId("test");
        ContainerResponse response = makeRequest("POST",
                                                 SERVICE_PATH,
                                                 MediaType.APPLICATION_JSON,
                                                 newWorkspace);
        assertEquals(response.getEntity().toString(), "You have not access to create more workspaces");

        verify(workspaceDao, times(0)).create(any(Workspace.class));
        verify(memberDao, times(0)).create(any(Member.class));
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspacesWithOrgAddon() throws Exception {
        isOrgAddonEnabledByDefault = "true";
        when(workspaceDao.getByAccount("test")).thenReturn(Collections.singletonList(new Workspace().withId("test1")
                                                                                                    .withName("test1")
                                                                                                    .withTemporary(false)
                                                                                                    .withAccountId("test1")));
        final Account test = new Account().withId("test")
                                          .withName("test");
        when(accountDao.getById(test.getId())).thenReturn(test);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Collections.singletonList(test));
        prepareSecurityContext("user");

        final NewWorkspace newWorkspace = DtoFactory.getInstance().createDto(NewWorkspace.class)
                                                    .withName("test2")
                                                    .withAccountId("test");
        ContainerResponse response = makeRequest("POST",
                                                 SERVICE_PATH,
                                                 MediaType.APPLICATION_JSON,
                                                 newWorkspace);
        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        verify(workspaceDao).create(any(Workspace.class));
        verify(memberDao).create(any(Member.class));
        final WorkspaceDescriptor descriptor = (WorkspaceDescriptor)response.getEntity();
        assertEquals(descriptor.getName(), "test2");
        assertEquals(descriptor.getAccountId(), "test");

    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWithExistedUser() throws Exception {
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        WorkspaceDescriptor created = (WorkspaceDescriptor)response.getEntity();
        assertTrue(created.isTemporary());
        verify(userDao, times(0)).create(any(User.class));
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verify(memberDao, times(1)).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToCreateTemporaryWorkspaceWithNotValidAttribute() throws Exception {
        workspace.getAttributes().put("codenvy:god_mode", "true");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWhenUserDoesNotExist() throws Exception {
        when(securityContext.getUserPrincipal()).thenReturn(null);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        WorkspaceDescriptor created = (WorkspaceDescriptor)response.getEntity();
        assertTrue(created.isTemporary());
        verify(userDao, times(1)).create(any(User.class));
        verify(userProfileDao, times(1)).create(any(Profile.class));
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verify(memberDao, times(1)).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToCreateWorkspaceIfAccountNotSubscribedOnMultipleWorkspaces()
            throws Exception {
        Account acc = new Account().withId(ACCOUNT_ID);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(acc));
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(acc);
        when(workspaceDao.getByAccount(ACCOUNT_ID)).thenReturn(Arrays.asList(new Workspace()));
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getEntity().toString(), "You have not access to create more workspaces");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByName() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin",
                                                                               "workspace/developer"))));
        String[] roles = new String[]{"workspace/admin", "workspace/developer", "system/admin", "system/developer"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "?name=" + WS_NAME, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            WorkspaceDescriptor ws = (WorkspaceDescriptor)response.getEntity();
            assertEquals(ws.getId(), workspace.getId());
            assertEquals(ws.getName(), workspace.getName());
            assertEquals(ws.isTemporary(), workspace.isTemporary());
            assertEquals(ws.getAccountId(), workspace.getAccountId());
            assertEquals(ws.getAttributes(), workspace.getAttributes());
            verifyLinksRel(ws.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getByName(WS_NAME);
    }

    @Test
    public void shouldNotBeAbleToRemoveAttributeIfAttributeNameStartsWithCodenvy() throws Exception {
        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/attribute?name=codenvy:runner_ram", null, null);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin"))));
        prepareSecurityContext("user");
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("test", "test");
        workspace.setAttributes(attributes);

        ContainerResponse response = makeRequest("DELETE",
                                                 SERVICE_PATH + "/" + WS_ID + "/attribute?name=test",
                                                 null,
                                                 null);

        verify(workspaceDao, times(1)).update(workspace);
        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertTrue(workspace.getAttributes().isEmpty());
    }

    @Test
    public void shouldGetWorkspaceByNameWithoutAttributesWhenUserHasNotAccessToIt() throws Exception {
        prepareSecurityContext("user");

        assertTrue(workspace.getAttributes().size() > 0);
        ContainerResponse response = makeRequest("GET",
                                                 SERVICE_PATH + "?name=" + WS_NAME,
                                                 null,
                                                 null);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        WorkspaceDescriptor ws = (WorkspaceDescriptor)response.getEntity();
        assertEquals(ws.getAttributes().size(), 0);
    }

    @Test
    public void shouldGetWorkspaceByIdWithoutAttributesWhenUserHasNotAccessToIt() throws Exception {
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        WorkspaceDescriptor ws = (WorkspaceDescriptor)response.getEntity();
        assertEquals(ws.getAttributes().size(), 0);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspace() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                                                           .withWorkspaceId(WS_ID)
                                                                                           .withRoles(Arrays.asList("workspace/admin"))));
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put("test", "test");
        workspace.setAttributes(attributes);
        WorkspaceUpdate workspaceToUpdate = DtoFactory.getInstance().createDto(WorkspaceUpdate.class).withName("ws2");
        workspaceToUpdate.setAttributes(Collections.singletonMap("test", "other_value"));
        String[] roles = new String[]{"workspace/admin", "system/admin"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("POST",
                                                     SERVICE_PATH + "/" + WS_ID,
                                                     MediaType.APPLICATION_JSON,
                                                     workspaceToUpdate);
            WorkspaceDescriptor actual = (WorkspaceDescriptor)response.getEntity();
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            assertEquals(actual.getName(), workspaceToUpdate.getName());
            assertEquals(actual.getAttributes().size(), 1);
            assertEquals(attributes.get("test"), "other_value");
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).update(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToUpdateWorkspaceIfAnyAttributeNameStartsWithCodenvy() throws Exception {
        WorkspaceUpdate workspaceToUpdate = DtoFactory.getInstance().createDto(WorkspaceUpdate.class).withName("ws2");
        workspaceToUpdate.setAttributes(Collections.singletonMap("codenvy:runner_ram", "64GB"));

        ContainerResponse response = makeRequest("POST",
                                                 SERVICE_PATH + "/" + WS_ID,
                                                 MediaType.APPLICATION_JSON,
                                                 workspaceToUpdate);

        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        List<Member> members = Arrays.asList(new Member().withWorkspaceId(WS_ID)
                                                         .withUserId(USER_ID)
                                                         .withRoles(Arrays.asList("workspace/admin")));
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(members);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(members);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID + "/members", null, null);
        //safe cast cause WorkspaceService#getMembers always return List<Member>
        @SuppressWarnings("unchecked") List<MemberDescriptor> descriptors = (List<MemberDescriptor>)response.getEntity();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(descriptors.size(), 1);
        verify(memberDao, times(1)).getWorkspaceMembers(WS_ID);
        verifyLinksRel(descriptors.get(0).getLinks(), Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                                                    Constants.LINK_REL_GET_WORKSPACE_MEMBER,
                                                                    com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID));
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMember() throws Exception {
        Member membership = new Member().withWorkspaceId(WS_ID)
                                        .withUserId(USER_ID)
                                        .withRoles(Arrays.asList("workspace/admin"));
        when(memberDao.getWorkspaceMember(WS_ID, USER_ID)).thenReturn(membership);
        prepareSecurityContext("workspace/admin");

        User user = DtoFactory.getInstance().createDto(User.class).withId(USER_ID);
        when(userDao.getById(USER_ID)).thenReturn(user);

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID + "/membership", null, null);
        //safe cast cause WorkspaceService#getMembers always return List<Member>
        @SuppressWarnings("unchecked") MemberDescriptor descriptor = (MemberDescriptor)response.getEntity();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        verify(memberDao, times(1)).getWorkspaceMember(WS_ID, USER_ID);
        verifyLinksRel(descriptor.getLinks(), Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                                            Constants.LINK_REL_GET_WORKSPACE_MEMBER,
                                                            com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID));
    }

    @Test
    public void shouldBeAbleToAddWorkspaceMembership() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin"))));
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        MemberDescriptor memberDescriptor = (MemberDescriptor)response.getEntity();
        assertEquals(memberDescriptor.getRoles(), membership.getRoles());
        assertEquals(memberDescriptor.getUserId(), USER_ID);
        assertEquals(memberDescriptor.getWorkspaceReference().getId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(memberDescriptor.getLinks(), Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                                                  Constants.LINK_REL_GET_WORKSPACE_MEMBER,
                                                                  com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID));
    }

    @Test
    public void shouldNotBeAbleToAddMembershipToPermanentWorkspaceIfUserIsNotWorkspaceAdmin() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/developer"))));
        prepareSecurityContext("workspace/developer");

        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getEntity().toString(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberForAnyUserIfAllowAttributeIsTrue() throws Exception {
        workspace.getAttributes().put("allowAnyoneAddMember", "true");
        prepareSecurityContext("user");
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        MemberDescriptor memberDescriptor = (MemberDescriptor)response.getEntity();
        assertEquals(memberDescriptor.getRoles(), membership.getRoles());
        assertEquals(memberDescriptor.getUserId(), USER_ID);
        assertEquals(memberDescriptor.getWorkspaceReference().getId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(memberDescriptor.getLinks(), Arrays.asList(com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID));
    }

    @Test
    public void shouldNotBeAbleToAddMemberForAnyUserIfAllowAttributeIsFalse() throws Exception {
        workspace.getAttributes().put("allowAnyoneAddMember", "false");
        prepareSecurityContext("user");
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getEntity().toString(), "Access denied");
    }

    @Test
    public void shouldNotBeAbleToAddMemberForAnyUserIfAllowAttributeMissed() throws Exception {
        prepareSecurityContext("user");
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getEntity().toString(), "Access denied");
    }

    @Test
    public void shouldNotBeAbleToAddNewWorkspaceMembershipWithoutRoles() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin"))));
        prepareSecurityContext("workspace/admin");
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class).withUserId(USER_ID);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getEntity().toString(), "Roles should not be empty");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        List<Member> wsMembers = Arrays.asList(new Member().withUserId("FAKE")
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/admin")),
                                               new Member().withUserId(USER_ID)
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/developer"))
                                              );
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(wsMembers);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(wsMembers);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(memberDao, times(1)).remove(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToRemoveLastWorkspaceAdmin() throws Exception {
        List<Member> wsMembers = Arrays.asList(new Member().withUserId(USER_ID)
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/admin")),
                                               new Member().withUserId("FAKE")
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/developer"))
                                              );
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(wsMembers);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(wsMembers);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getEntity(), "Workspace should have at least 1 admin");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceAdminIfOtherOneExists() throws Exception {
        List<Member> wsMembers = Arrays.asList(new Member().withUserId(USER_ID)
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/admin")),
                                               new Member().withUserId("FAKE")
                                                           .withWorkspaceId(WS_ID)
                                                           .withRoles(Arrays.asList("workspace/admin"))
                                              );
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(wsMembers);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(wsMembers);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(memberDao, times(1)).remove(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspace() throws Exception {
        when(memberDao.getUserRelationships(USER_ID))
                .thenReturn(Arrays.asList(new Member().withUserId(USER_ID)
                                                      .withWorkspaceId(WS_ID)
                                                      .withRoles(Arrays.asList("workspace/admin"))));
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(new Member().withWorkspaceId(WS_ID).withUserId(USER_ID)));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        verify(workspaceDao, times(1)).remove(WS_ID);
    }

    private ContainerResponse makeRequest(String method, String path, String contentType, Object toSend) throws Exception {
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

    private void verifyLinksRel(List<Link> links, List<String> rels) {
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

    private List<String> generateRels(String role) {
        List<String> result = new ArrayList<>();
        result.add(Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES);
        result.add("get projects");
        switch (role) {
            case "workspace/admin":
                result.add(Constants.LINK_REL_ADD_WORKSPACE_MEMBER);
                result.add(Constants.LINK_REL_GET_WORKSPACE_MEMBER);
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

    private void prepareSecurityContext(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        if (!securityContext.isUserInRole("system/admin") && !securityContext.isUserInRole("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
