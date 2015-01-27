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
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final MachineDao                     machineDao;
    private final ConcurrentMap<String, Machine> activeMachines;

    @Inject
    public MachineRegistry(MachineDao machineDao) {
        this.machineDao = machineDao;
        this.activeMachines = new ConcurrentHashMap<>();
    }

    public void addMachine(StoredMachine persistMachine, Machine runtimeMachine) throws ServerException {
        machineDao.create(DtoFactory.getInstance().createDto(StoredMachine.class)
                                    .withId(persistMachine.getId())
                                    .withUser(persistMachine.getUser())
                                    .withWorkspaceId(persistMachine.getWorkspaceId())
                                    .withProject(persistMachine.getProject()));

        activeMachines.put(persistMachine.getId(), runtimeMachine);
    }

    public List<MachineDescriptor> getMachines(String workspaceId, String project, String user) throws ServerException {
        List<MachineDescriptor> result = new LinkedList<>();
        final List<StoredMachine> machines = machineDao.findByUserWorkspaceProject(workspaceId, project, user);
        for (StoredMachine machine : machines) {
            Machine.State state =
                    activeMachines.containsKey(machine.getId()) ? activeMachines.get(machine.getId()).getState() : Machine.State.INACTIVE;

            result.add(DtoFactory.getInstance().createDto(MachineDescriptor.class)
                                 .withId(machine.getId())
                                 .withWorkspaceId(machine.getWorkspaceId())
                                 .withProject(machine.getProject())
                                 .withUser(machine.getUser())
                                 .withState(state));
        }

        return result;
    }

    public Machine getActiveMachine(String machineId) throws NotFoundException {
        final Machine machine = activeMachines.get(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine %s not found", machineId));
        }
        return machine;
    }

    public void removeActiveMachine(String machineId) throws NotFoundException {
        if (!activeMachines.containsKey(machineId)) {
            throw new NotFoundException(String.format("Machine with id %s not found", machineId));
        }
        activeMachines.remove(machineId);
    }
}
