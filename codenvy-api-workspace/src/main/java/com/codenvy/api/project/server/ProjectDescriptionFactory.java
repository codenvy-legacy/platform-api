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

import com.codenvy.api.core.util.ComponentLoader;
import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
@Singleton
public class ProjectDescriptionFactory {
    private final ProjectTypeRegistry        projectTypeRegistry;
    private final List<ValueProviderFactory> valueProviderFactories;

    public ProjectDescriptionFactory(ProjectTypeRegistry projectTypeRegistry) {
        this.projectTypeRegistry = projectTypeRegistry;
        // TODO: rework with IoC
        valueProviderFactories = new ArrayList<>(ComponentLoader.all(ValueProviderFactory.class));
    }

    public PersistentProjectDescription getDescription(Project project) {
        final ProjectType projectType = getProjectType(project);
        final List<Attribute> attributes = getAttributes(projectType, project);
        return new PersistentProjectDescription(projectType, attributes);
    }

    protected ProjectType getProjectType(Project project) {
        for (Property property : project.getProperties()) {
            if ("vfs:projectType".equals(property.getName())) {
                final List<String> value = property.getValue();
                if (!(value == null || value.isEmpty())) {
                    return projectTypeRegistry.getProjectType(value.get(0));
                }
            }
        }
        return null;
    }

    protected List<Attribute> getAttributes(ProjectType projectType, Project project) {
        final Map<String, Attribute> attributes = new LinkedHashMap<>();
        for (ValueProviderFactory factory : valueProviderFactories) {
            if (factory.isApplicable(projectType)) {
                final String attributeName = factory.getName();
                attributes.put(attributeName, new Attribute(attributeName, factory.newInstance(project)));
            }
        }
        for (Property property : project.getProperties()) {
            final String attributeName = property.getName();
            if (!attributes.containsKey(attributeName)) {
                attributes.put(attributeName, new Attribute(attributeName, new VfsPropertyValueProvider(property.getValue())));
            }
        }
        return new ArrayList<>(attributes.values());
    }
}
