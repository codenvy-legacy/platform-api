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
import com.codenvy.api.machine.server.dto.MachineMetadata;
import com.codenvy.commons.lang.NameGenerator;

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

    private final MachineMetadataDao          machineMetadataDao;
    private final Map<String, MachineFactory> machineFactories;

    @Inject
    public Machines(MachineMetadataDao machineMetadataDao, Set<MachineFactory> machineFactories) {
        this.machineMetadataDao = machineMetadataDao;
        this.machineFactories = new ConcurrentHashMap<>();
        for (MachineFactory builderFactory : machineFactories) {
            this.machineFactories.put(builderFactory.getMachineType(), builderFactory);
        }
    }

    public MachineBuilder newMachineOfType(String machineType) throws NotFoundException {
        return getMachineFactory(machineType).newMachineBuilder()
                                             .setMachineId(generateMachineId())
                                             .setMachineType(machineType)
                                             .setMachineMetadataDao(machineMetadataDao);
    }

    public List<Machine> getMachines(String workspaceId, String project, String user) throws ServerException, ForbiddenException {
        List<Machine> result = new LinkedList<>();
        for (MachineMetadata machineMetadata : machineMetadataDao.findByUserWorkspaceProject(workspaceId, project, user)) {
            try {
                final Machine machine = getMachineFactory(machineMetadata.getType()).getMachine(machineMetadata.getId());
                machine.setMachineMetadataDao(machineMetadataDao);
                result.add(machine);
            } catch (NotFoundException e) {
                LOG.error(e.getMessage());
            }
        }
        return result;
    }

    public Machine getMachine(String machineId) throws NotFoundException, ServerException {
        final MachineMetadata machineMetadata = machineMetadataDao.getById(machineId);
        final Machine machine = getMachineFactory(machineMetadata.getType()).getMachine(machineId);
        machine.setMachineMetadataDao(machineMetadataDao);
        return machine;
    }

    private MachineFactory getMachineFactory(String machineType) throws NotFoundException {
        final MachineFactory machineFactory = machineFactories.get(machineType);
        if (machineFactory == null) {
            throw new NotFoundException(String.format("Unknown machine type %s", machineType));
        }
        return machineFactory;
    }

    protected String generateMachineId() {
        return NameGenerator.generate("", 16);
    }
}
