/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describe machine
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface RuntimeMachineDescription {
    String getId();

    void setId(String id);

    RuntimeMachineDescription withId(String id);

    String getProject();

    void setProject(String project);

    RuntimeMachineDescription withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    RuntimeMachineDescription withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    RuntimeMachineDescription withUser(String user);

    String getState();

    void setState(String state);

    RuntimeMachineDescription withState(String state);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    RuntimeMachineDescription withLinks(List<Link> links);
}
