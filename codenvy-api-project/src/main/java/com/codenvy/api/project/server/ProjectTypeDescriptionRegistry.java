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

import com.codenvy.api.project.shared.Constants;
import com.google.inject.name.Named;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
    private final String                                        iconsBaseUrl;
    private final Map<String, List<AttributeDescription>>       attributeDescriptions;
    private final Map<String, List<Attribute>>                  predefinedAttributes;
    private final Map<String, List<ProjectTemplateDescription>> templates;
    private final Map<String, Map<String, String>>              iconRegistry;
    private final Map<String, Builders>                         builders;
    private final Map<String, Runners>                          runners;

    @Inject
    public ProjectTypeDescriptionRegistry(@Named("project.base_icon_url") String iconsBaseUrl) {
        projectTypeRegistry = new ProjectTypeRegistry();
        if (!iconsBaseUrl.endsWith("/")) {
            iconsBaseUrl = iconsBaseUrl + "/";
        }
        this.iconsBaseUrl = iconsBaseUrl;
        attributeDescriptions = new ConcurrentHashMap<>();
        predefinedAttributes = new ConcurrentHashMap<>();
        templates = new ConcurrentHashMap<>();
        iconRegistry = new ConcurrentHashMap<>();
        builders = new ConcurrentHashMap<>();
        runners = new ConcurrentHashMap<>();
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
        final Map<String, String> iconRegistry = extension.getIconRegistry();
        if (iconRegistry != null && !iconRegistry.isEmpty()) {
            Map<String, String> iconRegistryWithUrl = new HashMap<>();
            for (String key : iconRegistry.keySet()) {
                String path = iconRegistry.get(key);
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                iconRegistryWithUrl.put(key, iconsBaseUrl + path);
            }
            this.iconRegistry.put(type.getId(), iconRegistryWithUrl);
        }
        final Builders extBuilders = extension.getBuilders();
        if (extBuilders != null) {
            builders.put(type.getId(), new Builders(extBuilders));
        }
        final Runners extRunners = extension.getRunners();
        if (extRunners != null) {
            runners.put(type.getId(), new Runners(extRunners));
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
            final ArrayList<AttributeDescription> attributeDescriptions = new ArrayList<>(extension.getAttributeDescriptions());
            //TODO: add this temporary until we don't have possibility to extend project type
            final List<String> attributeNames = new LinkedList<>();
            for (AttributeDescription attributeDescription : attributeDescriptions) {
                attributeNames.add(attributeDescription.getName());
            }
            if (!attributeNames.contains(Constants.VCS_PROVIDER_NAME)) {
                attributeDescriptions.add(new AttributeDescription(Constants.VCS_PROVIDER_NAME));
            }
            this.attributeDescriptions.put(type.getId(), attributeDescriptions);

        }
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
        predefinedAttributes.remove(typeId);
        attributeDescriptions.remove(typeId);
        templates.remove(typeId);
        iconRegistry.remove(typeId);
        builders.remove(typeId);
        runners.remove(typeId);
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
     * Gets unmodifiable list of attribute descriptions for specified {@code ProjectType}.
     *
     * @param type
     *         {@code ProjectType} for which need to get attribute descriptions
     * @return unmodifiable list of attribute descriptions for specified {@code ProjectType}
     */
    public List<AttributeDescription> getAttributeDescriptions(ProjectType type) {
        final List<AttributeDescription> attributeDescription = attributeDescriptions.get(type.getId());
        if (attributeDescription != null) {
            return Collections.unmodifiableList((attributeDescription));
        }
        return Collections.emptyList();
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
     * Gets unmodifiable map of of registered project icons.
     *
     * @return unmodifiable map of registered project icons.
     */
    public Map<String, String> getIconRegistry(ProjectType type) {
        Map<String, String> iconRegistry = this.iconRegistry.get(type.getId());
        if (iconRegistry != null) {
            return Collections.unmodifiableMap(iconRegistry);
        }
        return Collections.emptyMap();
    }

    /**
     * Gets builder configuration by project type. This builder configuration may be used for new project if project doesn't contains own
     * builder configuration.
     */
    public Builders getBuilders(ProjectType projectType) {
        final Builders myBuilders = builders.get(projectType.getId());
        if (myBuilders == null) {
            return null;
        }
        // Return copy outside to avoid update instance that can impact all project of particular type.
        return new Builders(myBuilders);
    }

    /**
     * Gets runner configuration by project type. This runner configuration may be used for new project if project doesn't contains own
     * runner configuration.
     */
    public Runners getRunners(ProjectType projectType) {
        final Runners myRunners = runners.get(projectType.getId());
        if (myRunners == null) {
            return null;
        }
        // Return copy outside to avoid update instance that can impact all project of particular type.
        return new Runners(myRunners);
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
         * Registers new project type. Identifier returned by method {@link ProjectType#getId()} is used as unique key. If ProjectType with
         * the same identifier already registered it will be overwritten.
         *
         * @param type
         *         ProjectType
         * @see ProjectType#getId()
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