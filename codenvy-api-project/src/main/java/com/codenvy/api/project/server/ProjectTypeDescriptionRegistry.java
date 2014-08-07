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
    public ProjectTypeDescriptionRegistry() {
        projectTypeRegistry = new ProjectTypeRegistry();
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
     * Registers new project type. Identifier returned by method {@link com.codenvy.api.project.shared.ProjectType#getId()} is used as
     * unique key. If ProjectType with the same identifier already registered it will be overwritten.
     *
     * @param type
     *         ProjectType
     * @see com.codenvy.api.project.shared.ProjectType#getId()
     */
    public void registerProjectType(ProjectType type) {
        projectTypeRegistry.registerProjectType(type);
    }

    /**
     * Removes ProjectType from this registry.
     *
     * @param typeId
     *         project type's id
     * @return removed ProjectType or {@code null} if ProjectType with specified {@code id} isn't registered
     */
    public ProjectType unregisterProjectType(String typeId) {
        unregisterDescription(typeId);
        return projectTypeRegistry.unregisterProjectType(typeId);
    }

    /**
     * Gets ProjectType by id.
     *
     * @param typeId
     *         project type's id
     * @return ProjectType or {@code null} if ProjectType with specified {@code id} isn't registered
     */
    public ProjectType getProjectType(String typeId) {
        return projectTypeRegistry.getProjectType(typeId);
    }

    /**
     * Tests whether ProjectType with specified id is registered.
     *
     * @param typeId
     *         project type's id
     * @return {@code true} if ProjectType with specified {@code id} is registered and {@code false} otherwise
     */
    public boolean isProjectTypeRegistered(String typeId) {
        return projectTypeRegistry.isProjectTypeRegistered(typeId);
    }

    /**
     * Tests whether specified ProjectType is registered.
     *
     * @param type
     *         project type
     * @return {@code true} if ProjectType is registered and {@code false} otherwise
     */
    public boolean isProjectTypeRegistered(ProjectType type) {
        return projectTypeRegistry.isProjectTypeRegistered(type);
    }

    /**
     * Gets all registered project types. Modifications to the returned {@code List} will not affect the internal state of {@code
     * ProjectTypeRegistry}.
     *
     * @return registered project types
     */
    public List<ProjectType> getRegisteredTypes() {
        return projectTypeRegistry.getRegisteredTypes();
    }

    /**
     * Removes ProjectTypeDescription.
     *
     * @param type
     *         {@code ProjectType} for which need to remove {@code ProjectTypeDescription}
     * @return removed ProjectTypeDescription or {@code null} if ProjectTypeDescription isn't registered
     */
    public ProjectTypeDescription unregisterDescription(ProjectType type) {
        return unregisterDescription(type.getId());
    }

    private ProjectTypeDescription unregisterDescription(String typeId) {
        predefinedAttributes.remove(typeId);
        templates.remove(typeId);
        return descriptions.remove(typeId);
    }

    /**
     * Gets ProjectTypeDescription.
     *
     * @param type
     *         {@code ProjectType} for which need to get {@code ProjectTypeDescription}
     * @return ProjectTypeDescription or {@code null} if ProjectTypeDescription isn't registered
     */
    public ProjectTypeDescription getDescription(ProjectType type) {
        return descriptions.get(type.getId());
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

    /**
     * Stores information about registered (known) project types.
     */
    static class ProjectTypeRegistry {
        private final Map<String, ProjectType> types;

        ProjectTypeRegistry() {
            types = new ConcurrentHashMap<>();
        }

        /**
         * Registers new project type. Identifier returned by method {@link com.codenvy.api.project.shared.ProjectType#getId()} is used as
         * unique key. If ProjectType with the same identifier already registered it will be overwritten.
         *
         * @param type
         *         ProjectType
         * @see com.codenvy.api.project.shared.ProjectType#getId()
         */
        void registerProjectType(ProjectType type) {
            types.put(type.getId(), type);
        }

        /**
         * Removes ProjectType from this registry.
         *
         * @param id
         *         project type's id
         * @return removed ProjectType or {@code null} if ProjectType with specified {@code id} isn't registered
         */
        ProjectType unregisterProjectType(String id) {
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
        ProjectType getProjectType(String id) {
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
        boolean isProjectTypeRegistered(String id) {
            return id != null && types.get(id) != null;
        }

        /**
         * Tests whether specified ProjectType is registered.
         *
         * @param type
         *         project type
         * @return {@code true} if ProjectType is registered and {@code false} otherwise
         */
        boolean isProjectTypeRegistered(ProjectType type) {
            return types.get(type.getId()) != null;
        }

        /**
         * Gets all registered project types. Modifications to the returned {@code List} will not affect the internal state of {@code
         * ProjectTypeRegistry}.
         *
         * @return registered project types
         */
        List<ProjectType> getRegisteredTypes() {
            return new ArrayList<>(types.values());
        }
    }
}