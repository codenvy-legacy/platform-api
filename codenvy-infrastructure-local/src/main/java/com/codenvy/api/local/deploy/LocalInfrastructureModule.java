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

import com.codenvy.api.local.LocalMemberDaoImpl;
import com.codenvy.api.local.LocalOrganizationDaoImpl;
import com.codenvy.api.local.LocalProfileDaoImpl;
import com.codenvy.api.local.LocalUserDaoImpl;
import com.codenvy.api.local.LocalWorkspaceDaoImpl;
import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;

@DynaModule
public class LocalInfrastructureModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UserDao.class).toInstance(new LocalUserDaoImpl());
        bind(WorkspaceDao.class).toInstance(new LocalWorkspaceDaoImpl());
        bind(UserProfileDao.class).to(LocalProfileDaoImpl.class);
        bind(MemberDao.class).toInstance(new LocalMemberDaoImpl());
        bind(OrganizationDao.class).toInstance(new LocalOrganizationDaoImpl());
    }
}
