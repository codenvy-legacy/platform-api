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

import java.io.File;
import java.util.List;

/**
 * @author gazarenkov
 */
public interface Instance {

    InstanceMetadata getMetadata() throws MachineException;

    List<InstanceProcess> getProcesses() throws MachineException;

    InstanceProcess createProcess(String commandLine) throws MachineException;

    void mount(File dir) throws MachineException;

    ImageKey saveToImage() throws MachineException;

    void destroy() throws MachineException;
}
