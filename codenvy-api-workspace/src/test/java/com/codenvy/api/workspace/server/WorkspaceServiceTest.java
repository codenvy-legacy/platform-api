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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.ApiExceptionMapper;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.shared.dto.UserDescriptor;
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
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerRequest;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static com.codenvy.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
import static com.codenvy.api.project.server.Constants.LINK_REL_GET_PROJECTS;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_BY_ID;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_MEMBERS;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_GET_WORKSPACE_BY_NAME;
import static com.codenvy.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE;

/**
 * Tests for {@link com.codenvy.api.workspace.server.WorkspaceService}
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.workspace.server.WorkspaceService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    private static final String BASE_URI     = "http://localhost/service";
    private static final String SERVICE_PATH = BASE_URI + "/workspace";

    @Mock
    private WorkspaceDao       workspaceDao;
    @Mock
    private UserDao            userDao;
    @Mock
    private MemberDao          memberDao;
    @Mock
    private AccountDao         accountDao;
    @Mock
    private UserProfileDao     profileDao;
    @Mock
    private SecurityContext    securityContext;
    @Mock
    private EnvironmentContext environmentContext;
    private ResourceLauncher   launcher;
    private WorkspaceService   service;
    private User               testUser;
    private String             isOrgAddonEnabledByDefault;

    @BeforeMethod
    public void before() throws Exception {
        //set up launcher
        isOrgAddonEnabledByDefault = "false";
        final ResourceBinderImpl resources = new ResourceBinderImpl();
        resources.addResource(WorkspaceService.class, null);
        final DependencySupplierImpl dependencies = new DependencySupplierImpl() {
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
        dependencies.addComponent(UserProfileDao.class, profileDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        final ApplicationProviderBinder binder = new ApplicationProviderBinder();
        binder.addExceptionMapper(ApiExceptionMapper.class);
        final URI uri = new URI(BASE_URI);
        final ContainerRequest req = new ContainerRequest(null, uri, uri, null, null, securityContext);
        final ApplicationContextImpl contextImpl = new ApplicationContextImpl(req, null, binder);
        contextImpl.setDependencySupplier(dependencies);
        ApplicationContextImpl.setCurrent(contextImpl);
        final EverrestProcessor processor = new EverrestProcessor(resources,
                                                                  binder,
                                                                  dependencies,
                                                                  new EverrestConfiguration(),
                                                                  null);
        launcher = new ResourceLauncher(processor);
        service = (WorkspaceService)resources.getMatchedResource("/workspace", new ArrayList<String>())
                                             .getInstance(ApplicationContextImpl.getCurrent());
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        //set up test user
        final String userId = "test_user_id";
        final String userEmail = "test@test.com";
        testUser = new User().withId(userId).withEmail(userEmail);
        when(userDao.getById(userId)).thenReturn(testUser);
        when(userDao.getByAlias(userEmail)).thenReturn(testUser);
        com.codenvy.commons.env.EnvironmentContext.getCurrent().setUser(new com.codenvy.commons.user.User() {

            @Override
            public String getName() {
                return testUser.getEmail();
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public String getToken() {
                return null;
            }

            @Override
            public String getId() {
                return testUser.getId();
            }

            @Override
            public boolean isTemporary() {
                return false;
            }
        });
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(testUser.getEmail()));
    }

    @Test
    public void shouldBeAbleToCreateFirstWorkspace() throws Exception {
        //create new account with empty workspace list
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(Collections.<Workspace>emptyList());
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
        assertFalse(descriptor.isTemporary());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspacesIfAccountContainsMultiWsAttribute() throws Exception {
        //create new account with existed workspace and 'codenvy:multi-ws' attribute equal with 'true'
        final Account testAccount = createAccount().withAttributes(Collections.singletonMap("codenvy:multi-ws", "true"));
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(singletonList(new Workspace()));
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("test_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToCreateMultiWorkspacesIfAccountDoesNotContainMultiWsAttribute() throws Exception {
        //create new account with existed workspace with empty attributes
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(singletonList(new Workspace()));
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("test_workspace")
                                                                    .withAccountId(testAccount.getId());

        final String errorJson = doPost(SERVICE_PATH, newWorkspace, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "You don't have access to create more workspaces");
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspacesWithOrgAddon() throws Exception {
        isOrgAddonEnabledByDefault = "true";
        //create new account with existed workspace with empty attributes
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(singletonList(new Workspace()));
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("test_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspacesForSystemAdmin() throws Exception {
        prepareRole("system/admin");
        //create new account with existed workspace with empty attributes
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(singletonList(new Workspace()));
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("test_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToCreateNewWorkspaceWithNotValidAttribute() throws Exception {
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account")
                                                                    .withAttributes(singletonMap("codenvy:god_mode", "true"));

        final String errorJson = doPost(SERVICE_PATH, newWorkspace, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspace() throws Exception {
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account");

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/temp", newWorkspace, CREATED);

        assertTrue(descriptor.isTemporary());
        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(userDao, never()).create(any(User.class));
        verify(profileDao, never()).create(any(Profile.class));
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWhenUserDoesNotExist() throws Exception {
        when(securityContext.getUserPrincipal()).thenReturn(null);
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account");

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/temp", newWorkspace, CREATED);

        assertTrue(descriptor.isTemporary());
        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(userDao).create(any(User.class));
        verify(profileDao).create(any(Profile.class));
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToCreateTemporaryWorkspaceWithNotValidAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("codenvy:god_mode", "true");

        final String errorJson = doPost(SERVICE_PATH + "/temp", testWorkspace, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributesForWorkspaceAdminOrDeveloper() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());
        prepareRole("workspace/admin");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithEmptyAttributesForAnyone() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributeAllowAnyoneAddMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("test2_attribute", "test");
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("allowAnyoneAddMember"), "true");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameForWorkspaceAdminOrDeveloper() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());
        prepareRole("workspace/admin");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "?name=" + testWorkspace.getName());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameWithEmptyAttributesForAnyone() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameWithAttributeAllowAnyoneAddMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("test2_attribute", "test");
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("allowAnyoneAddMember"), "true");
    }

    @Test
    public void shouldNotBeAbleToRemoveAttributeIfAttributeNameStartsWithCodenvy() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final String errorJson = doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/attribute?name=codenvy:runner_ram", CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace().withAttributes(new HashMap<>(singletonMap("test", "test")));

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/attribute?name=test", NO_CONTENT);

        verify(workspaceDao).update(testWorkspace);
        assertTrue(testWorkspace.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToUpdateWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace().withAttributes(new HashMap<>(singletonMap("test", "test")));
        final String newName = "new_workspace";
        //workspace update descriptor
        final WorkspaceUpdate update = newDTO(WorkspaceUpdate.class).withName(newName)
                                                                    .withAttributes(singletonMap("test", "other_value"));

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId(), update, OK);

        assertEquals(descriptor.getName(), newName);
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("test"), "other_value");
        verify(workspaceDao).update(testWorkspace);
    }

    @Test
    public void shouldNotBeAbleToUpdateWorkspaceIfUpdateContainsNotValidAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final String newName = "new_workspace";
        //ensure workspace with update workspace name doesn't exist
        when(workspaceDao.getByName(newName)).thenThrow(new NotFoundException("workspace doesn't exist"));
        //workspace update descriptor
        final WorkspaceUpdate update = newDTO(WorkspaceUpdate.class).withName(newName)
                                                                    .withAttributes(singletonMap("codenvy:runner_ram", "64GB"));

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId(), update, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withWorkspaceId(testWorkspace.getId())
                                              .withUserId(testUser.getId())
                                              .withRoles(singletonList("workspace/admin"));
        final List<Member> members = singletonList(testMember);
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(members);

        final List<MemberDescriptor> descriptors = doGet(SERVICE_PATH + "/" + testWorkspace.getId() + "/members");

        assertEquals(descriptors.size(), 1);
        final MemberDescriptor descriptor = descriptors.get(0);
        assertEquals(descriptor.getUserId(), testMember.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testMember.getWorkspaceId());
        assertEquals(descriptor.getRoles(), testMember.getRoles());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withWorkspaceId(testWorkspace.getId())
                                              .withUserId(testUser.getId())
                                              .withRoles(singletonList("workspace/admin"));
        when(memberDao.getWorkspaceMember(testMember.getWorkspaceId(), testMember.getUserId())).thenReturn(testMember);

        final MemberDescriptor descriptor = doGet(SERVICE_PATH + "/" + testMember.getWorkspaceId() + "/membership");

        assertEquals(descriptor.getUserId(), testMember.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testMember.getWorkspaceId());
        assertEquals(descriptor.getRoles(), testMember.getRoles());
    }

    @Test
    public void shouldBeAbleToAddWorkspaceMemberToNotEmptyWorkspaceForWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId("test_user_id");
        prepareRole("workspace/admin");

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        assertEquals(descriptor.getRoles(), membership.getRoles());
        verify(memberDao).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceIfUserIsNotWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());
        prepareRole("workspace/developer");

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeIsTrue() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getRoles(), membership.getRoles());
        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        verify(memberDao).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeIsFalse() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(any(Member.class)));
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "false");
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeMissed() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberToEmptyWorkspaceForAnyUser() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final NewMembership membership = newDTO(NewMembership.class).withUserId(testUser.getId());

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        assertEquals(new HashSet<>(descriptor.getRoles()), new HashSet<>(asList("workspace/admin", "workspace/developer")));
    }

    @Test
    public void shouldNotBeAbleToAddNewWorkspaceMemberWithoutOfRolesToNotEmptyWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withUserId(testUser.getId());
        prepareRole("workspace/admin");

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Roles should not be empty");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final List<Member> members = asList(new Member().withUserId(testUser.getId())
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/admin")),
                                            new Member().withUserId("test_member")
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/developer")));
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(members);

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/members/test_member", NO_CONTENT);

        verify(memberDao).remove(members.get(1));
    }

    @Test
    public void shouldNotBeAbleToRemoveLastWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final List<Member> members = asList(new Member().withUserId(testUser.getId())
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/admin")),
                                            new Member().withUserId("test_member")
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/developer")));
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(members);

        final String errorJson = doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/members/" + testUser.getId(), CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Workspace should have at least 1 admin");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceAdminIfOtherOneExists() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final List<Member> members = asList(new Member().withUserId(testUser.getId())
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/admin")),
                                            new Member().withUserId("FAKE")
                                                        .withWorkspaceId(testWorkspace.getId())
                                                        .withRoles(singletonList("workspace/admin")));
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(members);

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/members/" + testUser.getId(), NO_CONTENT);

        verify(memberDao).remove(members.get(0));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId(), NO_CONTENT);

        verify(workspaceDao).remove(testWorkspace.getId());
    }

    @Test
    public void testWorkspaceDescriptorLinksForUser() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        when(securityContext.isUserInRole("user")).thenReturn(true);

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForWorkspaceDeveloper() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("workspace/developer");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              LINK_REL_GET_WORKSPACE_BY_ID,
                                                              LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForWorkspaceAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("workspace/admin");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              LINK_REL_GET_WORKSPACE_BY_ID,
                                                              LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_REMOVE_WORKSPACE,
                                                              LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForSystemManager() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("system/manager");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              LINK_REL_GET_WORKSPACE_BY_ID,
                                                              LINK_REL_GET_WORKSPACE_MEMBERS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForSystemAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("system/admin");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              LINK_REL_GET_WORKSPACE_BY_ID,
                                                              LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_REMOVE_WORKSPACE));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testMemberDescriptorLinksForWorkspaceDeveloper() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withUserId(testUser.getId())
                                              .withWorkspaceId(testWorkspace.getId());
        prepareRole("workspace/developer");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_GET_USER_BY_ID));

        assertEquals(asRels(service.toDescriptor(testMember, testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testMemberDescriptorLinksForWorkspaceAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withUserId(testUser.getId())
                                              .withWorkspaceId(testWorkspace.getId());
        prepareRole("workspace/admin");

        final Set<String> expectedRels = new HashSet<>(asList(LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_GET_USER_BY_ID,
                                                              LINK_REL_REMOVE_WORKSPACE_MEMBER));

        assertEquals(asRels(service.toDescriptor(testMember, testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @SuppressWarnings("unchecked")
    private <T> T doDelete(String path, Status expectedResponseStatus) throws Exception {
        final ContainerResponse response = launcher.service("DELETE", path, BASE_URI, null, null, null, environmentContext);
        assertEquals(response.getStatus(), expectedResponseStatus.getStatusCode());
        return (T)response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private <T> T doGet(String path) throws Exception {
        final ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null, environmentContext);
        assertEquals(response.getStatus(), OK.getStatusCode());
        return (T)response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private <T> T doPost(String path, Object entity, Status expectedResponseStatus) throws Exception {
        final byte[] data = JsonHelper.toJson(entity).getBytes();
        final Map<String, List<String>> headers = new HashMap<>(4);
        headers.put("Content-Type", singletonList("application/json"));
        final ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, data, null, environmentContext);
        assertEquals(response.getStatus(), expectedResponseStatus.getStatusCode());
        return (T)response.getEntity();
    }

    private ServiceError asError(String json) {
        return DtoFactory.getInstance().createDtoFromJson(json, ServiceError.class);
    }

    private <T> T newDTO(Class<T> dto) {
        return DtoFactory.getInstance().createDto(dto);
    }

    private Set<String> asRels(List<Link> links) {
        final Set<String> rels = new HashSet<>();
        for (Link link : links) {
            rels.add(link.getRel());
        }
        return rels;
    }

    private Account createAccount() throws NotFoundException, ServerException {
        final Account account = new Account().withId("fake_account_id");
        when(accountDao.getById(account.getId())).thenReturn(account);
        when(accountDao.getByOwner(testUser.getId())).thenReturn(singletonList(account));
        return account;
    }

    private Workspace createWorkspace() throws NotFoundException, ServerException {
        final String workspaceId = "test_workspace_id";
        final String workspaceName = "test_workspace_name";
        final String accountId = "test_account_id";
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("default_attribute", "default_value");
        final Workspace testWorkspace = new Workspace().withId(workspaceId)
                                                       .withName(workspaceName)
                                                       .withTemporary(false)
                                                       .withAccountId(accountId)
                                                       .withAttributes(attributes);
        when(workspaceDao.getById(workspaceId)).thenReturn(testWorkspace);
        when(workspaceDao.getByName(workspaceName)).thenReturn(testWorkspace);
        when(workspaceDao.getByAccount(accountId)).thenReturn(singletonList(testWorkspace));
        return testWorkspace;
    }

    private void prepareRole(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        if (!role.equals("system/admin") && !role.equals("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
