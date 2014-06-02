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

import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores available builders.
 *
 * @author andrew00x
 */
@Singleton
public class BuilderRegistry {
    private final Map<String, Builder> builders;

    public BuilderRegistry() {
        builders = new ConcurrentHashMap<>();
    }

    /**
     * Add {@code Builder}. Uses {@code String} returned by method {@code Builder.getName()} as builder's identifier. If {@code Builder}
     * with the same name already registered it is replaced by new one.
     *
     * @param builder
     *         Builder
     */
    public void add(Builder builder) {
        builders.put(builder.getName(), builder);
    }

    /**
     * Get {@code Builder} by its name.
     *
     * @param name
     *         name
     * @return {@code Builder} or {@code null} if there is no such {@code Builder}
     */
    public Builder get(String name) {
        if (name == null) {
            return null;
        }
        return builders.get(name);
    }

    /**
     * Remove {@code Builder} by its name.
     *
     * @param name
     *         name
     * @return {@code Builder} or {@code null} if there is no such {@code Builder}
     */
    public Builder remove(String name) {
        if (name == null) {
            return null;
        }
        return builders.remove(name);
    }

    /**
     * Get all available builders. Modifications to the returned {@code Set} will not affect the internal state of {@code BuilderRegistry}.
     *
     * @return all available builders
     */
    public Set<Builder> getAll() {
        return new LinkedHashSet<>(builders.values());
    }

    public void clear() {
        builders.clear();
    }
}
