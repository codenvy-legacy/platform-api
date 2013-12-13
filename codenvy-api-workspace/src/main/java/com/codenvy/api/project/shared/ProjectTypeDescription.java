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

    public ProjectTypeDescription(ProjectType projectType, AttributeDescription... attributeDescriptions) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributeDescriptions = new LinkedHashMap<>();
        addCommonAttributes();
        setAttributeDescriptions(attributeDescriptions);
    }

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
        setAttributeDescription(new AttributeDescription("zipball_sources_url"));
    }

    /**
     * Get descriptions of all attributes defined for the project type. Modifications to the returned {@code List} will not affect the
     * internal state.
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
    public void setAttributeDescriptions(AttributeDescription... elements) {
        if (elements != null && elements.length > 0) {
            for (AttributeDescription attributeDescription : elements) {
                this.attributeDescriptions.put(attributeDescription.getName(), attributeDescription);
            }
        }
    }

    /** Set descriptions of attributes. New descriptions will override exited description with the same name. */
    public void setAttributeDescriptions(List<AttributeDescription> list) {
        if (!(list == null || list.isEmpty())) {
            for (AttributeDescription attributeDescription : list) {
                this.attributeDescriptions.put(attributeDescription.getName(), attributeDescription);
            }
        }
    }
}