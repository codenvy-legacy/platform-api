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
 * Description of Project. Project description filled in a course of project creating and stored with a Project.
 *
 * @author gazarenkov
 */
public class ProjectDescription {
    private ProjectType            projectType;
    private String                 description;
    private Map<String, Attribute> attributes;

    public ProjectDescription(ProjectType projectType, List<Attribute> attributes) {
        this.attributes = new LinkedHashMap<>();
        setProjectType(projectType);
        setAttributes(attributes);
    }

    public ProjectDescription(ProjectType projectType) {
        this.attributes = new LinkedHashMap<>();
        setProjectType(projectType);
    }

    public ProjectDescription() {
        this(new ProjectType("nameless", "nameless", "nameless"));
    }

    public ProjectDescription(ProjectDescription origin) {
        final ProjectType originProjectType = origin.getProjectType();
        setProjectType(new ProjectType(originProjectType.getId(), originProjectType.getName(), originProjectType.getCategory()));
        final List<Attribute> originAttributes = origin.getAttributes();
        final List<Attribute> copyAttributes = new ArrayList<>();
        for (Attribute attribute : originAttributes) {
            copyAttributes.add(new Attribute(attribute));
        }
        this.attributes = new LinkedHashMap<>();
        this.description = origin.getDescription();
        setAttributes(copyAttributes);
    }

    /** @return Project type */
    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
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

    /** Get single value of attribute with specified name. If attribute has multiple value then this method returns first value in the list. */
    public String getAttributeValue(String name) {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /** Get values of attribute with specified name. */
    public List<String> getAttributeValues(String name) {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValues();
    }

    /** Set attributes. New attributes will override exited attributes with the same names. */
    public void setAttributes(List<Attribute> list) {
        if (!(list == null || list.isEmpty())) {
            for (Attribute attribute : list) {
                attributes.put(attribute.getName(), attribute);
            }
        }
    }

    /** Set single attribute. New attribute will override exited attribute with the same name. */
    public void setAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    /** Set project type and attributes. New attributes will override exited attributes with the same names. */
    public void setProjectTypeAndAttributes(ProjectType projectType, List<Attribute> list) {
        setProjectType(projectType);
        setAttributes(list);
    }

    public Attribute removeAttribute(String name) {
        return attributes.remove(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}