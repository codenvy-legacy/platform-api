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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;

/**
 * Represents process in the machine created by command.
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public interface InstanceProcess {
    /**
     * Returns pid of the process. Returns {@code 0} if process isn't started yet.
     *
     * @return pid of the process
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     */
    int getPid() throws ServerException;

    /**
     * Returns command with all its arguments
     *
     * @return command
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     */
    String getCommandLine() throws ServerException;

    /**
     * Starts process in the background.
     *
     * @throws com.codenvy.api.core.ConflictException
     *         if process is started already
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     * @see #start()
     * @see #isAlive()
     */
    void start() throws ConflictException, ServerException;

    /**
     * Starts process.
     *
     * @param output
     *         consumer for process' output. If this parameter is {@code null} process started in the background. If this parameter is
     *         specified then this method is blocked until process is running.
     * @throws com.codenvy.api.core.ConflictException
     *         if process is started already
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     */
    void start(LineConsumer output) throws ConflictException, ServerException;

    /**
     * Checks is process is running or not.
     *
     * @return {@code true} if process running and {@code false} otherwise
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     */
    boolean isAlive() throws ServerException;

    /**
     * Kills this process.
     *
     * @throws com.codenvy.api.core.ServerException
     *         if internal error occurs
     */
    void kill() throws ServerException;
}
