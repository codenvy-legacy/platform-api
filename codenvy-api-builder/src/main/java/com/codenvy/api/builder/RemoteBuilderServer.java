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
package com.codenvy.api.builder;

import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderList;
import com.codenvy.api.builder.internal.dto.ServerState;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
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
        } catch (RemoteException e) {
            throw new BuilderException(e.getServiceError());
        }
        throw new BuilderException(String.format("Invalid builder name %s", name));
    }

    public RemoteBuilder createRemoteBuilder(BuilderDescriptor descriptor) throws BuilderException {
        try {
            return new RemoteBuilder(baseUrl, descriptor, getLinks());
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (RemoteException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public List<BuilderDescriptor> getAvailableBuilders() throws BuilderException {
        try {
            final Link link = getLink(Constants.LINK_REL_AVAILABLE_BUILDERS);
            if (link == null) {
                throw new BuilderException("Unable get URL for retrieving list of remote builders");
            }
            return HttpJsonHelper.request(BuilderList.class, link).getBuilders();
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (RemoteException e) {
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
        } catch (RemoteException e) {
            throw new BuilderException(e.getServiceError());
        }
    }
}
