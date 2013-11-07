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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class RunnerRegistry {
    private final Map<String, Runner> runners;

    public RunnerRegistry() {
        runners = new ConcurrentHashMap<>();
    }

    /**
     * TODO
     */
    public void add(Runner myRunner) {
        runners.put(myRunner.getName(), myRunner);
    }

    /**
     * Get {@code Runner} by its name.
     *
     * @param name
     *         name
     * @return {@code Runner} or {@code null} if there is no such {@code Runner}
     */
    public Runner get(String name) {
        return runners.get(name);
    }

    /**
     * Remove {@code Runner} by its name.
     *
     * @param name
     *         name
     * @return {@code Runner} or {@code null} if there is no such {@code Runner}
     */
    public Runner remove(String name) {
        return runners.get(name);
    }

    /**
     * Get all available runners. Modifications to the returned {@code Set} will not affect the internal state of {@code RunnerRegistry}.
     *
     * @return all available runners
     */
    public Set<Runner> getAll() {
        return new LinkedHashSet<>(runners.values());
    }

    public void clear() {
        runners.clear();
    }
}
