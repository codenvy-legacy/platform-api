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
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.v2.server.spi.Image;
import com.codenvy.api.machine.v2.server.spi.ImageKey;
import com.codenvy.api.machine.v2.server.spi.ImageProvider;
import com.codenvy.api.machine.v2.server.spi.Instance;
import com.codenvy.api.machine.v2.server.spi.InstanceProcess;
import com.codenvy.api.machine.v2.shared.Command;
import com.codenvy.api.machine.v2.shared.Machine;
import com.codenvy.api.machine.v2.shared.Process;
import com.codenvy.api.machine.v2.shared.ProjectBinding;
import com.codenvy.api.machine.v2.shared.Recipe;
import com.codenvy.api.machine.v2.shared.RecipeId;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.NamedThreadFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
public class MachineManager {
    private final SnapshotStorage            snapshotStorage;
    private final Map<String, ImageProvider> imageProviders;
    private final Map<String, Machine>       machines;
    private final ExecutorService            executor;

    @Inject
    public MachineManager(SnapshotStorage snapshotStorage, Set<ImageProvider> imageProviders) {
        this.snapshotStorage = snapshotStorage;
        this.imageProviders = new HashMap<>();
        for (ImageProvider provider : imageProviders) {
            this.imageProviders.put(provider.getType(), provider);
        }
        machines = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("MachineManager-", true));
    }

    /**
     * Creates and starts machine from scratch using recipe.
     *
     * @param recipeId
     *         id of recipe
     * @param owner
     *         owner for new machine
     * @param creationLogsOutput
     *         output for image creation logs
     * @return Future for Machine creation process
     * @throws NotFoundException
     *         if recipe not found
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws MachineException
     *         if any exception occurs during starting
     */
    public Future<Machine> create(final RecipeId recipeId, final String owner, final LineConsumer creationLogsOutput)
            throws NotFoundException, UnsupportedRecipeException, InvalidRecipeException, MachineException {
        final Recipe recipe = getRecipe(recipeId);
        final String recipeType = recipe.getType();
        for (final ImageProvider imageProvider : imageProviders.values()) {
            if (imageProvider.getRecipeTypes().contains(recipeType)) {
                return executor.submit(new Callable<Machine>() {
                    @Override
                    public Machine call() throws Exception {
                        final Image image = imageProvider.createImage(recipe, creationLogsOutput);
                        final Instance instance = image.createInstance();
                        final Machine machine = new MachineImpl(generateMachineId(), imageProvider.getType(), owner, instance);
                        machines.put(machine.getId(), machine);
                        return machine;
                    }
                });
            }
        }
        throw new UnsupportedRecipeException(String.format("Recipe of type '%s' is not supported", recipeType));
    }

    private Recipe getRecipe(RecipeId recipeId) throws NotFoundException {
        return null; // TODO
    }

    /**
     * Restores and starts machine from snapshot.
     *
     * @param snapshotId
     *         id of snapshot
     * @param owner
     *         owner for new machine
     * @param creationLogsOutput
     *         output for image creation logs
     * @return Future for Machine creation process
     * @throws NotFoundException
     *         if snapshot not found
     * @throws InvalidImageException
     *         if Image pointed by snapshot is not valid
     * @throws MachineException
     *         if any exception occurs during starting
     */
    public Future<Machine> create(final String snapshotId, final String owner, final LineConsumer creationLogsOutput)
            throws NotFoundException, MachineException, InvalidImageException {
        final Snapshot snapshot = snapshotStorage.getSnapshot(snapshotId);
        final String imageType = snapshot.getImageType();
        final ImageProvider imageProvider = imageProviders.get(imageType);
        if (imageProvider == null) {
            throw new InvalidImageException(
                    String.format("Unable start machine from image '%s', unsupported image type '%s'", snapshotId, imageType));
        }
        return executor.submit(new Callable<Machine>() {
            @Override
            public Machine call() throws Exception {
                final Image image = imageProvider.createImage(snapshot.getImageKey(), creationLogsOutput);
                final Instance instance = image.createInstance();
                final Machine machine = new MachineImpl(generateMachineId(), imageProvider.getType(), owner, instance);
                machines.put(machine.getId(), machine);
                return machine;
            }
        });
    }

    private String generateMachineId() {
        return NameGenerator.generate("machine-", 16);
    }

    public void bindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
        doGetMachine(machineId).getProjectBindings().add(project);
        // TODO: 'physical' bind, e.g. download project sources and put in specific place
    }

    public void unbindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
        doGetMachine(machineId).getProjectBindings().remove(project);
        // TODO: 'physical' unbind, e.g. remove locally saved project
    }

    public List<ProjectBinding> getProjects(String machineId) throws NotFoundException, MachineException {
        return new ArrayList<>(doGetMachine(machineId).getProjectBindings());
    }

    public Machine getMachine(String machineId) throws NotFoundException {
        return doGetMachine(machineId);
    }

    private MachineImpl doGetMachine(String machineId) throws NotFoundException {
        final Machine machine = machines.get(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine '%s' does not exist", machineId));
        }
        return (MachineImpl)machine;
    }

    public List<Machine> getMachines() throws ServerException {
        return new ArrayList<>(machines.values());
    }

    /**
     * Machine(s) the Project is bound to.
     *
     * @param owner
     *         id of owner of machine
     * @param project
     *         project binding
     * @return list of machines or empty list
     */
    public List<Machine> getMachines(String owner, ProjectBinding project) {
        final List<Machine> result = new LinkedList<>();
        for (Machine machine : machines.values()) {
            if (owner != null && owner.equals(machine.getOwner()) && ((MachineImpl)machine).getProjectBindings().contains(project)) {
                result.add(machine);
            }
        }
        return result;
    }

    /**
     * Saves machine to Snapshot storage.
     *
     * @param machineId
     *         id of machine for saving
     * @param owner
     *         owner for new snapshot
     * @param description
     *         optional description that should help to understand purpose of new snapshot in future
     * @return Future for Snapshot creation process
     */
    public Future<Snapshot> save(final String machineId, final String owner, final String description)
            throws NotFoundException, MachineException {
        final MachineImpl machine = doGetMachine(machineId);
        return executor.submit(new Callable<Snapshot>() {
            @Override
            public Snapshot call() throws Exception {
                final Instance instance = machine.getInstance();
                final List<ProjectBinding> projectBindings = new ArrayList<>(machine.getProjectBindings());
                final ImageKey imageKey;
                try {
                    imageKey = instance.saveToImage();
                } catch (InstanceException e) {
                    throw new MachineException(e.getServiceError());
                }
                final Snapshot snapshot = new Snapshot(generateSnapshotId(), machine.getType(), imageKey, owner, System.currentTimeMillis(),
                                                       projectBindings, description);
                snapshotStorage.saveSnapshot(snapshot);
                return snapshot;
            }
        });
    }

    private String generateSnapshotId() {
        return NameGenerator.generate("snapshot-", 16);
    }

    /**
     * Gets list of Snapshots by project.
     *
     * @param owner
     *         id of owner of machine
     * @param project
     *         project binding
     * @return list of Snapshots
     */
    public List<Snapshot> getSnapshots(String owner, ProjectBinding project) {
        return snapshotStorage.findSnapshots(owner, project);
    }

    public void removeSnapshot(String snapshotId) throws NotFoundException {
        snapshotStorage.removeSnapshot(snapshotId);
    }

    /**
     * Removes Snapshots by project.
     *
     * @param project
     *         project binding
     */
    public void removeSnapshots(String owner, ProjectBinding project) {
        for (Snapshot snapshot : snapshotStorage.findSnapshots(owner, project)) {
            try {
                snapshotStorage.removeSnapshot(snapshot.getId());
            } catch (NotFoundException ignored) {
                // This is not expected since we just get list of snapshots from DAO.
            }
        }
    }

    public Process exec(final String machineId, final Command command, final LineConsumer commandOutput)
            throws NotFoundException, MachineException {
        final MachineImpl machine = doGetMachine(machineId);
        final Instance instance = machine.getInstance();
        final InstanceProcess instanceProcess;
        try {
            instanceProcess = instance.createProcess(command.getCommandLine());
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
        final Runnable execTask = new Runnable() {
            @Override
            public void run() {
                try {
                    instanceProcess.start(commandOutput);
                } catch (ConflictException | InstanceException e) {
                    try {
                        commandOutput.writeLine(e.getMessage());
                    } catch (IOException ignored) {
                    }
                }
            }
        };
        executor.execute(execTask);
        return new ProcessImpl(instanceProcess);
    }

    public void destroy(String machineId) throws NotFoundException, MachineException {
        final MachineImpl machine = doGetMachine(machineId);
        try {
            machine.getInstance().destroy();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
        machines.remove(machineId);
    }
}
