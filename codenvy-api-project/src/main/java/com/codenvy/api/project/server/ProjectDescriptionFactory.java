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
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
@Singleton
public class ProjectDescriptionFactory {
    private final ProjectTypeRegistry               projectTypeRegistry;
    private final ProjectTypeDescriptionRegistry    typeDescriptionRegistry;
    private final Map<String, ValueProviderFactory> valueProviderFactories;

    @Inject
    public ProjectDescriptionFactory(ProjectTypeRegistry projectTypeRegistry,
                                     ProjectTypeDescriptionRegistry typeDescriptionRegistry,
                                     Set<ValueProviderFactory> valueProviderFactories) {
        this.projectTypeRegistry = projectTypeRegistry;
        this.typeDescriptionRegistry = typeDescriptionRegistry;
        this.valueProviderFactories = new HashMap<>();
        for (ValueProviderFactory valueProviderFactory : valueProviderFactories) {
            this.valueProviderFactories.put(valueProviderFactory.getName(), valueProviderFactory);
        }
    }

    public PersistentProjectDescription getDescription(Project project) {
        final ProjectType projectType = getProjectType(project);
        final List<Attribute> attributes = getAttributes(projectType, project);
        return new PersistentProjectDescription(projectType, attributes);
    }

    protected ProjectType getProjectType(Project project) {
        ProjectType type = null;
        List<Property> properties = project.getProperties();
        for (int i = 0, size = properties.size(); i < size && type == null; i++) {
            Property property = properties.get(i);
            if ("vfs:projectType".equals(property.getName())) {
                final List<String> value = property.getValue();
                if (!(value == null || value.isEmpty())) {
                    type = projectTypeRegistry.getProjectType(value.get(0));
                }
            }
        }
        if (type == null) {
            type = new ProjectType("unknown", "unknown"); // TODO : Decide how should we treat such situation??
        }
        return type;
    }

    protected List<Attribute> getAttributes(ProjectType projectType, Project project) {
        final Map<String, Attribute> attributes = new LinkedHashMap<>();
        for (Property property : project.getProperties()) {
            final String propertyName = property.getName();
            attributes.put(propertyName, new Attribute(propertyName, new VfsPropertyValueProvider(propertyName, property.getValue())));
        }
        final ProjectTypeDescription typeDescription = typeDescriptionRegistry.getDescription(projectType);
        if (typeDescription != null) {
            for (AttributeDescription attributeDescription : typeDescription.getAttributeDescriptions()) {
                final String attributeName = attributeDescription.getName();
                if (!attributes.containsKey(attributeName)) {
                    final ValueProviderFactory factory = valueProviderFactories.get(attributeName);
                    if (factory != null) {
                        attributes.put(attributeName, new Attribute(attributeName, factory.newInstance(project)));
                    }
                }
            }
        }
        return new ArrayList<>(attributes.values());
    }
}
