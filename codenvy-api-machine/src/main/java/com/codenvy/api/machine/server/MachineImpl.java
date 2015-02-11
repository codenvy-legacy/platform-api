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
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.spi.Instance;
import com.codenvy.api.machine.server.spi.InstanceProcess;
import com.codenvy.api.machine.shared.Machine;
import com.codenvy.api.machine.shared.MachineState;
import com.codenvy.api.machine.shared.ProjectBinding;

import java.util.Collections;
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

    private Instance     instance;
    private MachineState state;

    MachineImpl(String id, String type, String owner, LineConsumer machineLogsOutput) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.machineLogsOutput = machineLogsOutput;
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

    public synchronized MachineState getState() {
        return state;
    }

    public ProcessImpl getProcesse(int pid) throws NotFoundException, MachineException {
        final Instance myInstance = getInstance();
        if (myInstance == null) {
            throw new NotFoundException(String.format("No such process: %d in machine %s", pid, id));
        }
        return new ProcessImpl(myInstance.getProcess(pid));
    }

    public List<ProcessImpl> getProcesses() throws MachineException {
        final Instance myInstance = getInstance();
        if (myInstance == null) {
            return Collections.emptyList();
        }
        final List<InstanceProcess> instanceProcesses = myInstance.getProcesses();
        final List<ProcessImpl> processes = new LinkedList<>();
        for (InstanceProcess instanceProcess : instanceProcesses) {
            processes.add(new ProcessImpl(instanceProcess));
        }
        return processes;
    }

    public Set<ProjectBinding> getProjectBindings() {
        return projectBindings;
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
}
