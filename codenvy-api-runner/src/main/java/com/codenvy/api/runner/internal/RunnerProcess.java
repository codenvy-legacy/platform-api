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
import com.codenvy.api.runner.RunnerException;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
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
     * Reports whether process is running or not.
     *
     * @return {@code true} if process is running and {@code false} otherwise
     * @throws RunnerException
     *         if an error occurs when try to check status of application
     */
    boolean isRunning() throws RunnerException;

    /**
     * Reports whether process was started and stopped successfully.
     *
     * @throws RunnerException
     *         if an error occurs when try to check status of application
     */
    boolean isStopped() throws RunnerException;

    /**
     * Get application logger.
     *
     * @return application logger
     * @throws RunnerException
     *         if an error occurs when try to get logger of application
     */
    ApplicationLogger getLogger() throws RunnerException;

    /**
     * Get name of runner which owns this process.
     *
     * @return name of runner which owns this process
     */
    String getRunner();

    /** Get configuration of current process. */
    RunnerConfiguration getConfiguration();
}
