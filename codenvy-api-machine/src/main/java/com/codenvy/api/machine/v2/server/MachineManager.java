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
import com.codenvy.api.machine.v2.server.spi.ImageProvider;
import com.codenvy.api.machine.v2.server.spi.Machine;
import com.codenvy.api.machine.v2.shared.ProjectBinding;
import com.codenvy.api.machine.v2.shared.Recipe;

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
final class MachineManager {

    private final Map<String, ImageProvider> imageProviders = new HashMap<>();

    @Inject
    MachineManager(Set<ImageProvider> providers) {
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

    /**
     * Machine(s) the Project is bound to
     * @param owner
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated machines,
     *                       false - if for only the parent project
     * @return list of machines or empty list
     */
    public List<Machine> getMachines(String owner, ProjectBinding project) {
        return null;
    }

    public Machine getMachine(String machineId) throws NotFoundException {
        return null;
    }

    /**
     * Saves machine to Snapshot storage
     * @param machine
     * @return stored Snapshot
     * @throws SnapshotException - if something went wrong during saving
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

    /**
     * removes Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     */
    void removeSnapshots(ProjectBinding project, boolean includeModules) throws NotFoundException {

    }

    public void removeSnapshot(String snapshotId) throws NotFoundException {
    }

    /**
     * TODO doe we need this one
     * @param machine
     * @throws NotFoundException
     * @throws MachineException
     */
    public void destory(Machine machine) throws NotFoundException, MachineException {
    }


    /**
     *
     * @param machine
     * @param saveSnapshot
     * @return Snapshot or null if not saved
     * @throws NotFoundException
     * @throws MachineException
     */
    Snapshot destory(Machine machine, boolean saveSnapshot) throws NotFoundException, MachineException {
        return null;
    }
}
