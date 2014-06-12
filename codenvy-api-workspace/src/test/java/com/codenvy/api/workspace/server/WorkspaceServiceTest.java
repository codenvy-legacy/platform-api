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

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.shared.dto.Account;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.Member;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Attribute;
import com.codenvy.api.workspace.shared.dto.NewMembership;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
    private AccountDao accountDao;

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
        //count of attributes should be > 0
        List<Attribute> attributes = new ArrayList<>(1);
        attributes.add(DtoFactory.getInstance().createDto(Attribute.class)
                                 .withName("default_attribute")
                                 .withValue("default_value")
                                 .withDescription("default_description"));
        workspace = DtoFactory.getInstance().createDto(Workspace.class)
                              .withId(WS_ID)
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
    }

    @Test
    public void shouldBeAbleToGetWorkspaceById() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class).withUserId(USER_ID).withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin", "workspace/developer"))
                                                                              ));
        String[] roles = new String[]{"workspace/admin", "workspace/developer", "system/admin", "system/manager"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Workspace ws = (Workspace)response.getEntity();
            assertEquals(ws, workspace);
            verifyLinksRel(ws.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getById(WS_ID);
    }

    @Test
    public void shouldBeAbleToCreateNewWorkspace() throws Exception {
        Account acc = DtoFactory.getInstance().createDto(Account.class).withId(ACCOUNT_ID);
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(acc);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(acc));

        String[] roles = new String[]{"user", "system/admin"};
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
    public void shouldNotBeAbleToCreateNewWorkspaceWithNotValidAttribute() throws Exception {
        workspace.getAttributes().add(DtoFactory.getInstance().createDto(Attribute.class).withName("codenvy:god_mode").withValue("true"));
        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWithExistedUser() throws Exception {
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());
        Workspace created = (Workspace)response.getEntity();
        assertTrue(created.isTemporary());
        verify(userDao, times(0)).create(any(User.class));
        verify(workspaceDao, times(1)).create(any(Workspace.class));
        verify(memberDao, times(1)).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToCreateTemporaryWorkspaceWithNotValidAttribute() throws Exception {
        workspace.getAttributes().add(DtoFactory.getInstance().createDto(Attribute.class).withName("codenvy:god_mode").withValue("true"));
        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/temp", MediaType.APPLICATION_JSON, workspace);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:god_mode' is not valid");
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
    public void shouldNotBeAbleToCreateWorkspaceIfAccountNotSubscribedOnMultipleWorkspaces()
            throws Exception {
        Account acc = DtoFactory.getInstance().createDto(Account.class).withId(ACCOUNT_ID);
        when(accountDao.getByOwner(USER_ID)).thenReturn(Arrays.asList(acc));
        when(accountDao.getById(ACCOUNT_ID)).thenReturn(acc);
        when(workspaceDao.getByAccount(ACCOUNT_ID))
                .thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Workspace.class)));

        prepareSecurityContext("user");
        ContainerResponse response = makeRequest("POST", SERVICE_PATH, MediaType.APPLICATION_JSON, workspace);
        assertEquals(response.getEntity().toString(), "You have not access to create more workspaces");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByName() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class)
                          .withUserId(USER_ID)
                          .withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin", "workspace/developer"))
                                                                              ));
        String[] roles = new String[]{"workspace/admin", "workspace/developer", "system/admin", "system/developer"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("GET", SERVICE_PATH + "?name=" + WS_NAME, null, null);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Workspace ws = (Workspace)response.getEntity();
            assertEquals(ws, workspace);
            verifyLinksRel(ws.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).getByName(WS_NAME);
    }

    @Test
    public void shouldBeAbleToAddNewAttribute() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class)
                          .withUserId(USER_ID)
                          .withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        prepareSecurityContext("user");
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("newAttribute")
                                           .withValue("value")
                                           .withDescription("description");
        int countBefore = workspace.getAttributes().size();

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(workspace.getAttributes().size(), countBefore + 1);
        verify(workspaceDao, times(1)).update(workspace);
    }

    @Test
    public void shouldNotBeAbleToAddNewAttributeIfAttributeNameStartsWithCodenvy() throws Exception {
        prepareSecurityContext("user");
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName("codenvy:runner_ram")
                                           .withValue("64GB")
                                           .withDescription("Runner ram");

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldNotBeAbleToRemoveAttributeIfAttributeNameStartsWithCodenvy() throws Exception {
        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/attribute?name=codenvy:runner_ram", null, null);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class)
                          .withUserId(USER_ID)
                          .withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        prepareSecurityContext("user");
        int countBefore = workspace.getAttributes().size();
        assertTrue(countBefore > 0);
        Attribute defaultAttribute = workspace.getAttributes().get(0);

        ContainerResponse response =
                makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/attribute?name=" + defaultAttribute.getName(), null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(workspace.getAttributes().size(), countBefore - 1);
        verify(workspaceDao, times(1)).update(workspace);
    }

    @Test
    public void shouldBeAbleToReplaceExistedAttribute() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class)
                          .withUserId(USER_ID)
                          .withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        prepareSecurityContext("user");
        int countBefore = workspace.getAttributes().size();
        assertTrue(countBefore > 0);
        Attribute defaultAttribute = workspace.getAttributes().get(0);
        Attribute newAttribute = DtoFactory.getInstance().createDto(Attribute.class)
                                           .withName(defaultAttribute.getName())
                                           .withValue("other_value")
                                           .withDescription("nobody cares about it");

        ContainerResponse response =
                makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/attribute", MediaType.APPLICATION_JSON, newAttribute);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        assertEquals(workspace.getAttributes().size(), 1);
        Attribute actual = workspace.getAttributes().get(0);
        assertEquals(actual.getName(), newAttribute.getName());
        assertEquals(actual.getValue(), newAttribute.getValue());
        assertEquals(actual.getDescription(), newAttribute.getDescription());
    }

    @Test
    public void shouldGetWorkspaceByNameWithoutAttributesWhenUserHasNotAccessToIt() throws Exception {
        prepareSecurityContext("user");

        assertTrue(workspace.getAttributes().size() > 0);
        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "?name=" + WS_NAME, null, null);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Workspace ws = (Workspace)response.getEntity();
        assertEquals(ws.getAttributes().size(), 0);
    }

    @Test
    public void shouldGetWorkspaceByIdWithoutAttributesWhenUserHasNotAccessToIt() throws Exception {
        prepareSecurityContext("user");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Workspace ws = (Workspace)response.getEntity();
        assertEquals(ws.getAttributes().size(), 0);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspace() throws Exception {
        Workspace workspaceToUpdate = DtoFactory.getInstance().createDto(Workspace.class).withName("ws2");
        assertTrue(workspace.getAttributes().size() > 0);
        Attribute old = workspace.getAttributes().get(0);
        workspaceToUpdate.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                .withName(old.getName())
                                                                .withValue("other_attribute_value")));

        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class)
                          .withUserId(USER_ID)
                          .withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        String[] roles = new String[]{"workspace/admin", "system/admin"};
        for (String role : roles) {
            prepareSecurityContext(role);
            ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID, MediaType.APPLICATION_JSON, workspaceToUpdate);
            Workspace actual = (Workspace)response.getEntity();
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            assertEquals(actual.getName(), workspaceToUpdate.getName());
            assertEquals(actual.getAttributes().size(), 1);
            Attribute attribute = actual.getAttributes().get(0);
            assertEquals(attribute.getName(), old.getName());
            assertEquals(attribute.getValue(), "other_attribute_value");
            verifyLinksRel(actual.getLinks(), generateRels(role));
        }
        verify(workspaceDao, times(roles.length)).update(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToUpdateWorkspaceIfAnyAttributeNameStartsWithCodenvy() throws Exception {
        Workspace workspaceToUpdate = DtoFactory.getInstance().createDto(Workspace.class).withName("ws2");
        workspaceToUpdate.setAttributes(Arrays.asList(DtoFactory.getInstance().createDto(Attribute.class)
                                                                .withName("codenvy:runner_ram")
                                                                .withValue("64GB")));

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID, MediaType.APPLICATION_JSON, workspaceToUpdate);
        assertEquals(response.getEntity().toString(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        List<Member> members = Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                       .withWorkspaceId(WS_ID)
                                                       .withUserId(USER_ID)
                                                       .withRoles(Arrays.asList("workspace/admin")));
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(members);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(members);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("GET", SERVICE_PATH + "/" + WS_ID + "/members", null, null);
        //safe cast cause WorkspaceService#getMembers always return List<Member>
        @SuppressWarnings("unchecked") List<Member> actualMembers = (List<Member>)response.getEntity();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(actualMembers.size(), 1);
        verify(memberDao, times(1)).getWorkspaceMembers(WS_ID);
        verifyLinksRel(actualMembers.get(0).getLinks(),
                       Arrays.asList(Constants.LINK_REL_GET_WORKSPACE_MEMBERS, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER,
                                     com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_PROFILE_BY_ID)
                      );
    }


    @Test
    public void shouldBeAbleToAddWorkspaceMembership() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class).withUserId(USER_ID).withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Member member = (Member)response.getEntity();
        assertEquals(member.getRoles(), membership.getRoles());
        assertEquals(member.getUserId(), USER_ID);
        assertEquals(member.getWorkspaceId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(member.getLinks(),
                       Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
    }

    @Test
    public void shouldNotBeAbleToAddMembershipToPermanentWorkspaceIfUserIsNotWorkspaceAdmin() throws Exception {
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class).withUserId(USER_ID).withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/developer"))
                                                                              ));
        prepareSecurityContext("workspace/developer");

        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getEntity().toString(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberForAnyUserIfAllowAttributeIsTrue() throws Exception {
        workspace.getAttributes().add(DtoFactory.getInstance().createDto(Attribute.class)
                                                .withName("allowAnyoneAddMember")
                                                .withValue("true"));
        prepareSecurityContext("user");
        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withRoles(Arrays.asList("workspace/developer"))
                                             .withUserId(USER_ID);

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);

        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Member member = (Member)response.getEntity();
        assertEquals(member.getRoles(), membership.getRoles());
        assertEquals(member.getUserId(), USER_ID);
        assertEquals(member.getWorkspaceId(), WS_ID);
        verify(memberDao, times(1)).create(any(Member.class));
        verifyLinksRel(member.getLinks(),
                       Arrays.asList(Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER, Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
    }

    @Test
    public void shouldNotBeAbleToAddMemberForAnyUserIfAllowAttributeIsFalse() throws Exception {
        workspace.getAttributes().add(DtoFactory.getInstance().createDto(Attribute.class)
                                                .withName("allowAnyoneAddMember")
                                                .withValue("false"));
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
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class).withUserId(USER_ID).withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));

        prepareSecurityContext("workspace/admin");

        NewMembership membership = DtoFactory.getInstance().createDto(NewMembership.class)
                                             .withUserId(USER_ID);

        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("POST", SERVICE_PATH + "/" + WS_ID + "/members", MediaType.APPLICATION_JSON, membership);
        assertEquals(response.getEntity().toString(), "Roles required");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        List<Member> wsMembers = Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId(USER_ID)
                                                         .withWorkspaceId(WS_ID)
                                                         .withRoles(Arrays.asList("workspace/developer")),
                                               DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId("FAKE")
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
    public void shouldNotBeAbleToRemoveLastWorkspaceAdmin() throws Exception {
        List<Member> wsMembers = Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId(USER_ID)
                                                         .withWorkspaceId(WS_ID)
                                                         .withRoles(Arrays.asList("workspace/admin")),
                                               DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId("FAKE")
                                                         .withWorkspaceId(WS_ID)
                                                         .withRoles(Arrays.asList("workspace/developer"))
                                              );
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(wsMembers);
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(wsMembers);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID + "/members/" + USER_ID, null, null);

        assertEquals(response.getEntity().toString(), "Workspace should have at least 1 admin");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceAdminIfOtherOneExists() throws Exception {
        List<Member> wsMembers = Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId(USER_ID)
                                                         .withWorkspaceId(WS_ID)
                                                         .withRoles(Arrays.asList("workspace/admin")),
                                               DtoFactory.getInstance().createDto(Member.class)
                                                         .withUserId("FAKE")
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
        when(memberDao.getUserRelationships(USER_ID)).thenReturn(Arrays.asList(
                DtoFactory.getInstance().createDto(Member.class).withUserId(USER_ID).withWorkspaceId(WS_ID)
                          .withRoles(Arrays.asList("workspace/admin"))
                                                                              ));
        when(memberDao.getWorkspaceMembers(WS_ID)).thenReturn(Arrays.asList(DtoFactory.getInstance().createDto(Member.class)
                                                                                      .withWorkspaceId(WS_ID)
                                                                                      .withUserId(USER_ID)));
        when(workspaceDao.getById(WS_ID)).thenReturn(workspace);
        prepareSecurityContext("workspace/admin");

        ContainerResponse response = makeRequest("DELETE", SERVICE_PATH + "/" + WS_ID, null, null);

        assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
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

    private List<String> generateRels(String role) {
        List<String> result = new ArrayList<>();
        result.add(Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES);
        result.add("get projects");
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
        if (!securityContext.isUserInRole("system/admin") && !securityContext.isUserInRole("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
