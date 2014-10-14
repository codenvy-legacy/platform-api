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
    private Map<String, Attribute> attributes;
    private String                 description;
    private Builders               builders;
    private Runners                runners;

    public ProjectDescription(ProjectType projectType, List<Attribute> attributes, Builders builders, Runners runners) {
        setProjectType(projectType);
        this.attributes = new LinkedHashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            setAttributes(attributes);
        }
        this.builders = builders;
        this.runners = runners;
    }

    public ProjectDescription(ProjectType projectType, Builders builders, Runners runners) {
        this(projectType, null, builders, runners);
    }

    public ProjectDescription(ProjectType projectType) {
        this(projectType, null, null);
    }

    public ProjectDescription() {
        this(ProjectType.BLANK);
    }

    /** Gets project type. */
    public ProjectType getProjectType() {
        return projectType;
    }

    /** @see #getProjectType() */
    public void setProjectType(ProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
    }

    /** Gets builder configurations. */
    public Builders getBuilders() {
        return builders;
    }

    public void setBuilders(Builders builders) {
        this.builders = builders;
    }

    /** Gets runner configurations. */
    public Runners getRunners() {
        return runners;
    }

    public void setRunners(Runners runners) {
        this.runners = runners;
    }

    /**
     * Gets all attributes of project. Modifications to the returned {@code List} will not affect the internal state.
     * <p/>
     * Note: attributes are stored within the project as a combination of persisted properties and "implicit" meta-info inside the project
     *
     * @return attributes
     * @see Attribute
     */
    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    /** Gets unmodifiable list of attributes of project which names are started with specified prefix. */
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

    /** Gets single attribute of project with specified name. */
    public Attribute getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.get(name) != null;
    }

    /**
     * Gets single value of attribute with specified name. If attribute has multiple value then this method returns first value in the
     * list.
     */
    public String getAttributeValue(String name) throws ValueStorageException {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /** Gets values of attribute with specified name. */
    public List<String> getAttributeValues(String name) throws ValueStorageException {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValues();
    }

    /** Sets attributes. New attributes will override exited attributes with the same names. */
    public void setAttributes(List<Attribute> list) {
        for (Attribute attribute : list) {
            attributes.put(attribute.getName(), attribute);
        }
    }

    // For internal usage only.
    void clearAttributes() {
        attributes.clear();
    }

    /** Sets single attribute. New attribute will override exited attribute with the same name. */
    public void setAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    /** Gets optional description of project. */
    public String getDescription() {
        return description;
    }

    /** Sets optional description of project. */
    public void setDescription(String description) {
        this.description = description;
    }
}