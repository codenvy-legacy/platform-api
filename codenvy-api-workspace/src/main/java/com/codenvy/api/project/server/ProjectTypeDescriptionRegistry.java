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
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.project.shared.ProjectTypeDescriptionExtension;
import com.codenvy.api.project.shared.ProjectTypeExtension;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectDescriptionRegistry
 *
 * @author gazarenkov
 */
@Singleton
public class ProjectTypeDescriptionRegistry {
    private final ProjectTypeRegistry                 projectTypeRegistry;
    private final Map<String, ProjectTypeDescription> descriptions;

    public ProjectTypeDescriptionRegistry(ProjectTypeRegistry projectTypeRegistry) {
        this.projectTypeRegistry = projectTypeRegistry;
        descriptions = new ConcurrentHashMap<>();
    }

    public void registerProjectType(ProjectTypeExtension extension) {
        final ProjectType projectType = extension.getProjectType();
        descriptions.put(projectType.getId(), new ProjectTypeDescription(projectType, extension.getPredefinedAttributes()));
    }

    public void registerDescription(ProjectTypeDescriptionExtension extension) {
        for (ProjectType type : extension.getProjectTypes()) {
            if (!projectTypeRegistry.isProjectTypeRegistered(type)) {
                // TODO: type should be registered?
                projectTypeRegistry.registerProjectType(type);
            }
            descriptions.put(type.getId(), new ProjectTypeDescription(type, extension.getAttributes()));
        }
    }

    public ProjectTypeDescription unregisterProjectType(ProjectType type) {
        return descriptions.remove(type.getId());
    }

    public ProjectTypeDescription getProjectType(ProjectType type) {
        return descriptions.get(type.getId());
    }

    public List<ProjectTypeDescription> getDescriptions() {
        return new ArrayList<>(descriptions.values());
    }
}