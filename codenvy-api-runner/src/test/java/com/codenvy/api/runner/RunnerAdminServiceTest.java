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

import com.codenvy.api.core.rest.shared.dto.ServiceDescriptor;
import com.codenvy.api.project.shared.dto.RunnerEnvironment;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentLeaf;
import com.codenvy.api.project.shared.dto.RunnerEnvironmentTree;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.RunnerServer;
import com.codenvy.dto.server.DtoFactory;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunnerAdminServiceTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    @Mock
    private RunQueue      runQueue;
    @InjectMocks
    private RunnerAdminService service;

    @BeforeMethod
    public void beforeMethod() {
        doNothing().when(runQueue).checkStarted();
    }

    @AfterMethod
    public void afterMethod() {
    }

    @Test
    public void testGetServers() throws Exception {
        String serverUrl = "http://localhost:8080/server1";
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(serverUrl).when(server1).getBaseUrl();
        doReturn(dto(ServiceDescriptor.class)).when(server1).getServiceDescriptor();
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();
        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        List<RunnerServer> _servers = service.getRegisteredServers();
        assertEquals(_servers.size(), 1);
        RunnerServer _server = _servers.get(0);
        assertEquals(_server.getUrl(), serverUrl);
    }

    @Test
    public void testGetServersIfOneServerUnavailable() throws Exception {
        String serverUrl = "http://localhost:8080/server1";
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(serverUrl).when(server1).getBaseUrl();
        doReturn(dto(ServiceDescriptor.class)).when(server1).getServiceDescriptor();
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        doThrow(new RunnerException("Connection refused")).when(server2).getRunnerDescriptors();
        doThrow(new RunnerException("Connection refused")).when(server2).getServiceDescriptor();
        servers.add(server2);

        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        List<RunnerServer> _servers = service.getRegisteredServers();
        assertEquals(_servers.size(), 1);
        RunnerServer _server = _servers.get(0);
        assertEquals(_server.getUrl(), serverUrl);
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}
