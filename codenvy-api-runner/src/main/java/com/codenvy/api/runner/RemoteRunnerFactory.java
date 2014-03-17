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
package com.codenvy.api.runner;

import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.dto.RunnerList;

import java.io.IOException;
import java.util.List;

/**
 * Factory for RemoteRunner. See {@link RemoteRunner} about usage of this class.
 *
 * @author andrew00x
 */
public class RemoteRunnerFactory extends RemoteServiceDescriptor {

    public RemoteRunnerFactory(String baseUrl) {
        super(baseUrl);
    }

    public RemoteRunner getRemoteRunner(String name) throws RunnerException {
        try {
            for (RunnerDescriptor runnerDescriptor : getAvailableRunners()) {
                if (name.equals(runnerDescriptor.getName())) {
                    return new RemoteRunner(baseUrl, runnerDescriptor, getLinks());
                }
            }
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (RemoteException e) {
            throw new RunnerException(e.getServiceError());
        }
        throw new RunnerException(String.format("Invalid runner name %s", name));
    }

    public RemoteRunner createRemoteRunner(RunnerDescriptor descriptor) throws RunnerException {
        try {
            return new RemoteRunner(baseUrl, descriptor, getLinks());
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (RemoteException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public List<RunnerDescriptor> getAvailableRunners() throws RunnerException {
        try {
            final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_AVAILABLE_RUNNERS);
            if (link == null) {
                throw new RunnerException("Unable get URL for retrieving list of remote runners");
            }
            return HttpJsonHelper.request(RunnerList.class, link).getRunners();
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (RemoteException e) {
            throw new RunnerException(e.getServiceError());
        }
    }
}
