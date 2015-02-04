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
package com.codenvy.api.machine.shared.model;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.machine.server.MachineBuilder;
import com.codenvy.api.machine.server.MachineFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
public final class MachineManager {

    private final Map<String, MachineFactory> factories = new HashMap<>();

    @Inject
    public MachineManager(Set<MachineFactory> factories) {
        for(MachineFactory f : factories) {
            this.factories.put(f.getMachineType(), f);
        }
    }



    /**
     * Creates and starts machine
     * @return newly created Machine
     * @throws InvalidRecipeException if recipe is not valid
     * @throws com.codenvy.api.machine.shared.model.MachineException - for any runtime exception during starting
     */
    Machine start(MachineBuilder builder) throws InvalidRecipeException, MachineException {


        return null;

    }

    /**
     * Stops machine
     * @param machine to stop
     */
    void stop(Machine machine) throws MachineException {

    }

    /**
     * Machine(s) the Project is bound to
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated machines,
     *                       false - if for only the parent project
     * @return list of machines or empty list
     */
    List<Machine> getMachines(ProjectBinding project, boolean includeModules) {

        return null;
    }

    /**
     * Machine by Id
     * @param machineId
     * @return particular machine
     */
    //Machine getMachine(String machineId) throws NotFoundException;

    /**
     * Saves machine to Snapshot storage
     * @param machine
     * @return stored Snapshot
     * @throws SnapshotException - if something went wrong during saving
     */
    Snapshot save(Machine machine) throws SnapshotException {

        return null;
    }

    /**
     * Starts machine with Snapshot
     * @param snapshot to restore
     * @return created Machine
     * @throws SnapshotException
     */
    Machine start(Snapshot snapshot) throws SnapshotException {

        return null;
    }

    /**
     * list of Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     * @return list of Snapshots
     */
    List <Snapshot> getSnapshots(ProjectBinding project, boolean includeModules) {

        return null;
    }

    /**
     * Snapshot by its ID
     * @param snapshot
     * @return the Snapshot
     * @throws NotFoundException if no such Snapshot found
     */
    //Snapshot getSnapshot(String snapshotId) throws NotFoundException;

    void removeSnapshot(Snapshot snapshot) {

    }

    /**
     * removes Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     * @throws SnapshotException
     */
    void removeSnapshots(ProjectBinding project, boolean includeModules) throws SnapshotException {

    }


}
