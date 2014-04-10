/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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

import com.codenvy.api.builder.internal.dto.ServerState;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface BuilderServer {
    String getUrl();

    void setUrl(String url);

    BuilderServer withUrl(String url);

    String getDescription();

    void setDescription(String description);

    BuilderServer withDescription(String description);

    boolean isDedicated();

    void setDedicated(boolean dedicated);

    BuilderServer withDedicated(boolean dedicated);

    String getWorkspace();

    BuilderServer withWorkspace(String workspace);

    void setWorkspace(String workspace);

    String getProject();

    BuilderServer withProject(String project);

    void setProject(String project);

    ServerState getServerState();

    BuilderServer withServerState(ServerState serverState);

    void setServerState(ServerState serverState);

    List<Link> getLinks();

    BuilderServer withLinks(List<Link> links);

    void setLinks(List<Link> links);
}
