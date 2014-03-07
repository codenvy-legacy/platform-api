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
