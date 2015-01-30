package com.codenvy.api.machine.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface CreateMachineRequest {
    String getType();

    void setType(String type);

    CreateMachineRequest withType(String type);

    String getReceipt();

    void setReceipt(String receipt);

    CreateMachineRequest withReceipt(String receipt);

    String getWorkspace();

    void setWorkspace(String workspace);

    CreateMachineRequest withWorkspace(String workspace);

    String getDisplayName();

    void setDisplayName(String displayName);

    CreateMachineRequest withDisplayName(String displayName);

    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CreateMachineRequest withOutputChannel(String outputChannel);
}
