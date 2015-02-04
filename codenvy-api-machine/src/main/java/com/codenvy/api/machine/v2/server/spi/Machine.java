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
package com.codenvy.api.machine.v2.server.spi;

import com.codenvy.api.machine.v2.server.MachineException;
import com.codenvy.api.machine.v2.server.Snapshot;
import com.codenvy.api.machine.v2.shared.Command;
import com.codenvy.api.machine.v2.shared.ProjectBinding;

import java.util.List;

/**
 * @author gazarenkov
 */
public interface Machine {

    public enum State {
        CREATING, RUNNIING, DESTROYING
    }

    String getType();

    String getId();

    /**
     * Gets identifier of user who launched this machine.
     *
     * @return identifier of user who launched this machine
     */
    String getOwner();

    State getState() throws MachineException;

    ProjectBinding bindProject(String workspace, String project) throws MachineException;

    List<ProjectBinding> getProjects() throws MachineException;

    List<Process> getProcesses() throws MachineException;

    Process newProcess(Command command) throws MachineException;

    // TODO
    //MachineConfig getConfig();
    //RuntimeInfo getInfo();

    Snapshot saveSnapshot() throws MachineException;

    void destroy() throws MachineException;
}
