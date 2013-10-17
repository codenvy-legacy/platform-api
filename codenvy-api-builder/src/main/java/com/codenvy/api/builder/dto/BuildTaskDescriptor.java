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
package com.codenvy.api.builder.dto;

import com.codenvy.api.builder.internal.BuildStatus;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes one build process. Typically instance of this class should provide set of links to make possible to get more info about build.
 * Set of links is depends to status of build. E.g. if build successful then one of the links provides location for download build result,
 * get build report, etc.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface BuildTaskDescriptor {
    BuildStatus getStatus();

    BuildTaskDescriptor withStatus(BuildStatus status);

    void setStatus(BuildStatus status);

    long getStartTime();

    BuildTaskDescriptor withStartTime(long startTime);

    void setStartTime(long startTime);

    List<Link> getLinks();

    BuildTaskDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);

    long getTaskId();

    BuildTaskDescriptor withTaskId(long taskId);

    void setTaskId(long taskId);
}
