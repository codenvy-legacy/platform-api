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
package com.codenvy.api.local.deploy;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.local.LocalAccountDaoImpl;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;

@DynaModule
public class LocalInfrastructureModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(com.codenvy.api.user.server.dao.UserDao.class).to(com.codenvy.api.local.LocalUserDaoImpl.class);
        bind(com.codenvy.api.workspace.server.dao.WorkspaceDao.class).to(com.codenvy.api.local.LocalWorkspaceDaoImpl.class);
        bind(com.codenvy.api.user.server.dao.UserProfileDao.class).to(com.codenvy.api.local.LocalProfileDaoImpl.class);
        bind(MemberDao.class).to(com.codenvy.api.local.LocalMemberDaoImpl.class);
        bind(AccountDao.class).to(LocalAccountDaoImpl.class);
        bind(com.codenvy.api.auth.AuthenticationDao.class).to(com.codenvy.api.local.LocalAuthenticationDaoImpl.class);
        bind(com.codenvy.api.factory.FactoryStore.class).to(com.codenvy.api.local.InMemoryFactoryStore.class);
    }
}
