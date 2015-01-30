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
import com.codenvy.api.machine.server.dto.MachineMetaInfo;
import com.codenvy.api.machine.server.dto.Snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author andrew00x
 */
public abstract class Machine {
    private static final Logger LOG = LoggerFactory.getLogger(Machine.class);

    public enum State {
        CREATING, RUNNING, STOPPING, STOPPED, DESTROYED
    }

    private final String             id;
    private final MachineMetaInfoDao machineMetaInfoDao;

    private LineConsumer outputConsumer;

    protected Machine(String id, MachineMetaInfoDao machineMetaInfoDao) {
        this.id = id;
        this.machineMetaInfoDao = machineMetaInfoDao;
        outputConsumer = LineConsumer.DEV_NULL;
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
    public abstract void destroy() throws ServerException;

    /** Saves the machine's current state and returns id of created snapshot. */
    public abstract String saveSnapshot(String description) throws ServerException;

    public abstract void removeSnapshot(String snapshotId) throws ServerException;

    public abstract List<Snapshot> getSnapshots() throws ServerException;

    public abstract void restoreToSnapshot(String snapshotId) throws ServerException;

    public abstract CommandProcess newCommandProcess(String command);

    /**
     * Get list of processes that are running in the machine.
     *
     * @return list of running processes
     * @throws ServerException
     *         if internal error occurs
     */
    public abstract List<CommandProcess> getRunningProcesses() throws ServerException;

    public abstract void bind(String workspaceId, String project) throws NotFoundException, ServerException;
    
    public abstract void unbind(String workspaceId, String project) throws NotFoundException, ServerException;

    public void setOutputConsumer(LineConsumer outputConsumer) {
        if (outputConsumer == null) {
            throw new IllegalArgumentException("Output consumer can't be null");
        }
        this.outputConsumer = outputConsumer;
    }

    protected LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    /** Gets id of machine. */
    public final String getId() {
        return id;
    }

    public final String getDisplayName() throws ServerException {
        try {
            return machineMetaInfoDao.getById(id).getDisplayName();
        } catch (NotFoundException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Can't retrieve display name of machine");
        }
    }

    public final void setDisplayName(String displayName) throws ServerException {
        try {
            final MachineMetaInfo machineInfo = machineMetaInfoDao.getById(id);

            machineMetaInfoDao.update(machineInfo.withDisplayName(displayName));
        } catch (NotFoundException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException("Display name of machine failed");
        }
    }
}
