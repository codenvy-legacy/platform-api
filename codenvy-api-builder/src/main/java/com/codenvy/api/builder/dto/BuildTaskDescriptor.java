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
package com.codenvy.api.builder.dto;

import com.codenvy.api.builder.BuildStatus;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes one build process. Typically instance of this class should provide set of links to make possible to get more info about build.
 * Set of links is depends to status of build. E.g. if build successful then one of the links provides location for download build result,
 * get build report, etc.
 *
 * @author andrew00x
 */
@DTO
public interface BuildTaskDescriptor {
    BuildStatus getStatus();

    BuildTaskDescriptor withStatus(BuildStatus status);

    void setStatus(BuildStatus status);

    long getCreationTime();

    BuildTaskDescriptor withCreationTime(long creationTime);

    void setCreationTime(long creationTime);

    long getStartTime();

    BuildTaskDescriptor withStartTime(long startTime);

    void setStartTime(long startTime);

    long getEndTime();

    BuildTaskDescriptor withEndTime(long endTime);

    void setEndTime(long endTime);

    List<Link> getLinks();

    BuildTaskDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);

    long getTaskId();

    BuildTaskDescriptor withTaskId(long taskId);

    void setTaskId(long taskId);

    String getCommandLine();

    void setCommandLine(String cmd);

    BuildTaskDescriptor withCommandLine(String cmd);

    List<BuilderMetric> getBuildStats();

    BuildTaskDescriptor withBuildStats(List<BuilderMetric> stats);

    void setBuildStats(List<BuilderMetric> stats);
}
