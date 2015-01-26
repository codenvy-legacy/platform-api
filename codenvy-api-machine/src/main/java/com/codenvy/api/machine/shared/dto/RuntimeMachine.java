package com.codenvy.api.machine.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface RuntimeMachine {
    String getId();

    void setId(String id);

    RuntimeMachine withId(String id);

    String getProject();

    void setProject(String project);

    RuntimeMachine withProject(String project);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    RuntimeMachine withWorkspaceId(String workspaceId);

    String getUser();

    void setUser(String user);

    RuntimeMachine withUser(String user);

    String getState();

    void setState(String state);

    RuntimeMachine withState(String state);
}
