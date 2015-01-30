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

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.server.dto.MachineMetaInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(MachineRegistry.class);

    private final MachineMetaInfoDao machineMetaInfoDao;
    private final MachineFactories   machineFactories;

    @Inject
    public MachineRegistry(MachineMetaInfoDao machineMetaInfoDao, MachineFactories machineFactories) {
        this.machineMetaInfoDao = machineMetaInfoDao;
        this.machineFactories = machineFactories;
    }

    public void addMachine(MachineMetaInfo persistMachine) throws ServerException {
        machineMetaInfoDao.add(persistMachine);
    }

    public List<Machine> getMachines(String workspaceId, String project, String user) throws ServerException, ForbiddenException {
        List<Machine> result = new LinkedList<>();
        final List<MachineMetaInfo> machines = machineMetaInfoDao.findByUserWorkspaceProject(workspaceId, project, user);
        for (MachineMetaInfo machine : machines) {
            final MachineFactory machineFactory = machineFactories.get(machine.getType());
            if (machineFactory == null) {
                LOG.error("Unknown machine type {} of machine {}", machine.getType(), machine.getId());
            } else {
                result.add(machineFactory.getMachine(machine.getId()));
            }
        }
        return result;
    }

    public Machine getMachine(String machineId) throws NotFoundException, ServerException {
        final MachineMetaInfo machine = machineMetaInfoDao.getById(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine %s not found", machineId));
        }
        final MachineFactory machineFactory = machineFactories.get(machine.getType());
        if (machineFactory == null) {
            throw new ServerException(String.format("Unknown machine type %s", machine.getType()));
        }
        return machineFactory.getMachine(machineId);
    }
}
