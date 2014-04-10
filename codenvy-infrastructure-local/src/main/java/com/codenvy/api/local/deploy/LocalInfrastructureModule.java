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
package com.codenvy.api.local.deploy;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.local.LocalAccountDaoImpl;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;

@DynaModule
public class LocalInfrastructureModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(com.codenvy.api.user.server.dao.UserDao.class).to(com.codenvy.api.local.LocalUserDaoImpl.class);
        bind(com.codenvy.api.workspace.server.dao.WorkspaceDao.class).to(com.codenvy.api.local.LocalWorkspaceDaoImpl.class);
        bind(com.codenvy.api.user.server.dao.UserProfileDao.class).to(com.codenvy.api.local.LocalProfileDaoImpl.class);
        bind(com.codenvy.api.user.server.dao.MemberDao.class).to(com.codenvy.api.local.LocalMemberDaoImpl.class);
        bind(AccountDao.class).to(LocalAccountDaoImpl.class);
        bind(com.codenvy.api.auth.AuthenticationDao.class).to(com.codenvy.api.local.LocalAuthenticationDaoImpl.class);
        bind(com.codenvy.api.factory.FactoryStore.class).to(com.codenvy.api.local.InMemoryFactoryStore.class);
    }
}
