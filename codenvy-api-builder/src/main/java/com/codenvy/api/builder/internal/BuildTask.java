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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.util.CommandLine;

/**
 * Build task abstraction.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface BuildTask {

    /** Will be notified when processing of {@code BuildTask} is done (successfully, failed or cancelled). */
    interface Callback {
        void done(BuildTask task);
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    Long getId();

    /**
     * Get command line which this task runs. Modifications to the returned {@code CommandLine} will not affect the task it it already
     * started. Caller always must check is task is started before use this method.
     *
     * @return command line
     * @see #isStarted()
     */
    CommandLine getCommandLine();

    /**
     * Get name of builder which owns this task.
     *
     * @return name of builder which owns this task
     */
    String getBuilder();

    /**
     * Get build logger.
     *
     * @return build logger
     */
    BuildLogger getBuildLogger();

    /**
     * Reports whether build task is started or not.
     *
     * @return {@code true} if task is started and {@code false} otherwise
     * @throws BuilderException
     *         if an error occurs when try to check status of build process
     */
    boolean isStarted() throws BuilderException;

    /**
     * Get time when task was started.
     *
     * @return time when task was started or {@code -1} if task is not started yet
     * @throws BuilderException
     *         if an error occurs when try to check status of build process
     * @see #isStarted()
     */
    long getStartTime() throws BuilderException;

    /**
     * Reports whether build task is done (successfully ends, fails, cancelled) or not.
     *
     * @return {@code true} if task is done and {@code false} otherwise
     * @throws BuilderException
     *         if an error occurs when try to check status of build process
     */
    boolean isDone() throws BuilderException;

    /**
     * Reports that the process was interrupted.
     *
     * @return {@code true} if task was interrupted and {@code false} otherwise
     * @throws BuilderException
     *         if an error occurs when try to check status of build process
     */
    boolean isCancelled() throws BuilderException;

    /**
     * Interrupt build process.
     *
     * @throws BuilderException
     *         if an error occurs when try to interrupt build process
     */
    void cancel() throws BuilderException;

    /**
     * Get build result.
     *
     * @return build result or {@code null} if task is not done yet
     * @throws BuilderException
     *         if an error occurs when try to start build process or get its result.
     *         <p/>
     *         <strong>Note</strong> Throwing of this exception is typically should not be related to failed build process itself. Builder
     *         should always provide result of build process with BuildResult instance. Throwing of this exception means something going
     *         wrong with build system itself and it is not possible to start build process or getting result of a build
     */
    BuildResult getResult() throws BuilderException;

    /**
     * Get configuration of this task.
     *
     * @return configuration of this task
     */
    BuilderConfiguration getConfiguration();
}
