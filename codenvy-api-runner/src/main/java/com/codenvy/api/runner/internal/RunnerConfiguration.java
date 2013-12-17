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
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner configuration for particular run process.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class RunnerConfiguration {
    private final int        memory;
    /** Application port. */
    private final int        port;
    private final int        debugPort;
    private final List<Link> links;
    private final RunRequest request;

    public RunnerConfiguration(int memory, int port, int debugPort, RunRequest request) {
        this.memory = memory;
        this.port = port;
        this.debugPort = debugPort;
        this.request = request;
        this.links = null;
    }

    public RunnerConfiguration(int memory, int port, int debugPort, List<Link> links, RunRequest request) {
        this.memory = memory;
        this.port = port;
        this.debugPort = debugPort;
        this.request = request;
        this.links = new ArrayList<>(links);
    }

    public int getMemory() {
        return memory;
    }

    public int getPort() {
        return port;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public List<Link> getLinks() {
        if (links == null) {
            return new ArrayList<>(0);
        }
        return links;
    }

    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(request.getOptions());
    }

    public RunRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    @Override
    public String toString() {
        return "RunnerConfiguration{" +
               "memory=" + memory +
               ", port=" + port +
               ", debugPort=" + debugPort +
               ", request=" + request +
               '}';
    }
}
