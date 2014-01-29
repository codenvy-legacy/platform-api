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

import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;

import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;

/**
 * Test for Workspace service
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    @Mock
    private WorkspaceDao workspaceDao;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

    @InjectMocks
    private WorkspaceService workspaceService;
}
