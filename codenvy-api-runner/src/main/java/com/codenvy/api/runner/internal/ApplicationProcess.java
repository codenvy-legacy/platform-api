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
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public abstract class ApplicationProcess {
    public static interface Callback {
        void started(ApplicationProcess process);

        void stopped(ApplicationProcess process);

        void startError(Throwable error);

        void stopError(Throwable error);
    }

    private static final AtomicLong sequence = new AtomicLong(1);

    private final Callback callback;
    private final Long     id;

    public ApplicationProcess(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException();
        }
        this.callback = callback;
        this.id = sequence.getAndIncrement();
    }

    public final Long getId() {
        return id;
    }

    /** Starts application process. */
    public final void start() {
        try {
            doStart();
            callback.started(this);
        } catch (Throwable e) {
            callback.startError(e);
        }
    }

    protected abstract void doStart() throws Throwable;

    /** Stops application process. */
    public final void stop() {
        try {
            doStop();
            callback.stopped(this);
        } catch (Throwable e) {
            callback.stopError(e);
        }
    }

    protected abstract void doStop() throws Throwable;

    /** Get exit code of application process. Returns {@code -1} if application is not started or still running. */
    public abstract int exitCode() throws RunnerException;

    /** Reports whether application process is running or not. */
    public abstract boolean isRunning() throws RunnerException;

    /** Get application logger. */
    public abstract ApplicationLogger getLogger() throws RunnerException;
}
