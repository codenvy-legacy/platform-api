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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.shared.dto.ActiveMachineDescriptor;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final MachineMetaInfoDao             machineMetaInfoDao;
    private final ConcurrentMap<String, Machine> activeMachines;
    private       ExecutorService                executorService;
    private       MachineBuilderFactory          machineBuilderFactory;

    @Inject
    public MachineRegistry(MachineMetaInfoDao machineMetaInfoDao,
                           MachineBuilderFactory machineBuilderFactory) {
        this.machineMetaInfoDao = machineMetaInfoDao;
        this.machineBuilderFactory = machineBuilderFactory;
        this.activeMachines = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("MachineRegistry-", true));
    }

    // TODO add ability to check recipe before machine build execution to throw exception synchronously if recipe is not found or valid
    // will help in situations when build fails before client will be able to subscribe to output of build

    // TODO add ability to name machines

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    public List<ActiveMachineDescriptor> getMachines(String workspaceId, String project, String user) throws ServerException {
        List<ActiveMachineDescriptor> result = new LinkedList<>();
        final List<MachineDescriptor> machines = machineMetaInfoDao.findByUserWorkspaceProject(workspaceId, project, user);
        for (MachineDescriptor machine : machines) {
            // TODO should ask slave machine runner for state
            String state = activeMachines.containsKey(machine.getId()) ? "active" : "inactive";

            result.add(DtoFactory.getInstance().createDto(ActiveMachineDescriptor.class)
                                 .withId(machine.getId())
                                 .withWorkspaceId(machine.getWorkspaceId())
                                 .withProject(machine.getProject())
                                 .withUser(machine.getUser())
                                 .withState(state));
        }

        return result;
    }

    public String createMachine(final InputStream recipeStream,
                                final String machineType,
                                final String workspace,
                                final String project,
                                final String user) {
        final String outputChannelId = NameGenerator.generate("", 16);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final Machine machine;
                try {
                    // TODO Add logReader to send logs to client
                    machine = machineBuilderFactory.newMachineBuilder(machineType)
                            // TODO .setOutput(id)
                            .setRecipe(new BaseMachineRecipe() {
                                @Override
                                public InputStream asStream() {
                                    return recipeStream;
                                }
                            })
                            .buildMachine();

                    machineMetaInfoDao.create(DtoFactory.getInstance().createDto(MachineDescriptor.class)
                            // TODO put image id here .withId()
                                                      .withUser(user)
                                                      .withWorkspaceId(workspace)
                                                      .withProject(project));

                    activeMachines.put(machine.getId(), machine);
                } catch (ServerException | ForbiddenException | NotFoundException e) {
                    // TODO put to logReader
                }
            }
        });

        return outputChannelId;
    }

    public void destroy(final String machineId) throws NotFoundException {
        if (!activeMachines.containsKey(machineId)) {
            throw new NotFoundException(String.format("Machine with id %s not found", machineId));
        }

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    activeMachines.get(machineId).destroy();
                } catch (ServerException e) {
                    // TODO put to logReader
                }
            }
        });
    }

    public void suspend(final String machineId) throws NotFoundException {
        if (!activeMachines.containsKey(machineId)) {
            throw new NotFoundException(String.format("Machine with id %s not found", machineId));
        }

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final Machine machine = activeMachines.get(machineId);

                try {
                    machine.suspend();
                } catch (ServerException e) {
                    // TODO put to logReader
                }
            }
        });
    }

    public String resumeMachine(String machineId) throws NotFoundException, ServerException {
        machineMetaInfoDao.getById(machineId);

        final String outputChannelId = NameGenerator.generate("", 16);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // TODO resume
            }
        });

        return outputChannelId;
    }

    public String execute(String machineId, final String command) throws NotFoundException {
        final Machine machine;
        if ((machine = activeMachines.get(machineId)) == null) {
            throw new NotFoundException(String.format("Machine with id %s not found", machineId));
        }

        final String outputChannelId = NameGenerator.generate("", 16);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                final CommandProcess commandProcess = machine.newCommandProcess(command);
                try {
                    commandProcess.start();
                } catch (ConflictException | ServerException e) {
                    // TODO put to logReader
                }
            }
        });

        return outputChannelId;
    }

    public List<CommandProcess> getProcesses(String machineId) throws ServerException {
        return activeMachines.get(machineId).getRunningProcesses();
    }

    public void killProcess(String machineId, int processId) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine;
        if ((machine = activeMachines.get(machineId)) == null) {
            throw new NotFoundException(String.format("Machine with id %s not found", machineId));
        }
        for (CommandProcess commandProcess : machine.getRunningProcesses()) {
            if (commandProcess.getPid() == processId) {
                if (!commandProcess.isAlive()) {
                    throw new ForbiddenException("Process finished already");
                }
                // TODO should we do that in separate thread?
                commandProcess.kill();
                return;
            }
        }
        throw new NotFoundException(String.format("Process with id %s not found in machine %s.", processId, machineId));
    }
}
