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

import sun.launcher.resources.launcher;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.assured.EverrestJetty;
import org.everrest.core.RequestHandler;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ApplicationPublisher;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.RequestDispatcher;
import org.everrest.core.impl.RequestHandlerImpl;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for Workspace service
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    private static final String WS_ID       = "workspace12asd123asdasd1f";
    private static final String WS_NAME     = "ws1";

    @Mock
    private UriInfo uriInfo;

    @Mock
    private WorkspaceDao workspaceDao;

    @Mock
    private UserDao userDao;

    @Mock
    private MemberDao memberDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UriBuilder baseUriBuilder;

    @Mock
    private UriBuilder serviceUriBuilder;

    @Mock
    private ServiceContext serviceContext;

    @InjectMocks
    private WorkspaceService workspaceService;

    private Workspace workspace = DtoFactory.getInstance().createDto(Workspace.class)
                                            .withId(WS_ID)
                                            .withName(WS_NAME);

    @BeforeMethod
    public void beforeTest() throws WorkspaceException, NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void testCreateWorkspace() throws Exception {
        System.out.println();
//        workspaceService.getServiceContext();
//        when(securityContext.isUserInRole("user")).thenReturn(true);
//        workspaceService.create(securityContext, workspace);
//        verify(workspaceDao, times(1)).create(workspace);
    }


}
