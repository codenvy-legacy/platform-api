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

    String getRecipe();

    void setRecipe(String recipe);

    CreateMachineRequest withRecipe(String recipe);

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
