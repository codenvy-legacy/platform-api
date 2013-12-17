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
package com.codenvy.api.core.util;

import com.codenvy.api.core.config.SingletonConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Helps to find free ports.
 * Usage:
 * <pre>
 *     CustomPortService portService = ...
 *     int free = portService.acquire();
 *     if (free < 0) {
 *         // No free ports.
 *     } else {
 *         try {
 *             // Do something.
 *         } finally {
 *             portService.release(free);
 *         }
 *     }
 * </pre>
 * <p/>
 * Note: It is important to release port when it is not needed any more, otherwise it will be not possible to reuse ports.
 *
 * @author andrew00x
 * @see #MIN_PORT
 * @see #MAX_PORT
 */
@Singleton
public class CustomPortService {
    /** Name of configuration parameter that sets min port number. Default value is 49152. */
    public static final String MIN_PORT = "sys.resources.min_port";
    /** Name of configuration parameter that sets max port number. Default value is 65535. */
    public static final String MAX_PORT = "sys.resources.max_port";

    private static final Logger LOG = LoggerFactory.getLogger(CustomPortService.class);

    private static class CustomPortServiceHolder {
        static final CustomPortService INSTANCE = new CustomPortService(Pair.of(SingletonConfiguration.get().getInt(MIN_PORT, 49152),
                                                                                SingletonConfiguration.get().getInt(MAX_PORT, 65535)));
    }

    public static CustomPortService getInstance() {
        return CustomPortServiceHolder.INSTANCE;
    }

    private final ConcurrentMap<Integer, Boolean> portsInUse;
    private final Pair<Integer, Integer>          range;

    private CustomPortService(Pair<Integer, Integer> range) {
        if (range.first < 0 || range.second > 65535) {
            throw new IllegalArgumentException(String.format("Invalid port range: [%d:%d]", range.first, range.second));
        }
        this.range = range;
        portsInUse = new ConcurrentHashMap<>();
    }

    /**
     * This service stores allocated ports in internal storage to avoid checking ports that already in use. After calling this method
     * storage is cleared. For next port allocation this service will iterates through range of configured ports until finds free port.
     * It may be expensive since checking port means trying to open {@link ServerSocket} and {@link DatagramSocket} on each port in the
     * range.
     *
     * @see #MIN_PORT
     * @see #MAX_PORT
     */
    public void reset() {
        portsInUse.clear();
    }

    /**
     * Returns range of ports that service uses for lookup free port. Modifications to the returned {@code Pair} will not affect the
     * internal {@code Pair}.
     */
    public Pair<Integer, Integer> getRange() {
        return Pair.of(range.first, range.second);
    }

    /**
     * Get free port from the whole range of possible ports.
     *
     * @return free port or {@code -1} if there is no free port
     */
    public int acquire() {
        return doAcquire(range.first, range.second);
    }

    /**
     * Get free port from the specified range. Specified range may not be wider than configured range otherwise IllegalArgumentException is
     * thrown. Configured range may be checked with method {@link #getRange()}.
     *
     * @return free port or {@code -1} if there is no free port
     * @throws IllegalArgumentException
     *         if {@code min > range.first} or if {@code min > range.second}
     * @see #getRange()
     * @see #MIN_PORT
     * @see #MAX_PORT
     */
    public int acquire(int min, int max) {
        if (min < range.first) {
            throw new IllegalArgumentException(String.format("Min port value may not be less than %d", range.first));
        }
        if (max > range.second) {
            throw new IllegalArgumentException(String.format("Max port value may not be greater than %d", range.second));
        }
        return doAcquire(min, max);
    }

    public void release(int port) {
        if (port != -1) {
            portsInUse.remove(port);
        }
    }

    private int doAcquire(int min, int max) {
        for (int port = min; port <= max; port++) {
            if (portsInUse.putIfAbsent(port, Boolean.TRUE) == null) {
                ServerSocket ss = null;
                DatagramSocket ds = null;
                try {
                    ss = new ServerSocket(port);
                    ds = new DatagramSocket(port);
                    LOG.debug("Connect on port {}", port);
                    return port;
                } catch (IOException ignored) {
                    portsInUse.remove(port);
                } finally {
                    if (ds != null) {
                        ds.close();
                    }
                    if (ss != null) {
                        try {
                            ss.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
        return -1;
    }
}
