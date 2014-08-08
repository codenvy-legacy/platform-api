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

    long getCreationTime();

    ApplicationProcessDescriptor withCreationTime(long creationTime);

    void setCreationTime(long creationTime);

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

    /** Name of workspace which the project is belong. */
    String getWorkspace();

    void setWorkspace(String workspace);

    ApplicationProcessDescriptor withWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    void setProject(String project);

    ApplicationProcessDescriptor withProject(String project);

    /** The name of user who ran application. */
    String getUserName();

    ApplicationProcessDescriptor withUserName(String userName);

    void setUserName(String userName);

    /**
     * The URL of server where application is running. This information is accessible only over RunnerAdminServer and is not available for
     * regular users.
     */
    String getServerUrl();

    void setServerUrl(String server);

    ApplicationProcessDescriptor withServerUrl(String server);
}
