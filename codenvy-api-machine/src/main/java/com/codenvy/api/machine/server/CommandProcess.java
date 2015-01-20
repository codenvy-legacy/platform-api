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

import com.codenvy.api.core.util.LineConsumer;

/**
 * @author andrew00x
 */
public interface CommandProcess {
    /** Return id of the process. Should be unique against all machine runner slaves. */
    long getId();

    /**
     * Starts process in the background.
     *
     * @see #start()
     * @see #isAlive()
     */
    void start();

    /**
     * Starts process.
     *
     * @param output
     *         consumer for process' output. If this parameter is {@code null} process started in the background. If this parameter is
     *         specified then this method is blocked until process is running.
     */
    void start(LineConsumer output);

    /**
     * Checks is process is running or not.
     *
     * @return {@code true} if process running and {@code false} otherwise
     */
    boolean isAlive();

    /**
     * Kills this process.
     */
    void kill();
}
