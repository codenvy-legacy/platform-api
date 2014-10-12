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
package com.codenvy.api.runner;

import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.RunnerEnvironment;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentTree;
import com.codenvy.api.project.shared.dto.RunnersDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.RunnerServerAccessCriteria;
import com.codenvy.api.runner.dto.RunnerServerLocation;
import com.codenvy.api.runner.dto.RunnerServerRegistration;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.commons.lang.Pair;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunQueueTest {
    DtoFactory dtoFactory = DtoFactory.getInstance();
    @Mock
    private RunQueue                          runQueue;
    @Mock
    private HttpJsonHelper.HttpJsonHelperImpl httpJsonHelper;

    private String wsId   = "my_ws";
    private String wsName = wsId;
    private String pName  = "my_project";
    private ProjectDescriptor   project;
    private WorkspaceDescriptor workspace;

    @BeforeMethod
    public void before() throws Exception {
        Field field = HttpJsonHelper.class.getDeclaredField("httpJsonHelperImpl");
        field.setAccessible(true);
        field.set(null, httpJsonHelper);

        wsId = "my_ws";
        wsName = wsId;
        pName = "my_project";
        project = dto(ProjectDescriptor.class)
                .withName(pName)
                .withPath("/" + pName);
        workspace = dto(WorkspaceDescriptor.class)
                .withId(wsId).withName(wsName)
                .withTemporary(false)
                .withAccountId("my_account");
    }

    @Test
    public void testRegisterSlaveRunner() throws Exception {
        String serverUrl = "http://localhost:8080/api/runner/internal";

        RemoteRunnerServer slaveRunnerServer = mock(RemoteRunnerServer.class);
        when(slaveRunnerServer.getLink(Constants.LINK_REL_AVAILABLE_RUNNERS)).thenReturn(dto(Link.class).withHref("").withMethod(""));
        when(runQueue.createRemoteRunnerServer(serverUrl)).thenReturn(slaveRunnerServer);

        final RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class)
                .withName("test_name").withDescription("test description")
                .withEnvironments(dto(RunnerEnvironmentTree.class).withDisplayName("java-webapp"));
        runnerDescriptor.getEnvironments().getEnvironments().add(
                dto(RunnerEnvironment.class).withDisplayName("tomcat7").withId("system:/java-webapp/tomcat7"));
        when(httpJsonHelper.request(eq(RunnerDescriptor.class), anyString(), anyString(), any(),
                                    (Pair<String, ?>[])any(Pair[].class))).thenReturn(runnerDescriptor);
        when(runQueue.doRegisterRunnerServer(slaveRunnerServer)).thenReturn(true);

        final RunnerServerRegistration registration = dto(RunnerServerRegistration.class).withRunnerServerLocation(
                dto(RunnerServerLocation.class).withUrl(serverUrl));
        when(runQueue.registerRunnerServer(registration)).thenCallRealMethod();
        runQueue.registerRunnerServer(registration);

        verify(runQueue, times(1)).createRemoteRunnerServer(eq(serverUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(slaveRunnerServer));
    }

    @Test
    public void testRegisterSlaveRunnerWithRunnerServerAccessCriteria() throws Exception {
        String serverUrl = "http://localhost:8080/api/runner/internal";

        RemoteRunnerServer slaveRunnerServer = mock(RemoteRunnerServer.class);
        when(slaveRunnerServer.getLink(Constants.LINK_REL_AVAILABLE_RUNNERS)).thenReturn(dto(Link.class).withHref("").withMethod(""));
        when(runQueue.createRemoteRunnerServer(serverUrl)).thenReturn(slaveRunnerServer);

        final RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class)
                .withName("test_name").withDescription("test description")
                .withEnvironments(dto(RunnerEnvironmentTree.class).withDisplayName("java-webapp"));
        runnerDescriptor.getEnvironments().getEnvironments().add(
                dto(RunnerEnvironment.class).withDisplayName("tomcat7").withId("system:/java-webapp/tomcat7"));
        when(httpJsonHelper.request(eq(RunnerDescriptor.class), anyString(), anyString(), any(),
                                    (Pair<String, ?>[])any(Pair[].class))).thenReturn(runnerDescriptor);
        when(runQueue.doRegisterRunnerServer(slaveRunnerServer)).thenReturn(true);

        final RunnerServerRegistration registration = dto(RunnerServerRegistration.class)
                .withRunnerServerLocation(dto(RunnerServerLocation.class).withUrl(serverUrl))
                .withRunnerServerAccessCriteria(dto(RunnerServerAccessCriteria.class)
                                                        .withWorkspace("my_ws")
                                                        .withProject("my_project")
                                                        .withInfra("paid"));
        when(runQueue.registerRunnerServer(registration)).thenCallRealMethod();
        runQueue.registerRunnerServer(registration);

        verify(slaveRunnerServer, times(1)).setAssignedWorkspace("my_ws");
        verify(slaveRunnerServer, times(1)).setAssignedProject("my_project");
        verify(slaveRunnerServer, times(1)).setInfra("paid");
        verify(runQueue, times(1)).createRemoteRunnerServer(eq(serverUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(slaveRunnerServer));
    }

    @Test(expectedExceptions = {RunnerException.class},
          expectedExceptionsMessageRegExp = "Runner 'system:/java/web/tomcat7' is not available. ")
    public void testRunWhenReadRunnerConfigurationFromProject_RunnerIsNotAvailable() throws Exception {
        ServiceContext serviceContext = mock(ServiceContext.class);
        RunOptions runOptions = mock(RunOptions.class);
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        when(runQueue.run(anyString(), anyString(), any(ServiceContext.class), any(RunOptions.class))).thenCallRealMethod();
        when(runQueue.getProjectDescriptor(wsId, pName, serviceContext)).thenReturn(project);
        when(runQueue.getWorkspaceDescriptor(wsId, serviceContext)).thenReturn(workspace);

        try {
            runQueue.run(wsId, pName, serviceContext, runOptions);
        } catch (RunnerException e) {
            ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
            verify(runQueue, times(1)).hasRunner(eq("community"), runRequestCaptor.capture());
            RunRequest runRequest = runRequestCaptor.getValue();
            assertNotNull(runRequest);
            assertEquals(runRequest.getRunner(), "java/web");
            assertEquals(runRequest.getEnvironmentId(), "system:/java/web/tomcat7");
            throw e;
        }
    }

    @Test(enabled = false)
    public void testRunWhenReadRunnerConfigurationFromProject() throws Exception {
        ServiceContext serviceContext = mock(ServiceContext.class);
        RunOptions runOptions = mock(RunOptions.class);
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        when(runQueue.run(anyString(), anyString(), any(ServiceContext.class), any(RunOptions.class))).thenCallRealMethod();
        when(runQueue.getProjectDescriptor(wsId, pName, serviceContext)).thenReturn(project);
        when(runQueue.getWorkspaceDescriptor(wsId, serviceContext)).thenReturn(workspace);
        when(runQueue.hasRunner(eq("community"), any(RunRequest.class))).thenReturn(true);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pName, serviceContext, runOptions);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        RunRequest runRequest = runRequestCaptor.getValue();
        assertNotNull(runRequest);
        assertEquals(runRequest.getRunner(), "java/web");
        assertEquals(runRequest.getEnvironmentId(), "system:/java/web/tomcat7");
        verify(runQueue, times(1)).hasRunner(eq("community"), runRequestCaptor.capture());
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}
