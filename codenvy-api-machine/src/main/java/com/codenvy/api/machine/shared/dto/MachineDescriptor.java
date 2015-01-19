package com.codenvy.api.machine.shared.dto;

import com.codenvy.dto.shared.DTO;

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
}
