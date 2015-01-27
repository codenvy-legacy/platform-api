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
 * Describe machine
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface StoredMachine {
    String getId();

    void setId(String id);

    StoredMachine withId(String id);

    String getType();

    void setType(String type);

    StoredMachine withType(String type);

    String getProject();

    void setProject(String project);

    StoredMachine withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    StoredMachine withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    StoredMachine withUser(String user);
}
