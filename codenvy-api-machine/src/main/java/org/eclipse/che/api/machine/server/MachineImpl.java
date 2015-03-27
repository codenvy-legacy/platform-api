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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.Machine;
import org.eclipse.che.api.machine.shared.MachineState;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author andrew00x
 */
public class MachineImpl implements Machine {
    private final String              id;
    private final String              type;
    private final String              owner;
    private final LineConsumer        machineLogsOutput;
    private final Set<ProjectBinding> projectBindings;
    private final String              workspaceId;

    private Instance     instance;
    private MachineState state;

    MachineImpl(String id, String type, String workspaceId, String owner, LineConsumer machineLogsOutput) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.machineLogsOutput = machineLogsOutput;
        this.workspaceId = workspaceId;
        projectBindings = new CopyOnWriteArraySet<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public Set<ProjectBinding> getProjects() {
        return projectBindings;
    }

    @Override
    public InstanceMetadata getMetadata() throws MachineException {
        return instance.getMetadata();
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    public synchronized MachineState getState() {
        return state;
    }

    ProcessImpl getProcess(int pid) throws NotFoundException, MachineException {
        final Instance myInstance = getInstance();
        if (myInstance == null) {
            throw new MachineException(String.format("Machine %s is not ready to perform this action", id));
        }
        return new ProcessImpl(myInstance.getProcess(pid));
    }

    List<ProcessImpl> getProcesses() throws MachineException {
        final Instance myInstance = getInstance();
        if (myInstance == null) {
            throw new MachineException(String.format("Machine %s is not ready to perform this action", id));
        }
        final List<InstanceProcess> instanceProcesses = myInstance.getProcesses();
        final List<ProcessImpl> processes = new LinkedList<>();
        for (InstanceProcess instanceProcess : instanceProcesses) {
            processes.add(new ProcessImpl(instanceProcess));
        }
        return processes;
    }

    LineConsumer getMachineLogsOutput() {
        return machineLogsOutput;
    }

    synchronized void setState(MachineState state) {
        this.state = state;
    }

    synchronized Instance getInstance() {
        return instance;
    }

    synchronized void setInstance(Instance instance) {
        this.instance = instance;
    }

    public String getLocationAddress() {
        return instance.getLocationAddress();
    }

    public File getHostProjectsFolder() {
        return instance.getHostProjectsFolder();
    }
}
