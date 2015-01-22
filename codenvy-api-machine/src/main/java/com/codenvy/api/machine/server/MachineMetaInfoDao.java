package com.codenvy.api.machine.server;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
public interface MachineMetaInfoDao {
    MachineDescriptor getById(String id) throws NotFoundException, ServerException;

    void create(MachineDescriptor machineDescriptor) throws ServerException;

    void remove(String id) throws NotFoundException, ServerException;

    List<MachineDescriptor> findByUser(String userId) throws ServerException;

    List<MachineDescriptor> findByUserWorkspaceProject(String userId, String wsId, String projectName) throws ServerException;

    List<MachineDescriptor> findByWorkspaceProject(String wsId, String projectName) throws ServerException;
}
