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
package com.codenvy.api.machine.shared;

import com.codenvy.api.machine.server.MachineException;

/**
 * @author andrew00x
 */
public interface Process {
    int getPid() throws MachineException;

    String getCommandLine() throws MachineException;

    boolean isAlive() throws MachineException;
}