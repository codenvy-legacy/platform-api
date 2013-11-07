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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.ApplicationStatus;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes one application process. Typically instance of this class should provide set of links to make possible to get more info about
 * application. Set of links is depends to status of process.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface ApplicationProcessDescriptor {
    long getProcessId();

    ApplicationProcessDescriptor withProcessId(long processId);

    void setProcessId(long processId);

    ApplicationStatus getStatus();

    void setStatus(ApplicationStatus status);

    ApplicationProcessDescriptor withStatus(ApplicationStatus status);

    String getUrl();

    void setUrl(String url);

    ApplicationProcessDescriptor withUrl(String url);

    int getDebugPort();

    void setDebugPort(int port);

    ApplicationProcessDescriptor withDebugPort(int port);

    String getDebugHost();

    void setDebugHost(String host);

    ApplicationProcessDescriptor withDebugHost(String host);

    List<Link> getLinks();

    ApplicationProcessDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);
}
