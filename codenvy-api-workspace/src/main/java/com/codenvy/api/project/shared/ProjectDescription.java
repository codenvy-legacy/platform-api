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
 * Description of Project. Project description filled in a course of project creating and stored with a Project.
 *
 * @author gazarenkov
 */
public class ProjectDescription {
    private ProjectType projectType;

    private final Map<String, Attribute> attributes;

    public ProjectDescription(ProjectType projectType, Attribute... attributes) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributes = new LinkedHashMap<>();
        setAttributes(attributes);
    }

    public ProjectDescription(ProjectType projectType, List<Attribute> attributes) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributes = new LinkedHashMap<>();
        setAttributes(attributes);
    }

    public ProjectDescription(ProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
        this.attributes = new LinkedHashMap<>();
    }

    /** @return Project type */
    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    /**
     * Get all attributes of project. Modifications to the returned {@code List} will not affect the internal state.
     * <p/>
     * Note: attributes are stored within the project as a combination of persisted properties and "implicit" metainfo inside the project
     *
     * @return attributes
     * @see Attribute
     */
    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    /** Get unmodifiable list of attributes of project which names are started with specified prefix. */
    public List<Attribute> getAttributes(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.unmodifiableList(getAttributes());
        }
        final List<Attribute> result = new ArrayList<>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Get single attribute of project with specified name. */
    public Attribute getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.get(name) != null;
    }

    /** Set single attribute. New attribute will override exited attribute with the same name. */
    public void setAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    /** Set attributes. New attributes will override exited attributes with the same names. */
    public void setAttributes(Attribute... elements) {
        if (elements != null && elements.length > 0) {
            for (Attribute attribute : elements) {
                this.attributes.put(attribute.getName(), attribute);
            }
        }
    }

    /** Set attributes. New attributes will override exited attributes with the same names. */
    public void setAttributes(List<Attribute> list) {
        if (!(list == null || list.isEmpty())) {
            for (Attribute attribute : list) {
                this.attributes.put(attribute.getName(), attribute);
            }
        }
    }
}