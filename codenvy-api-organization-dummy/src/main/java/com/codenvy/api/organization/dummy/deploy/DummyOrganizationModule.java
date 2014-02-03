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
package com.codenvy.api.organization.dummy.deploy;

import com.codenvy.api.organization.dummy.DummyMemberDaoImpl;
import com.codenvy.api.organization.dummy.DummyProfileDaoImpl;
import com.codenvy.api.organization.dummy.DummyUserDaoImpl;
import com.codenvy.api.organization.dummy.DummyWorkspaceDaoImpl;
import com.codenvy.api.organization.dummy.server.DummyProjectService;
import com.codenvy.api.user.server.UserProfileService;
import com.codenvy.api.user.server.UserService;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.workspace.server.WorkspaceService;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;

@DynaModule
public class DummyOrganizationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserDao.class).toInstance(new DummyUserDaoImpl());
        bind(WorkspaceDao.class).toInstance(new DummyWorkspaceDaoImpl());
        bind(UserProfileDao.class).toInstance(new DummyProfileDaoImpl());
        bind(MemberDao.class).toInstance(new DummyMemberDaoImpl());

        bind(WorkspaceService.class);
        bind(UserService.class);
        bind(UserProfileService.class);

        bind(DummyProjectService.class);
    }
}
