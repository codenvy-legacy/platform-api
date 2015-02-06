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
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.user.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
public class MachineManager {
    // TODO: run machine creation process asynchronously (in separate threads)

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
     * @return newly created Machine
     * @throws NotFoundException
     *         if recipe not found
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws MachineException
     *         if any exception occurs during starting
     */
    public Machine create(RecipeId recipeId, LineConsumer creationLogsOutput)
            throws NotFoundException, UnsupportedRecipeException, InvalidRecipeException, MachineException {
        final Recipe recipe = getRecipe(recipeId);
        final String recipeType = recipe.getType();
        for (ImageProvider imageProvider : imageProviders.values()) {
            if (imageProvider.getRecipeTypes().contains(recipeType)) {
                final Image image = imageProvider.createImage(recipe);
                final Instance instance = image.createInstance();
                final MachineImpl machine = new MachineImpl(generateMachineId(), imageProvider.getType(), getCurrentUserId(), instance);
                machines.put(machine.getId(), machine);
                return machine;
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
     * @return newly created Machine
     * @throws NotFoundException
     *         if snapshot not found
     * @throws InvalidImageException
     *         if Image pointed by snapshot is not valid
     * @throws MachineException
     *         if any exception occurs during starting
     */
    public Machine create(String snapshotId, LineConsumer creationLogsOutput)
            throws NotFoundException, MachineException, InvalidImageException {
        final Snapshot snapshot = snapshotStorage.getSnapshot(snapshotId);
        final String imageType = snapshot.getImageType();
        final ImageProvider imageProvider = imageProviders.get(imageType);
        if (imageProvider == null) {
            throw new InvalidImageException(
                    String.format("Unable start machine from image '%s', unsupported image type '%s'", snapshotId, imageType));
        }
        final Image image = imageProvider.createImage(snapshot.getImageKey());
        final Instance instance = image.createInstance();
        final MachineImpl machine = new MachineImpl(generateMachineId(), imageProvider.getType(), getCurrentUserId(), instance);
        machines.put(machine.getId(), machine);
        return machine;
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
     * @param description
     *         optional description that should help to understand purpose of new snapshot in future
     * @return stored Snapshot
     */
    public Snapshot save(String machineId, String description) throws NotFoundException, MachineException {
        final MachineImpl machine = doGetMachine(machineId);
        final Instance instance = machine.getInstance();
        final ImageKey imageKey;
        try {
            imageKey = instance.saveToImage();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
        final Snapshot snapshot =
                new Snapshot(generateSnapshotId(), machine.getType(), imageKey, getCurrentUserId(), System.currentTimeMillis(),
                             new ArrayList<>(machine.getProjectBindings()), description);
        snapshotStorage.saveSnapshot(snapshot);
        return snapshot;
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
    public void removeSnapshots(ProjectBinding project) {
        for (Snapshot snapshot : snapshotStorage.findSnapshots(null, project)) {
            try {
                snapshotStorage.removeSnapshot(snapshot.getId());
            } catch (NotFoundException ignored) {
                // This is not expected since we just get list of snapshots from DAO.
            }
        }
    }

    public Process exec(String machineId, final Command command, final LineConsumer commandOutput) throws NotFoundException, MachineException {
        final MachineImpl machine = doGetMachine(machineId);
        final Instance instance = machine.getInstance();
        final Instance.State state = instance.getState();
        if (state != Instance.State.RUNNING) {
            throw new MachineException(String.format("Unable execute command on machine that is in '%s' state", state));
        }
        final InstanceProcess instanceProcess;
        try {
            instanceProcess = instance.createProcess(command.getCommandLine(), commandOutput);
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
        final Runnable execTask = new Runnable() {
            @Override
            public void run() {
                try {
                    instanceProcess.start(commandOutput);
                } catch (Exception e) {
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

    private String getCurrentUserId() {
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getId();
        }
        return null;
    }
}
