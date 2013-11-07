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

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class RemoteRunnerFactory extends RemoteServiceDescriptor {

    public RemoteRunnerFactory(String baseUrl) {
        super(baseUrl);
    }

    public RemoteRunner getRemoteRunner(String name) throws IOException, RemoteException, RunnerException {
        for (RunnerDescriptor runnerDescriptor : getAvailableRunners()) {
            if (name.equals(runnerDescriptor.getName())) {
                return new RemoteRunner(baseUrl, runnerDescriptor, getLinks());
            }
        }
        throw new IllegalStateException(String.format("Invalid runner name %s", name));
    }

    public List<RunnerDescriptor> getAvailableRunners() throws IOException, RemoteException, RunnerException {
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_AVAILABLE_RUNNERS);
        if (link == null) {
            throw new RunnerException("Unable get URL for retrieving list of remote runners");
        }
        return HttpJsonHelper.request(RunnerList.class, link).getRunners();
    }
}
