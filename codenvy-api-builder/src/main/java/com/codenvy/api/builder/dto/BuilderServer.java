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

import com.codenvy.api.core.rest.shared.dto.Hyperlinks;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface BuilderServer extends Hyperlinks {
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

    BuilderServer withLinks(List<Link> links);
}
