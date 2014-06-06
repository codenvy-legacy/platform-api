/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.util.Cancellable;
import com.codenvy.api.runner.ApplicationStatus;

/** @author andrew00x */
public interface RunnerProcess extends Cancellable {
    interface Callback {
        void started(RunnerProcess process);

        void stopped(RunnerProcess process);

        void error(RunnerProcess process, Throwable t);
    }

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    Long getId();

    ApplicationStatus getStatus();

    /**
     * Get application process.
     *
     * @return ApplicationProcess or {@code null} if application is not started yet.
     */
    ApplicationProcess getApplicationProcess();

    /**
     * Get name of runner which owns this process.
     *
     * @return name of runner which owns this process
     */
    String getRunner();

    /** Get configuration of current process. */
    RunnerConfiguration getConfiguration();

    Throwable getError();

    /**
     * Get time when process was started.
     *
     * @return time when process was started or {@code -1} if process is not started yet
     */
    long getStartTime();

    /**
     * Get time when process was started.
     *
     * @return time when process was stopped or {@code -1} if process is not started yet or still running
     */
    long getStopTime();

    /**
     * Get uptime of application process.
     *
     * @return time when process was started or {@code 0} if process is not started yet.
     */
    long getUptime();
}
