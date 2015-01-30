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

import com.codenvy.api.core.rest.shared.dto.Hyperlinks;
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
public interface MachineDescriptor extends Hyperlinks {
    String getId();

    void setId(String id);

    MachineDescriptor withId(String id);

    String getType();

    void setType(String type);

    MachineDescriptor withType(String type);

    List<String> getProjects();

    void setProjects(List<String> projects);

    MachineDescriptor withProjects(List<String> projects);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineDescriptor withWorkspaceId(String workspaceId);

    String getCreatedBy();

    void setCreatedBy(String userId);

    MachineDescriptor withCreatedBy(String userId);

    String getDisplayName();

    void setDisplayName(String displayName);

    MachineDescriptor withDisplayName(String displayName);

    List<SnapshotDescriptor> getSnapshots();

    void setSnapshots(List<SnapshotDescriptor> snapshots);

    MachineDescriptor withSnapshots(List<SnapshotDescriptor> snapshots);

    Machine.State getState();

    void setState(Machine.State state);

    MachineDescriptor withState(Machine.State state);

    @Override
    MachineDescriptor withLinks(List<Link> links);
}
