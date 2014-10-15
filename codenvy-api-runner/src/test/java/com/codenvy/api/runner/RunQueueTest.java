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

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.project.shared.dto.RunnerEnvironment;
import com.codenvy.api.project.shared.dto.RunnersDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.RunnerServerAccessCriteria;
import com.codenvy.api.runner.dto.RunnerServerLocation;
import com.codenvy.api.runner.dto.RunnerServerRegistration;
import com.codenvy.api.runner.dto.RunnerState;
import com.codenvy.api.runner.dto.ServerState;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.ArgumentCaptor;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunQueueTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    private RunQueue runQueue;
    private String wsId   = "my_ws";
    private String wsName = wsId;
    private String pName  = "my_project";
    private String pPath  = "/" + pName;
    private ProjectDescriptor   project;
    private WorkspaceDescriptor workspace;

    @BeforeMethod
    public void before() throws Exception {
        EventService eventService = mock(EventService.class);
        RunnerSelectionStrategy selectionStrategy = new LastInUseRunnerSelectionStrategy();
        runQueue = spy(new RunQueue("http://localhost:8080/api/workspace",
                                    "http://localhost:8080/api/project",
                                    "http://localhost:8080/api/builder",
                                    256,
                                    10,
                                    10,
                                    10,
                                    selectionStrategy,
                                    eventService));
        runQueue.start();
        verify(runQueue, timeout(1000).times(1)).start();

        project = dto(ProjectDescriptor.class).withName(pName).withPath(pPath);
        workspace = dto(WorkspaceDescriptor.class).withId(wsId).withName(wsName).withTemporary(false).withAccountId("my_account");
    }

    @AfterMethod
    public void after() {
        runQueue.stop();
    }

    @Test
    public void testRegisterSlaveRunner() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";

        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));

        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, null);
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");

        verify(runQueue, times(1)).createRemoteRunnerServer(eq(remoteUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(runnerServer));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 1);
        assertEquals(registerRunnerServers.get(0), runnerServer);

        Set<RemoteRunner> runnerList = runQueue.getRunnerList("community", null, null);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        runnerList = runQueue.getRunnerList("community", wsId, null);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        runnerList = runQueue.getRunnerList("community", wsId, pPath);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        assertNull(runQueue.getRunnerList("paid", null, null));
        assertNull(runQueue.getRunnerList("paid", wsId, null));
        assertNull(runQueue.getRunnerList("paid", wsId, pPath));
    }

    @Test
    public void testRegisterSlaveRunnerWithRunnerServerAccessCriteria() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        RunnerServerAccessCriteria accessRules = dto(RunnerServerAccessCriteria.class)
                .withWorkspace(wsId)
                .withProject(pPath)
                .withInfra("paid");
        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, accessRules);
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");

        verify(runnerServer, times(1)).setAssignedWorkspace(wsId);
        verify(runnerServer, times(1)).setAssignedProject(pPath);
        verify(runnerServer, times(1)).setInfra("paid");
        verify(runQueue, times(1)).createRemoteRunnerServer(eq(remoteUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(runnerServer));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 1);
        assertEquals(registerRunnerServers.get(0), runnerServer);

        Set<RemoteRunner> runnerList = runQueue.getRunnerList("paid", wsId, pPath);
        assertNotNull(runnerList);
        assertTrue(runnerList.contains(runner));

        assertNull(runQueue.getRunnerList("paid", wsId, null));
        assertNull(runQueue.getRunnerList("paid", null, null));
        assertNull(runQueue.getRunnerList("community", wsId, pPath));
        assertNull(runQueue.getRunnerList("community", wsId, null));
        assertNull(runQueue.getRunnerList("community", null, null));
    }

    @Test
    public void testUnregisterSlaveRunner() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        runQueue.unregisterRunnerServer(dto(RunnerServerLocation.class).withUrl(runnerServer.getBaseUrl()));

        verify(runQueue, times(1)).doUnregisterRunners(eq(Arrays.asList(runnerServer.getRemoteRunner("java/web"))));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 0);

        assertNull(runQueue.getRunnerList("community", null, null));
    }

    @Test(expectedExceptions = {RunnerException.class},
          expectedExceptionsMessageRegExp = "Runner environment 'system:/java/web/jboss7' is not available.")
    public void testRunWhenReadRunnerConfigurationFromProject_RunnerIsNotAvailable() throws Exception {
        registerDefaultRunnerServer(); // doesn't have what we need
        ServiceContext serviceContext = mock(ServiceContext.class);
        RunOptions runOptions = mock(RunOptions.class);
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/jboss7"));

        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);

        runQueue.run(wsId, pPath, serviceContext, runOptions);
    }

    @Test
    public void testRunWhenReadRunnerConfigurationFromProject() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = mock(ServiceContext.class);
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, null);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000).times(1)).run(runRequestCaptor.capture());

        RunRequest request = runRequestCaptor.getValue();
        assertEquals(request.getMemorySize(), 256); // default mem size
        assertEquals(request.getEnvironmentId(), "tomcat7");
        assertEquals(request.getRunner(), "java/web");
        assertTrue(request.getOptions().isEmpty());
        assertTrue(request.getVariables().isEmpty());
    }

    @Test
    public void testRunWithRunOptions() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = mock(ServiceContext.class);
        RunOptions runOptions = mock(RunOptions.class);
        doReturn("system:/java/web/tomcat7").when(runOptions).getEnvironmentId();
        doReturn(384).when(runOptions).getMemorySize();
        Map<String, String> options = new HashMap<>(4);
        options.put("jpda", "");
        options.put("run", "");
        doReturn(options).when(runOptions).getOptions();
        Map<String, String> envVar = new HashMap<>(4);
        envVar.put("jpda", "");
        envVar.put("run", "");
        doReturn(envVar).when(runOptions).getVariables();

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, runOptions);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000).times(1)).run(runRequestCaptor.capture());

        // check was RunOptions in use.
        verify(runOptions, times(1)).getMemorySize();
        verify(runOptions, times(1)).getEnvironmentId();
        verify(runOptions, times(1)).getOptions();
        verify(runOptions, times(1)).getVariables();

        RunRequest request = runRequestCaptor.getValue();
        assertEquals(request.getMemorySize(), 384);
        assertEquals(request.getEnvironmentId(), "tomcat7");
        assertEquals(request.getRunner(), "java/web");
        assertEquals(request.getOptions(), options);
        assertEquals(request.getVariables(), envVar);
    }

    @Test
    public void testRunWithCustomEnvironment() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("docker");
        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, null);
        RemoteRunner runner = runnerServer.getRemoteRunner(runnerDescriptor.getName());
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = mock(ServiceContext.class);
        String envName = "my_env_1";
        RunOptions runOptions = mock(RunOptions.class);
        doReturn("project://" + envName).when(runOptions).getEnvironmentId();

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        EnvironmentContext codenvyContext = mock(EnvironmentContext.class);
        User user = mock(User.class);
        doReturn(user).when(codenvyContext).getUser();
        String secureToken = "secret";
        doReturn(secureToken).when(user).getToken();

        ItemReference recipe = dto(ItemReference.class).withName("Dockerfile").withType("file");
        String recipeUrl = String.format("http://localhost:8080/api/project/%s/.codenvy/runners/environments/%s", wsId,
                                         envName);
        recipe.getLinks().add(dto(Link.class).withRel(com.codenvy.api.project.server.Constants.LINK_REL_GET_CONTENT).withHref(recipeUrl));
        doReturn(Arrays.asList(recipe)).when(runQueue).getProjectRunnerRecipes(eq(project), eq(envName));

        EnvironmentContext.setCurrent(codenvyContext);

        runQueue.run(wsId, pPath, serviceContext, runOptions);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000).times(1)).run(runRequestCaptor.capture());

        RunRequest request = runRequestCaptor.getValue();
        assertNull(request.getEnvironmentId());
        assertEquals(request.getRunner(), "docker");
        List<String> recipeUrls = request.getRecipeUrls();
        assertEquals(recipeUrls.size(), 1);
        // secure token must be appended
        assertTrue(recipeUrls.contains(recipeUrl + "?token=" + secureToken));
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }

    private RemoteRunnerServer registerDefaultRunnerServer() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        return registerRunnerServer(remoteUrl, runnerDescriptor, null);
    }

    private RemoteRunnerServer registerRunnerServer(String remoteUrl, RunnerDescriptor runnerDescriptor,
                                                    RunnerServerAccessCriteria accessRules) throws Exception {
        RemoteRunnerServer runnerServer = spy(new RemoteRunnerServer(remoteUrl));
        doReturn(Arrays.asList(runnerDescriptor)).when(runnerServer).getRunnerDescriptors();
        RemoteRunner runner = spy(new RemoteRunner(remoteUrl, runnerDescriptor, new ArrayList<Link>()));
        doReturn(runner).when(runnerServer).createRemoteRunner(runnerDescriptor);
        doReturn(runner).when(runnerServer).getRemoteRunner(eq(runnerDescriptor.getName()));
        when(runQueue.createRemoteRunnerServer(remoteUrl)).thenReturn(runnerServer);
        RunnerServerRegistration registration = dto(RunnerServerRegistration.class)
                .withRunnerServerLocation(dto(RunnerServerLocation.class).withUrl(remoteUrl))
                .withRunnerServerAccessCriteria(accessRules);
        runQueue.registerRunnerServer(registration);
        return runnerServer;
    }
}
