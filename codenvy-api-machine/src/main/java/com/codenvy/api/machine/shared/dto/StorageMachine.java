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

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describe machine
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface StorageMachine {
    String getId();

    void setId(String id);

    StorageMachine withId(String id);

    String getProject();

    void setProject(String project);

    StorageMachine withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    StorageMachine withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    StorageMachine withUser(String user);

    List<String> getSnapshots();

    void setSnapshots(List<String> snapshots);

    StorageMachine withSnapshots(List<String> snapshots);
}
