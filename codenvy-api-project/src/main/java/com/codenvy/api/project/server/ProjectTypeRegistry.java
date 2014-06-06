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

/** @author andrew00x */
@Singleton
public class ProjectTypeRegistry {
    private final Map<String, ProjectType> types;

    @Inject
    public ProjectTypeRegistry() {
        types = new ConcurrentHashMap<>();
    }

    public void registerProjectType(ProjectType type) {
        types.put(type.getId(), type);
    }

    public ProjectType unregisterProjectType(String id) {
        if (id == null) {
            return null;
        }
        return types.remove(id);
    }

    public ProjectType getProjectType(String id) {
        if (id == null) {
            return null;
        }
        return types.get(id);
    }

    public boolean isProjectTypeRegistered(String id) {
        return id != null && types.get(id) != null;
    }

    public boolean isProjectTypeRegistered(ProjectType type) {
        return types.get(type.getId()) != null;
    }

    public List<ProjectType> getRegisteredTypes() {
        return new ArrayList<>(types.values());
    }
}
