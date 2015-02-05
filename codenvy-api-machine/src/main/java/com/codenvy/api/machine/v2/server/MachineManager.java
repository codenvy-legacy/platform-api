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
import com.codenvy.api.machine.v2.server.spi.ImageProvider;
import com.codenvy.api.machine.v2.shared.Process;
import com.codenvy.api.machine.v2.shared.Command;
import com.codenvy.api.machine.v2.shared.Machine;
import com.codenvy.api.machine.v2.shared.ProjectBinding;
import com.codenvy.api.machine.v2.shared.RecipeId;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
public class MachineManager {

    private final Map<String, ImageProvider> imageProviders = new HashMap<>();

    @Inject
    public MachineManager(Set<ImageProvider> providers) {
        for (ImageProvider provider : providers) {
            this.imageProviders.put(provider.getType(), provider);
        }
    }


    /**
     * Creates and starts machine from scratch using recipe
     * @return newly created Machine
     * @throws InvalidRecipeException if recipe is not valid
     * @throws MachineException - for any runtime exception during starting
     * @throws com.codenvy.api.core.NotFoundException if recipe not found
     */
    public Machine create(RecipeId recipeId) throws InvalidRecipeException, MachineException, NotFoundException {
        return null;
    }

    /**
     * Restores and starts  machine from snapshot
     * @return newly created Machine
     * @throws com.codenvy.api.core.NotFoundException if snapshot not found
     * @throws MachineException - for any runtime exception during starting
     * @throws InvalidImageException - if Image pointed by snapshot is not valid
     */
    public Machine create(String snapshotId) throws NotFoundException, MachineException, InvalidImageException {
        return null;
    }

    public void bindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
    }

    public void unbindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
    }

    public List<ProjectBinding> getProjects(String machineId) throws NotFoundException, MachineException {
        return null;
    }

    public Machine getMachine(String machineId) throws NotFoundException {
        return null;
    }

    /**
     * Machine(s) the Project is bound to
     * @param owner
     * @param project
     * @return list of machines or empty list
     */
    public List<Machine> getMachines(String owner, ProjectBinding project) throws ServerException {
        return null;
    }

    /**
     * Saves machine to Snapshot storage
     * @param machine
     * @return stored Snapshot
     */
    public Snapshot save(Machine machine) throws MachineException {
        return null;
    }

    /**
     * list of Snapshots by project
     * @param owner
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     * @return list of Snapshots
     */
    public List<Snapshot> getSnapshots(String owner, ProjectBinding project, boolean includeModules) {
        return null;
    }

    public void removeSnapshot(String snapshotId) throws NotFoundException {
    }

    /**
     * removes Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     */
    public void removeSnapshots(ProjectBinding project, boolean includeModules) throws NotFoundException {
    }


    public Process exec(Command command, LineConsumer commandOutput, String machineId) throws NotFoundException, MachineException {
        return null;
    }

    /**
     *
     * @param machineId
     * @throws NotFoundException
     * @throws MachineException
     */
    public void destroy(String machineId) throws NotFoundException, MachineException {
    }
}
