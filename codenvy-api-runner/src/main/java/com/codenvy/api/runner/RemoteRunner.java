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
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.dto.RunnerState;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class RemoteRunner {
    private final String     baseUrl;
    private final String     description;
    private final String     name;
    private final List<Link> links;
    private final int        hashCode;

    private volatile long lastUsage = -1;

    RemoteRunner(String baseUrl, RunnerDescriptor runnerDescriptor, List<Link> links) {
        this.baseUrl = baseUrl;
        this.name = runnerDescriptor.getName();
        this.description = runnerDescriptor.getDescription();
        this.links = new ArrayList<>(links);
        int hashCode = 7;
        hashCode = hashCode * 31 + baseUrl.hashCode();
        hashCode = hashCode * 31 + name.hashCode();
        this.hashCode = hashCode;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    /** @see com.codenvy.api.runner.internal.Runner#getName() */
    public final String getName() {
        return name;
    }

    /** @see com.codenvy.api.runner.internal.Runner#getDescription() */
    public final String getDescription() {
        return description;
    }

    public long getLastUsageTime() {
        return lastUsage;
    }

    public RemoteRunnerProcess run(RunRequest request) throws IOException, RemoteException, RunnerException {
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_RUN);
        if (link == null) {
            throw new RunnerException("Unable get URL for starting application's process");
        }
        final ApplicationProcessDescriptor process = HttpJsonHelper.request(ApplicationProcessDescriptor.class, link, request);
        lastUsage = System.currentTimeMillis();
        return new RemoteRunnerProcess(baseUrl, request.getRunner(), process.getProcessId());
    }

    public RunnerState getRemoteRunnerState() throws IOException, RemoteException, RunnerException {
        final Link stateLink = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_RUNNER_STATE);
        if (stateLink == null) {
            throw new RunnerException(String.format("Unable get URL for getting state of a remote runner '%s'", name));
        }
        return HttpJsonHelper.request(RunnerState.class, stateLink, Pair.of("runner", name));
    }

    private Link getLink(String rel) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                // create copy of link since we pass it outside from this class
                return DtoFactory.getInstance().clone(link);
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteRunner)) {
            return false;
        }
        RemoteRunner other = (RemoteRunner)o;
        return baseUrl.equals(other.baseUrl) && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }
}
