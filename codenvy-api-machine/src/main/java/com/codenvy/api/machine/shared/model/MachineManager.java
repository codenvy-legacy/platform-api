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
import com.codenvy.api.machine.server.MachineRecipe;

import java.util.List;

/**
 *
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
public interface MachineManager {


    /**
     * Creates and starts machine
     * @return newly created Machine
     * @throws InvalidRecipeException if recipe is not valid
     * @throws com.codenvy.api.machine.shared.model.MachineException - for any runtime exception during starting
     */
    Machine start(MachineRecipe recipe) throws InvalidRecipeException, MachineException ;

    /**
     * Stops machine
     * @param machine to stop
     */
    void stop(Machine machine) throws MachineException;

    /**
     * Machine(s) the Project is bound to
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated machines,
     *                       false - if for only the parent project
     * @return list of machines or empty list
     */
    List<Machine> getMachines(ProjectBinding project, boolean includeModules);

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
    Snapshot save(Machine machine) throws SnapshotException;

    /**
     * Starts machine with Snapshot
     * @param snapshot to restore
     * @return created Machine
     * @throws SnapshotException
     */
    Machine start(Snapshot snapshot) throws SnapshotException;

    /**
     * list of Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     * @return list of Snapshots
     */
    List <Snapshot> getSnapshots(ProjectBinding project, boolean includeModules);

    /**
     * Snapshot by its ID
     * @param snapshotId
     * @return the Snapshot
     * @throws NotFoundException if no such Snapshot found
     */
    //Snapshot getSnapshot(String snapshotId) throws NotFoundException;

    void removeSnapshot(Snapshot snapshot);

    /**
     * removes Snapshots by project
     * @param project
     * @param includeModules - true if also needs all the Project's modules associated snapshots,
     *                       false - if for only for parent project
     * @throws SnapshotException
     */
    void removeSnapshots(ProjectBinding project, boolean includeModules) throws SnapshotException;

}
