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

import com.codenvy.api.runner.RunnerException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Facade for application process.
 *
 * @author andrew00x
 */
public abstract class ApplicationProcess {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long id;

    public ApplicationProcess() {
        this.id = sequence.getAndIncrement();
    }

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    public final Long getId() {
        return id;
    }

    /**
     * Starts application process.
     *
     * @throws RunnerException
     *         if an error occurs when start process
     * @throws IllegalStateException
     *         if process is already started
     */
    public abstract void start() throws RunnerException;

    /**
     * Stops application process.
     *
     * @throws RunnerException
     *         if an error occurs when stop process
     * @throws IllegalStateException
     *         if process isn't started yet
     */
    public abstract void stop() throws RunnerException;

    /**
     * Wait, if necessary, until this process stops, then returns exit code.
     *
     * @throws IllegalStateException
     *         if process isn't started yet
     * @throws RunnerException
     *         if any other error occurs
     */
    public abstract int waitFor() throws RunnerException;

    /**
     * Get exit code of application process. Returns {@code -1} if application is not started or still running.
     *
     * @throws RunnerException
     *         if an error occurs when try getting process' exit code
     */
    public abstract int exitCode() throws RunnerException;

    /**
     * Reports whether application process is running or not.
     *
     * @throws RunnerException
     *         if an error occurs when try getting process' status
     */
    public abstract boolean isRunning() throws RunnerException;

    /** Get application logger. */
    public abstract ApplicationLogger getLogger() throws RunnerException;
}
