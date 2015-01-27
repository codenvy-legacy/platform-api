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
import com.codenvy.api.machine.server.Machine;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describe machine
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineDescriptor {
    String getId();

    void setId(String id);

    MachineDescriptor withId(String id);

    String getProject();

    void setProject(String project);

    MachineDescriptor withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineDescriptor withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    MachineDescriptor withUser(String user);

    List<String> getSnapshots();

    void setSnapshots(List<String> snapshots);

    MachineDescriptor withSnapshots(List<String> snapshots);

    Machine.State getState();

    void setState(Machine.State state);

    MachineDescriptor withState(Machine.State state);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    MachineDescriptor withLinks(List<Link> links);
}
