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

import com.codenvy.api.machine.v2.server.spi.InstanceProcess;
import com.codenvy.api.machine.v2.shared.Process;

/**
 * @author andrew00x
 */
public class ProcessImpl implements Process {
    private final InstanceProcess instanceProcess;

    ProcessImpl(InstanceProcess instanceProcess) {
        this.instanceProcess = instanceProcess;
    }

    public int getPid() throws MachineException {
        try {
            return instanceProcess.getPid();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
    }

    public String getCommandLine() throws MachineException {
        try {
            return instanceProcess.getCommandLine();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
    }

    public boolean isAlive() throws MachineException {
        try {
            return instanceProcess.isAlive();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
    }

    public void kill() throws MachineException {
        try {
            instanceProcess.kill();
        } catch (InstanceException e) {
            throw new MachineException(e.getServiceError());
        }
    }
}
