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
package com.codenvy.api.machine.server.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes machine.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineMetaInfo {
    String getId();

    void setId(String id);

    MachineMetaInfo withId(String id);

    String getType();

    void setType(String type);

    MachineMetaInfo withType(String type);

    List<String> getProjects();

    void setProjects(List<String> projects);

    MachineMetaInfo withProjects(List<String> projects);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineMetaInfo withWorkspaceId(String workspaceId);

    String getUserId();

    void setUserId(String userId);

    MachineMetaInfo withUserId(String userId);

    String getDisplayName();

    void setDisplayName(String displayName);

    MachineMetaInfo withDisplayName(String displayName);
}
