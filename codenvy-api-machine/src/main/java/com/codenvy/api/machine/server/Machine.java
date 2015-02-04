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

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.dto.MachineMetadata;
import com.codenvy.api.machine.server.dto.Snapshot;

import java.util.List;

/**
 * @author andrew00x
 */
public abstract class Machine {
    public enum State {
        CREATING, RUNNING, STOPPING, STOPPED, DESTROYED
    }

    private final String id;

    private MachineMetadataDao machineMetadataDao;
    private MachineMetadata    machineMetadata;
    private LineConsumer outputConsumer = LineConsumer.DEV_NULL;

    protected Machine(String id) {
        this.id = id;
    }

    /** Gets id of machine. */
    public final String getId() {
        return id;
    }

    /** Gets state of machine. */
    public abstract State getState();

    /**
     * Starts machine.
     *
     * @throws ServerException
     *         if internal error occurs
     */
    public abstract void start() throws ServerException;

    /**
     * Stops machine.
     *
     * @throws ServerException
     *         if internal error occurs
     */
    public abstract void stop() throws ServerException;

    /**
     * Destroys machine.
     *
     * @throws ServerException
     *         if internal error occurs
     */
    public final void destroy() throws ServerException {
        doDestroy();
        final MachineMetadataDao machineMetadataDao = getMachineMetadataDao();
        if (machineMetadataDao != null) {
            try {
                machineMetadataDao.remove(id);
            } catch (NotFoundException ignored) {
            }
        }
    }

    protected abstract void doDestroy()  throws ServerException;

    /** Saves the machine's current state and returns id of created snapshot. */
    public abstract String saveSnapshot(String description) throws ServerException;

    public abstract void removeSnapshot(String snapshotId) throws ServerException;

    public abstract List<Snapshot> getSnapshots() throws ServerException;

    public abstract void restoreToSnapshot(String snapshotId) throws ServerException;

    public final void bind(String workspaceId, String project) throws NotFoundException, ServerException {
        doBind(workspaceId, project);

        final MachineMetadata machineMetadata = getMachineMetadata();
        machineMetadata.getProjects().add(project);
        updateMachineMetaInfo(machineMetadata);
    }

    protected abstract void doBind(String workspaceId, String project) throws NotFoundException, ServerException;

    public final void unbind(String workspaceId, String project) throws NotFoundException, ServerException {
        doUnbind(workspaceId, project);

        final MachineMetadata machineMetadata = getMachineMetadata();
        machineMetadata.getProjects().remove(project);
        updateMachineMetaInfo(machineMetadata);
    }

    protected abstract void doUnbind(String workspaceId, String project) throws NotFoundException, ServerException;

    public abstract CommandProcess newCommandProcess(String command);

    /**
     * Get list of processes that are running in the machine.
     *
     * @return list of running processes
     * @throws ServerException
     *         if internal error occurs
     */
    public abstract List<CommandProcess> getRunningProcesses() throws ServerException;

    public final String getDisplayName() throws ServerException {
        return getMachineMetadata().getDisplayName();
    }

    public final void setDisplayName(String displayName) throws ServerException {
        final MachineMetadata machineMetadata = getMachineMetadata();
        machineMetadata.setDisplayName(displayName);
        updateMachineMetaInfo(machineMetadata);
    }

    public final String getCreatedBy() throws ServerException {
        return getMachineMetadata().getCreatedBy();
    }

    public final String getWorkspaceId() throws ServerException {
        return getMachineMetadata().getWorkspaceId();
    }

    public final String getType() throws ServerException {
        return getMachineMetadata().getType();
    }

    public final List<String> getProjects() throws ServerException {
        return getMachineMetadata().getProjects();
    }

    private MachineMetadata getMachineMetadata() throws ServerException {
        MachineMetadata machineMetadata = this.machineMetadata;
        if (machineMetadata != null) {
            return machineMetadata;
        } else {
            final MachineMetadataDao machineMetadataDao = getMachineMetadataDao();
            if (machineMetadataDao != null) {
                try {
                    machineMetadata = machineMetadataDao.getById(id);
                    this.machineMetadata = machineMetadata;
                    return machineMetadata;
                } catch (NotFoundException e) {
                    throw new ServerException(String.format("Meta information for machine %s not found.", id));
                }
            }
            throw new ServerException(String.format("Meta information for machine %s isn't available.", id));
        }
    }

    private void updateMachineMetaInfo(MachineMetadata machineMetadata) throws ServerException {
        final MachineMetadataDao machineMetadataDao = getMachineMetadataDao();
        if (machineMetadataDao != null) {
            try {
                machineMetadataDao.update(machineMetadata);
                this.machineMetadata = machineMetadata;
            } catch (NotFoundException e) {
                throw new ServerException(String.format("Meta information for machine %s not found.", id));
            }
        }
        throw new ServerException(String.format("Meta information for machine %s isn't available.", id));
    }

    public final void setOutputConsumer(LineConsumer outputConsumer) {
        if (outputConsumer == null) {
            throw new IllegalArgumentException("Output consumer can't be null");
        }
        this.outputConsumer = outputConsumer;
    }

    protected LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    protected MachineMetadataDao getMachineMetadataDao() {
        return machineMetadataDao;
    }

    final void setMachineMetadataDao(MachineMetadataDao machineMetadataDao) {
        this.machineMetadataDao = machineMetadataDao;
    }
}
