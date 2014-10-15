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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.ServerState;
import com.codenvy.api.runner.internal.Constants;

import java.io.IOException;
import java.util.List;

/**
 * Factory for RemoteRunner. See {@link RemoteRunner} about usage of this class.
 *
 * @author andrew00x
 */
public class RemoteRunnerServer extends RemoteServiceDescriptor {
    /** Name of IDE workspace this server is used for. */
    private String assignedWorkspace;
    /** Name of project inside IDE workspace this server is used for. */
    private String assignedProject;

    private String infra = "community";

    public RemoteRunnerServer(String baseUrl) {
        super(baseUrl);
    }

    public String getInfra() {
        return infra;
    }

    public void setInfra(String infra) {
        this.infra = infra;
    }

    public String getAssignedWorkspace() {
        return assignedWorkspace;
    }

    public void setAssignedWorkspace(String assignedWorkspace) {
        this.assignedWorkspace = assignedWorkspace;
    }

    public String getAssignedProject() {
        return assignedProject;
    }

    public void setAssignedProject(String assignedProject) {
        this.assignedProject = assignedProject;
    }

    public boolean isDedicated() {
        return assignedWorkspace != null;
    }

    public RemoteRunner getRemoteRunner(String name) throws RunnerException {
        try {
            for (RunnerDescriptor runnerDescriptor : getRunnerDescriptors()) {
                if (name.equals(runnerDescriptor.getName())) {
                    return new RemoteRunner(baseUrl, runnerDescriptor, getLinks());
                }
            }
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException e) {
            throw new RunnerException(e.getServiceError());
        }
        throw new RunnerException(String.format("Invalid runner name %s", name));
    }

    RemoteRunner createRemoteRunner(RunnerDescriptor descriptor) throws RunnerException {
        try {
            return new RemoteRunner(baseUrl, descriptor, getLinks());
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public List<RunnerDescriptor> getRunnerDescriptors() throws RunnerException {
        try {
            final Link link = getLink(Constants.LINK_REL_AVAILABLE_RUNNERS);
            if (link == null) {
                throw new RunnerException("Unable get URL for retrieving list of remote runners");
            }
            return HttpJsonHelper.requestArray(RunnerDescriptor.class, link);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public ServerState getServerState() throws RunnerException {
        try {
            final Link stateLink = getLink(Constants.LINK_REL_SERVER_STATE);
            if (stateLink == null) {
                throw new RunnerException(String.format("Unable get URL for getting state of a remote server '%s'", baseUrl));
            }
            return HttpJsonHelper.request(ServerState.class, stateLink);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }
}
