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
package com.codenvy.api.local;

import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.server.dao.SubscriptionAttributes;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@DynaModule
public class LocalInfrastructureModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(com.codenvy.api.user.server.dao.UserDao.class).to(LocalUserDaoImpl.class);
        bind(com.codenvy.api.workspace.server.dao.WorkspaceDao.class).to(LocalWorkspaceDaoImpl.class);
        bind(com.codenvy.api.user.server.dao.UserProfileDao.class).to(LocalProfileDaoImpl.class);
        bind(com.codenvy.api.user.server.dao.PreferenceDao.class).to(LocalPreferenceDaoImpl.class);
        bind(com.codenvy.api.workspace.server.dao.MemberDao.class).to(LocalMemberDaoImpl.class);
        bind(com.codenvy.api.account.server.dao.AccountDao.class).to(LocalAccountDaoImpl.class);
        bind(com.codenvy.api.auth.AuthenticationDao.class).to(LocalAuthenticationDaoImpl.class);
        bind(com.codenvy.api.factory.FactoryStore.class).to(InMemoryFactoryStore.class);
        bind(com.codenvy.api.user.server.TokenValidator.class).to(DummyTokenValidator.class);
    }


    //~~~ AccountDao

    @Provides
    @Named("codenvy.local.infrastructure.accounts")
    Set<Account> accounts() {
        final Set<Account> accounts = new HashSet<>(1);
        accounts.add(new Account().withName("codenvy_account").withId("account1234567890"));
        return accounts;
    }

    @Provides
    @Named("codenvy.local.infrastructure.account.members")
    Set<com.codenvy.api.account.server.dao.Member> accountMembers() {
        final Set<com.codenvy.api.account.server.dao.Member> members = new HashSet<>(1);
        final com.codenvy.api.account.server.dao.Member member =
                new com.codenvy.api.account.server.dao.Member().withUserId("codenvy").withAccountId("account1234567890");
        Collections.addAll(member.getRoles(), "account/owner", "account/member");
        members.add(member);
        return members;
    }

    @Provides
    @Named("codenvy.local.infrastructure.account.subscriptions")
    Set<Subscription> subscriptions() {
        return Collections.emptySet();
    }

    @Provides
    @Named("codenvy.local.infrastructure.account.subscriptionAttributes")
    Map<String, SubscriptionAttributes> subscriptionAttributes() {
        return Collections.emptyMap();
    }

    // AccountDao ~~~


    // ~~~ WorkspaceDao

    @Provides
    @Named("codenvy.local.infrastructure.workspaces")
    Set<Workspace> workspaces() {
        final Set<Workspace> workspaces = new HashSet<>(1);
        workspaces.add(new Workspace().withId("1q2w3e").withName("default").withTemporary(false));
        return workspaces;
    }

    // WorkspaceDao ~~~


    // ~~~ MemberDao

    @Provides
    @Named("codenvy.local.infrastructure.workspace.members")
    Set<com.codenvy.api.workspace.server.dao.Member> workspaceMembers() {
        final Set<com.codenvy.api.workspace.server.dao.Member> members = new HashSet<>(1);
        final com.codenvy.api.workspace.server.dao.Member member =
                new com.codenvy.api.workspace.server.dao.Member().withUserId("codenvy").withWorkspaceId("1q2w3e");
        Collections.addAll(member.getRoles(), "workspace/admin", "workspace/developer");
        members.add(member);
        return members;
    }

    // MemberDao ~~~


    // ~~~ UserDao

    @Provides
    @Named("codenvy.local.infrastructure.users")
    Set<User> users() {
        final Set<User> users = new HashSet<>(1);
        final User user = new User().withId("codenvy")
                                    .withEmail("codenvy@codenvy.com")
                                    .withPassword("secret");
        user.getAliases().add("codenvy@codenvy.com");
        users.add(user);
        return users;
    }

    // UserDao ~~~
}
