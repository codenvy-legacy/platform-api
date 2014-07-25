/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.ProjectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores information about registered (known) project types.
 *
 * @author andrew00x
 */
@Singleton
public class ProjectTypeRegistry {
    private final Map<String, ProjectType> types;

    @Inject
    public ProjectTypeRegistry() {
        types = new ConcurrentHashMap<>();
    }

    /**
     * Registers new project type. Identifier returned by method {@link ProjectType#getId()} is used as unique key. If ProjectType with the
     * same identifier already registered it will be overwritten.
     *
     * @param type
     *         ProjectType
     * @see ProjectType#getId()
     */
    public void registerProjectType(ProjectType type) {
        types.put(type.getId(), type);
    }

    /**
     * Removes ProjectType from this registry.
     *
     * @param id
     *         project type's id
     * @return removed ProjectType or {@code null} if ProjectType with specified {@code id} isn't registered
     */
    public ProjectType unregisterProjectType(String id) {
        if (id == null) {
            return null;
        }
        return types.remove(id);
    }

    /**
     * Gets ProjectType by id.
     *
     * @param id
     *         project type's id
     * @return ProjectType or {@code null} if ProjectType with specified {@code id} isn't registered
     */
    public ProjectType getProjectType(String id) {
        if (id == null) {
            return null;
        }
        return types.get(id);
    }

    /**
     * Tests whether ProjectType with specified id is registered.
     *
     * @param id
     *         project type's id
     * @return {@code true} if ProjectType with specified {@code id} is registered and {@code false} otherwise
     */
    public boolean isProjectTypeRegistered(String id) {
        return id != null && types.get(id) != null;
    }

    /**
     * Tests whether specified ProjectType is registered.
     *
     * @param type
     *         project type
     * @return {@code true} if ProjectType is registered and {@code false} otherwise
     */
    public boolean isProjectTypeRegistered(ProjectType type) {
        return types.get(type.getId()) != null;
    }

    /**
     * Gets all registered project types. Modifications to the returned {@code List} will not affect the internal state of {@code
     * ProjectTypeRegistry}.
     *
     * @return registered project types
     */
    public List<ProjectType> getRegisteredTypes() {
        return new ArrayList<>(types.values());
    }
}
