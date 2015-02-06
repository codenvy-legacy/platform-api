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

import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.v2.server.InstanceException;

import java.io.File;
import java.util.List;

/**
 * @author gazarenkov
 */
public interface Instance {

    public enum State {
        CREATING, RUNNING, DESTROYING
    }

    InstanceMetadata getMetadata() throws InstanceException;

    State getState();

    List<InstanceProcess> getProcesses() throws InstanceException;

    InstanceProcess createProcess(String commandLine, LineConsumer output) throws InstanceException;

    void mount(File dir) throws InstanceException;

    ImageKey saveToImage() throws InstanceException;

    void destroy() throws InstanceException;
}
