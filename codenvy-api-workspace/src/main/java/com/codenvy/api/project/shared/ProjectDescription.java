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
import java.util.List;

/**
 * Description of Project. Project description filled in a course of project creating and stored with a Project
 *
 * @author gazarenkov
 */
public class ProjectDescription {
    private final String      name;
    private final ProjectType projectType;

    private List<Attribute> attributes;

    public ProjectDescription(String name, ProjectType projectType, List<Attribute> attributes) {
        if (name == null) {
            throw new IllegalArgumentException("Project name may not be null. ");
        }
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.name = name;
        this.projectType = projectType;
        this.attributes = attributes;
    }

    public ProjectDescription(ProjectType projectType, String name) {
        this(name, projectType, null);
    }

    public String getName() {
        return name;
    }

    /** @return Project type */
    public ProjectType getProjectType() {
        return projectType;
    }

    /**
     * @return attributes
     *         <p/>
     *         Note: attributes are stored within the project as a combination of persisted properties and "implicit" metainfo inside the
     *         project
     * @see Attribute
     */
    public List<Attribute> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes;
    }

    public Attribute getAttribute(String name) {
        if (attributes == null) {
            return null;
        }
        for (Attribute attribute : attributes) {
            if (name.equals(attribute.getName())) {
                return attribute;
            }
        }
        return null;
    }
}