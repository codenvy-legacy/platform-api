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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Helps to find free ports.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class CustomPortService {
    // TODO: make singleton!
    private static final Logger LOG = LoggerFactory.getLogger(CustomPortService.class);
    private final ConcurrentMap<Integer, Boolean> portsInUse;
    private       Pair<Integer, Integer>          range;

    public CustomPortService(int minPort, int maxPort) {
        this(Pair.of(minPort, maxPort));
    }

    public CustomPortService() {
        this(null);
    }

    private CustomPortService(Pair<Integer, Integer> range) {
        this.range = range;
        portsInUse = new ConcurrentHashMap<>();
    }

    /**
     * Get free port from the range specified in constructor of this class.
     *
     * @return free port or {@code -1} if there is no free port
     */
    public int acquire() {
        final Pair<Integer, Integer> range = this.range;
        if (range == null) {
            return -1;
        }
        for (int port = range.first; port <= range.second; port++) {
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

    public void release(int port) {
        portsInUse.remove(port);
    }

    public void setRange(int minPort, int maxPort) {
        range = Pair.of(minPort, maxPort);
    }

    public Pair<Integer, Integer> getRange() {
        final Pair<Integer, Integer> range = this.range;
        if (range == null) {
            return Pair.of(-1, -1);
        }
        return Pair.of(range.first, range.second);
    }
}
