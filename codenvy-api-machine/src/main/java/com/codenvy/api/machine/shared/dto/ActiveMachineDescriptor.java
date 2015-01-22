package com.codenvy.api.machine.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface ActiveMachineDescriptor {
    String getId();

    void setId(String id);

    ActiveMachineDescriptor withId(String id);

    String getProject();

    void setProject(String project);

    ActiveMachineDescriptor withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    ActiveMachineDescriptor withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    ActiveMachineDescriptor withUser(String user);

    List<String> getSnapshots();

    void setSnapshots(List<String> snapshots);

    ActiveMachineDescriptor withSnapshots(List<String> snapshots);

    String getState();

    void setState(String state);

    ActiveMachineDescriptor withState(String state);
}
