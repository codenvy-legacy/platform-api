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
package com.codenvy.api.machine.server;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.server.dto.StoredMachine;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final MachineDao                     machineDao;
    private final MachineBuilderFactoryRegistry machineBuilderFactoryRegistry;

    @Inject
    public MachineRegistry(MachineDao machineDao, MachineBuilderFactoryRegistry machineBuilderFactoryRegistry) {
        this.machineDao = machineDao;
        this.machineBuilderFactoryRegistry = machineBuilderFactoryRegistry;
    }

    public void addMachine(StoredMachine persistMachine) throws ServerException {
        machineDao.add(persistMachine);
    }

    public List<Machine> getMachines(String workspaceId, String project, String user) throws ServerException {
        List<Machine> result = new LinkedList<>();
        final List<StoredMachine> machines = machineDao.findByUserWorkspaceProject(workspaceId, project, user);
        for (StoredMachine machine : machines) {
            result.add(machineBuilderFactoryRegistry.get(machine.getType()).restoreMachine(machine.getId()));
        }
        return result;
    }

    public Machine getMachine(String machineId) throws NotFoundException, ServerException {
        final StoredMachine machine = machineDao.getById(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine %s not found", machineId));
        }
        return machineBuilderFactoryRegistry.get(machine.getType()).restoreMachine(machineId);
    }
}
