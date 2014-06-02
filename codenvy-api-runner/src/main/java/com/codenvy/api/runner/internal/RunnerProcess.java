/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
