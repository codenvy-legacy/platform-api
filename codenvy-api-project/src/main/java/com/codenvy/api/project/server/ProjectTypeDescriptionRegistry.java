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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    public void registerDescription(ProjectTypeDescriptionExtension extension) {
        for (ProjectType type : extension.getProjectTypes()) {
            if (!projectTypeRegistry.isProjectTypeRegistered(type)) {
                projectTypeRegistry.registerProjectType(type);
            }
            descriptions.put(type.getId(), new ProjectTypeDescription(type, extension.getAttributeDescriptions()));
        }
    }

    public ProjectTypeDescription unregisterDescription(ProjectType type) {
        predefinedAttributes.remove(type.getId());
        templates.remove(type.getId());
        return descriptions.remove(type.getId());
    }

    public ProjectTypeDescription getDescription(ProjectType type) {
        final ProjectTypeDescription typeDescription = descriptions.get(type.getId());
        if (typeDescription != null) {
            return typeDescription;
        }
        return new ProjectTypeDescription(type);
    }

    public List<Attribute> getPredefinedAttributes(ProjectType type) {
        final List<Attribute> attributes = predefinedAttributes.get(type.getId());
        if (attributes != null) {
            return Collections.unmodifiableList((attributes));
        }
        return Collections.emptyList();
    }

    public List<ProjectTypeDescription> getDescriptions() {
        return new ArrayList<>(descriptions.values());
    }

    public List<ProjectTemplateDescription> getTemplates(ProjectType type) {
        final List<ProjectTemplateDescription> templates = this.templates.get(type.getId());
        if (templates != null) {
            return Collections.unmodifiableList((templates));
        }
        return Collections.emptyList();
    }
}