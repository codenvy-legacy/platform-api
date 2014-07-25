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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ProjectTypeRegistry                           projectTypeRegistry;
    private final Map<String, ProjectTypeDescription>           descriptions;
    private final Map<String, List<Attribute>>                  predefinedAttributes;
    private final Map<String, List<ProjectTemplateDescription>> templates;

    @Inject
    public ProjectTypeDescriptionRegistry(ProjectTypeRegistry projectTypeRegistry) {
        this.projectTypeRegistry = projectTypeRegistry;
        descriptions = new ConcurrentHashMap<>();
        predefinedAttributes = new ConcurrentHashMap<>();
        templates = new ConcurrentHashMap<>();
    }

    /**
     * Registers project type with ProjectTypeExtension.
     *
     * @param extension
     *         ProjectTypeExtension
     * @see com.codenvy.api.project.server.ProjectTypeExtension
     */
    public void registerProjectType(ProjectTypeExtension extension) {
        final ProjectType type = extension.getProjectType();
        if (!projectTypeRegistry.isProjectTypeRegistered(type)) {
            projectTypeRegistry.registerProjectType(type);
        }
        final List<Attribute> typePredefinedAttributes = extension.getPredefinedAttributes();
        if (!(typePredefinedAttributes == null || typePredefinedAttributes.isEmpty())) {
            predefinedAttributes.put(type.getId(), new ArrayList<>(typePredefinedAttributes));
        }
        final List<ProjectTemplateDescription> templates = extension.getTemplates();
        if (templates != null && !templates.isEmpty()) {
            this.templates.put(type.getId(), new ArrayList<>(templates));
        }
    }

    /**
     * Registers project type with ProjectTypeDescriptionExtension.
     *
     * @param extension
     *         ProjectTypeDescriptionExtension
     * @see com.codenvy.api.project.server.ProjectTypeDescriptionExtension
     */
    public void registerDescription(ProjectTypeDescriptionExtension extension) {
        for (ProjectType type : extension.getProjectTypes()) {
            if (!projectTypeRegistry.isProjectTypeRegistered(type)) {
                projectTypeRegistry.registerProjectType(type);
            }
            descriptions.put(type.getId(), new ProjectTypeDescription(type, extension.getAttributeDescriptions()));
        }
    }

    /**
     * Removes ProjectTypeDescription.
     *
     * @param type
     *         {@code ProjectType} for which need to remove {@code ProjectTypeDescription}
     * @return removed ProjectTypeDescription or {@code null} if ProjectTypeDescription isn't registered
     */
    public ProjectTypeDescription unregisterDescription(ProjectType type) {
        predefinedAttributes.remove(type.getId());
        templates.remove(type.getId());
        return descriptions.remove(type.getId());
    }

    /**
     * Gets ProjectTypeDescription.
     *
     * @param type
     *         {@code ProjectType} for which need to get {@code ProjectTypeDescription}
     * @return ProjectTypeDescription or {@code null} if ProjectTypeDescription isn't registered
     */
    public ProjectTypeDescription getDescription(ProjectType type) {
        final ProjectTypeDescription typeDescription = descriptions.get(type.getId());
        if (typeDescription != null) {
            return typeDescription;
        }
        return new ProjectTypeDescription(type);
    }

    /**
     * Gets unmodifiable list of predefined attributes for specified {@code ProjectType}.
     *
     * @param type
     *         {@code ProjectType} for which need to get predefined attributes
     * @return unmodifiable list of predefined attributes for specified {@code ProjectType}
     */
    public List<Attribute> getPredefinedAttributes(ProjectType type) {
        final List<Attribute> attributes = predefinedAttributes.get(type.getId());
        if (attributes != null) {
            return Collections.unmodifiableList((attributes));
        }
        return Collections.emptyList();
    }

    /**
     * Gets all registered project type descriptions. Modifications to the returned {@code List} will not affect the internal state of
     * {@code ProjectTypeDescriptionRegistry}.
     *
     * @return registered project type descriptions
     */
    public List<ProjectTypeDescription> getDescriptions() {
        return new ArrayList<>(descriptions.values());
    }

    /**
     * Gets unmodifiable list of registered project templates descriptions.
     *
     * @return unmodifiable list of registered project templates descriptions
     */
    public List<ProjectTemplateDescription> getTemplates(ProjectType type) {
        final List<ProjectTemplateDescription> templates = this.templates.get(type.getId());
        if (templates != null) {
            return Collections.unmodifiableList((templates));
        }
        return Collections.emptyList();
    }
}