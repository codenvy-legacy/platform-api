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
package com.codenvy.core.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * It controls the time of {@code Cancellable} invocation and if time if greater than timeout it terminates such {@code Cancellable}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class Watchdog implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Watchdog.class);

    private final String name;
    private final long   timeout;

    private boolean     watch;
    private Cancellable cancellable;

    /**
     * Create new {@code Watchdog}.
     *
     * @param name
     *         name for background {@code Thread}. It helps to identify out threads. This parameter is optional and may be {@code null}.
     * @param timeout
     *         timeout
     * @param unit
     *         timeout unit
     */
    public Watchdog(String name, long timeout, TimeUnit unit) {
        this.name = name;
        if (timeout < 1) {
            throw new IllegalArgumentException(String.format("Invalid timeout: %d", timeout));
        }
        this.timeout = unit.toMillis(timeout);
    }

    public Watchdog(long timeout, TimeUnit unit) {
        this(null, timeout, unit);
    }

    /**
     * Start watching {@code Cancellable}.
     *
     * @param cancellable
     *         Cancellable
     */
    public synchronized void start(Cancellable cancellable) {
        this.cancellable = cancellable;
        this.watch = true;
        final Thread t = name == null ? new Thread(this) : new Thread(this, name);
        t.setDaemon(true);
        t.start();
    }

    /** Stop watching. */
    public synchronized void stop() {
        watch = false;
        notify();
    }

    /** NOTE: Not expected to call directly by regular users of this class. */
    public synchronized void run() {
        final long end = System.currentTimeMillis() + timeout;
        long now;
        while (watch && (end > (now = System.currentTimeMillis()))) {
            try {
                wait(end - now);
            } catch (InterruptedException ignored) {
                // Not expected to be thrown
            }
        }
        if (watch) {
            try {
                cancellable.cancel();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            watch = false;
        }
    }
}
