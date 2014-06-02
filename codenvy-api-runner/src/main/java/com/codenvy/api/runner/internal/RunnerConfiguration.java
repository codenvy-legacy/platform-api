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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner configuration for particular run process.
 *
 * @author andrew00x
 */
public class RunnerConfiguration {
    private final int        memory;
    private final List<Link> links;
    private final RunRequest request;

    private String  debugHost;
    private int     debugPort;
    private boolean debugSuspend;

    public RunnerConfiguration(int memory, RunRequest request) {
        this.memory = memory;
        this.request = request;
        this.links = new ArrayList<>(2);
        this.debugPort = -1;
    }

    public RunnerConfiguration(int memory, RunRequest request, List<Link> links) {
        this.memory = memory;
        this.request = request;
        this.links = new ArrayList<>(links);
        this.debugPort = -1;
    }

    public int getMemory() {
        return memory;
    }

    /**
     * Get application links. List of links is modifiable.
     *
     * @return application links
     */
    public List<Link> getLinks() {
        return links;
    }

    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(request.getOptions());
    }

    public RunRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    public String getDebugHost() {
        return debugHost;
    }

    public void setDebugHost(String debugHost) {
        this.debugHost = debugHost;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public boolean isDebugSuspend() {
        return debugSuspend;
    }

    public void setDebugSuspend(boolean debugSuspend) {
        this.debugSuspend = debugSuspend;
    }

    @Override
    public String toString() {
        return "RunnerConfiguration{" +
               "memory=" + memory +
               ", links=" + links +
               ", request=" + request +
               ", debugHost='" + debugHost + '\'' +
               ", debugPort=" + debugPort +
               ", debugSuspend=" + debugSuspend +
               '}';
    }
}
