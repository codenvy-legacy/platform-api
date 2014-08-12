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
package com.codenvy.api.project.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of type of project.
 *
 * @author andrew00x
 */
public class ProjectTypeDescription {
    private final ProjectType                       projectType;
    private final Map<String, AttributeDescription> attributeDescriptions;

    public ProjectTypeDescription(ProjectType projectType, List<AttributeDescription> attributeDescriptions) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributeDescriptions = new LinkedHashMap<>();
        addCommonAttributes();
        setAttributeDescriptions(attributeDescriptions);
    }

    public ProjectTypeDescription(ProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributeDescriptions = new LinkedHashMap<>();
        addCommonAttributes();
    }

    /** @return Project type */
    public ProjectType getProjectType() {
        return projectType;
    }

    // Probably temporary solution for adding common attributes that are applicable for any type of project.
    protected void addCommonAttributes() {
        setAttributeDescription(new AttributeDescription("runner.user_defined_launcher"));
        setAttributeDescription(new AttributeDescription("runner.run_scripts"));
        setAttributeDescription(new AttributeDescription("vcs.provider.name"));
        // TODO: Remove. Added temporary, for back compatibility after change format of .codenvy/project.json file
        setAttributeDescription(new AttributeDescription("builder.name"));
        setAttributeDescription(new AttributeDescription("runner.name"));
    }

    /**
     * Get descriptions of all attributes that are defined for the project type. Modifications to the returned {@code List} will not affect
     * the internal state.
     *
     * @return descriptions of attributes
     * @see Attribute
     * @see AttributeDescription
     */
    public List<AttributeDescription> getAttributeDescriptions() {
        return new ArrayList<>(attributeDescriptions.values());
    }


    /** Get unmodifiable list of descriptions of attributes of project which names are started with specified prefix. */
    public List<AttributeDescription> getAttributeDescriptions(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.unmodifiableList(getAttributeDescriptions());
        }
        final List<AttributeDescription> result = new ArrayList<>();
        for (Map.Entry<String, AttributeDescription> entry : attributeDescriptions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Get single attribute of project with specified name. */
    public AttributeDescription getAttributeDescription(String name) {
        return attributeDescriptions.get(name);
    }

    public boolean hasAttributeDescription(String name) {
        return attributeDescriptions.get(name) != null;
    }

    /** Set single description of attribute. New description will override exited description with the same name. */
    public void setAttributeDescription(AttributeDescription attributeDescription) {
        attributeDescriptions.put(attributeDescription.getName(), attributeDescription);
    }

    /** Set descriptions of attributes. New descriptions will override exited description with the same name. */
    public void setAttributeDescriptions(List<AttributeDescription> list) {
        if (!(list == null || list.isEmpty())) {
            for (AttributeDescription attributeDescription : list) {
                this.attributeDescriptions.put(attributeDescription.getName(), attributeDescription);
            }
        }
    }

    @Override
    public String toString() {
        return "ProjectTypeDescription{" +
               "projectType=" + projectType +
               ", attributeDescriptions=" + attributeDescriptions +
               '}';
    }
}