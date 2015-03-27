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
package org.eclipse.che.api.machine.server.spi;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.MachineException;

import java.io.File;
import java.util.List;

/**
 * @author gazarenkov
 */
public interface Instance {

    InstanceMetadata getMetadata() throws MachineException;

    InstanceProcess getProcess(int pid) throws NotFoundException, MachineException;

    List<InstanceProcess> getProcesses() throws MachineException;

    InstanceProcess createProcess(String commandLine) throws MachineException;

    /**
     * Mount host directory to some directory in this instance. Methods returns path to the instance's directory (target of mount).
     *
     * @param dir
     *         mountpoint on local host
     * @return path to directory on this instance where host directory is mounted
     * @throws MachineException
     *         if any error occurs while mounting
     */
    String mount(File dir) throws MachineException;

    /**
     * Directory on local host that is mounted some target place in this instance an reserved for holding projects bounded to this
     * instance. Method returns {@code null} if there is no any directory for bounded projects.
     */
    File getHostProjectsFolder();

    ImageKey saveToImage(String owner) throws MachineException;

    void destroy() throws MachineException;

    String getLocationAddress();
}
