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
package com.codenvy.api.runner.dto;

import com.codenvy.api.builder.dto.BuilderMetric;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.ApplicationStatus;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes one application process. Typically instance of this class should provide set of links to make possible to get more info about
 * application. Set of links is depends to status of process.
 *
 * @author andrew00x
 */
@DTO
public interface ApplicationProcessDescriptor {
    long getProcessId();

    ApplicationProcessDescriptor withProcessId(long processId);

    void setProcessId(long processId);

    ApplicationStatus getStatus();

    void setStatus(ApplicationStatus status);

    ApplicationProcessDescriptor withStatus(ApplicationStatus status);

    long getStartTime();

    ApplicationProcessDescriptor withStartTime(long startTime);

    void setStartTime(long startTime);

    long getStopTime();

    ApplicationProcessDescriptor withStopTime(long stopTime);

    void setStopTime(long stopTime);

    int getDebugPort();

    void setDebugPort(int port);

    ApplicationProcessDescriptor withDebugPort(int port);

    String getDebugHost();

    void setDebugHost(String host);

    ApplicationProcessDescriptor withDebugHost(String host);

    List<RunnerMetric> getRunStats();

    ApplicationProcessDescriptor withRunStats(List<RunnerMetric> stats);

    void setRunStats(List<RunnerMetric> stats);

    List<BuilderMetric> getBuildStats();

    ApplicationProcessDescriptor withBuildStats(List<BuilderMetric> stats);

    void setBuildStats(List<BuilderMetric> stats);

    List<Link> getLinks();

    ApplicationProcessDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);
}
