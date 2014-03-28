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

/**
 * Represents remote {@code Runner}.
 * <p/>
 * Usage:
 * <pre>
 *     String baseUrl = ...
 *     String runnerName = ...
 *     RemoteRunnerFactory factory = new RemoteRunnerFactory(baseUrl);
 *     RemoteRunner runner = factory.getRemoteRunner(runnerName);
 *     RunRequest request = ...
 *     RemoteRunnerProcess remote = runner.run(request);
 *     // do something with RemoteRunnerProcess, e.g. check status
 *     System.out.println(remote.getApplicationProcessDescriptor());
 * </pre>
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.RemoteRunnerFactory
 */
public class RemoteRunner {
    private final String           baseUrl;
    private final String           name;
    private final RunnerDescriptor descriptor;
    private final int              hashCode;
    private final List<Link>       links;

    private volatile long lastUsage = -1;

    /* Package visibility, not expected to be created by api users. They should use RemoteRunnerFactory to get an instance of RemoteRunner. */
    RemoteRunner(String baseUrl, RunnerDescriptor runnerDescriptor, List<Link> links) {
        this.baseUrl = baseUrl;
        this.name = runnerDescriptor.getName();
        this.descriptor = DtoFactory.getInstance().clone(runnerDescriptor);
        this.links = new ArrayList<>(links);
        int hashCode = 7;
        hashCode = hashCode * 31 + baseUrl.hashCode();
        hashCode = hashCode * 31 + this.name.hashCode();
        this.hashCode = hashCode;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get name of this runner.
     *
     * @return name of this runner
     * @see com.codenvy.api.runner.internal.Runner#getName()
     */
    public final String getName() {
        return name;
    }

    /**
     * Get last time of usage of this runner.
     *
     * @return last time of usage of this runner
     */
    public long getLastUsageTime() {
        return lastUsage;
    }

    public RunnerDescriptor getDescriptor() {
        return DtoFactory.getInstance().clone(descriptor);
    }

    /**
     * Stats new application process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws RunnerException
     *         if an error occurs
     */
    public RemoteRunnerProcess run(RunRequest request) throws RunnerException {
        final Link link = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_RUN);
        if (link == null) {
            throw new RunnerException("Unable get URL for starting application's process");
        }
        final ApplicationProcessDescriptor process;
        try {
            process = HttpJsonHelper.request(ApplicationProcessDescriptor.class, link, request);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (RemoteException e) {
            throw new RunnerException(e.getServiceError());
        }
        lastUsage = System.currentTimeMillis();
        return new RemoteRunnerProcess(baseUrl, getName(), process.getProcessId());
    }

    /**
     * Get current state of remote runner.
     *
     * @return current state of remote runner.
     * @throws RunnerException
     *         if an error occurs
     */
    public RunnerState getRemoteRunnerState() throws RunnerException {
        final Link stateLink = getLink(com.codenvy.api.runner.internal.Constants.LINK_REL_RUNNER_STATE);
        if (stateLink == null) {
            throw new RunnerException(String.format("Unable get URL for getting state of a remote runner '%s' at '%s'",
                                                    descriptor.getName(), baseUrl));
        }
        try {
            return HttpJsonHelper.request(RunnerState.class, stateLink, Pair.of("runner", descriptor.getName()));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (RemoteException e) {
            throw new RunnerException(e.getServiceError());
        }
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
