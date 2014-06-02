/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.builder;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.dto.BuilderDescriptor;
import com.codenvy.api.builder.dto.ServerState;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;

import java.io.IOException;
import java.util.List;

/**
 * Factory for RemoteBuilder. See {@link RemoteBuilder} about usage of this class.
 *
 * @author andrew00x
 * @see RemoteBuilder
 */
public class RemoteBuilderServer extends RemoteServiceDescriptor {

    /** Name of IDE workspace this server used for. */
    private String assignedWorkspace;
    /** Name of project inside IDE workspace this server used for. */
    private String assignedProject;

    public RemoteBuilderServer(String baseUrl) {
        super(baseUrl);
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

    public RemoteBuilder getRemoteBuilder(String name) throws BuilderException {
        try {
            for (BuilderDescriptor builderDescriptor : getAvailableBuilders()) {
                if (name.equals(builderDescriptor.getName())) {
                    return new RemoteBuilder(baseUrl, builderDescriptor, getLinks());
                }
            }
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException e) {
            throw new BuilderException(e.getServiceError());
        }
        throw new BuilderException(String.format("Invalid builder name %s", name));
    }

    public RemoteBuilder createRemoteBuilder(BuilderDescriptor descriptor) throws BuilderException {
        try {
            return new RemoteBuilder(baseUrl, descriptor, getLinks());
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public List<BuilderDescriptor> getAvailableBuilders() throws BuilderException {
        try {
            final Link link = getLink(Constants.LINK_REL_AVAILABLE_BUILDERS);
            if (link == null) {
                throw new BuilderException("Unable get URL for retrieving list of remote builders");
            }
            return HttpJsonHelper.requestArray(BuilderDescriptor.class, link);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public ServerState getServerState() throws BuilderException {
        try {
            final Link stateLink = getLink(Constants.LINK_REL_SERVER_STATE);
            if (stateLink == null) {
                throw new BuilderException(String.format("Unable get URL for getting state of a remote server '%s'", baseUrl));
            }
            return HttpJsonHelper.request(ServerState.class, stateLink);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }
}
