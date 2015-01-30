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
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.dto.MachineMetaInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class Machines {
    private static final Logger LOG = LoggerFactory.getLogger(Machines.class);

    private final MachineMetaInfoDao machineMetaInfoDao;
    private final MachineIdGenerator machineIdGenerator;
    private final Map<String, MachineFactory> machineFactories;

    @Inject
    public Machines(MachineMetaInfoDao machineMetaInfoDao, Set<MachineFactory> machineFactories, MachineIdGenerator machineIdGenerator) {
        this.machineMetaInfoDao = machineMetaInfoDao;
        this.machineIdGenerator = machineIdGenerator;
        this.machineFactories = new ConcurrentHashMap<>();
        for (MachineFactory builderFactory : machineFactories) {
            this.machineFactories.put(builderFactory.getMachineType(), builderFactory);
        }
    }

    public MachineBuilder newMachineOfType(String machineType) throws NotFoundException {
        return getMachineFactory(machineType).newMachineBuilder()
                                             .setMachineId(machineIdGenerator.generateId())
                                             .setMachineType(machineType)
                                             .setMachineMetaInfoDao(machineMetaInfoDao);
    }

    public List<Machine> getMachines(String workspaceId, String project, String user) throws ServerException, ForbiddenException {
        List<Machine> result = new LinkedList<>();
        for (MachineMetaInfo machineMetaInfo : machineMetaInfoDao.findByUserWorkspaceProject(workspaceId, project, user)) {
            try {
                final Machine machine = getMachineFactory(machineMetaInfo.getType()).getMachine(machineMetaInfo.getId());
                machine.setMachineMetaInfoDao(machineMetaInfoDao);
                result.add(machine);
            } catch (NotFoundException e) {
                LOG.error(e.getMessage());
            }
        }
        return result;
    }

    public Machine getMachine(String machineId) throws NotFoundException, ServerException {
        final MachineMetaInfo machineMetaInfo = machineMetaInfoDao.getById(machineId);
        if (machineMetaInfo == null) {
            throw new NotFoundException(String.format("Machine %s not found", machineId));
        }
        final Machine machine = getMachineFactory(machineMetaInfo.getType()).getMachine(machineId);
        machine.setMachineMetaInfoDao(machineMetaInfoDao);
        return machine;
    }

    private MachineFactory getMachineFactory(String machineType) throws NotFoundException {
        final MachineFactory machineFactory = machineFactories.get(machineType);
        if (machineFactory == null) {
            throw new NotFoundException(String.format("Unknown machine type %s", machineType));
        }
        return machineFactory;
    }
}
