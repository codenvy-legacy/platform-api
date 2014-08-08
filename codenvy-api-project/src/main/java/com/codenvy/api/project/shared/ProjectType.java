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

/**
 * ProjectType is the named group of attributes for Project Description.
 *
 * @author gazarenkov
 */
public class ProjectType {

    private final String id;
    private final String name;
    private final String category;

    private String builder;
    private String runner;

    public ProjectType(String id, String name, String category, String builder, String runner) {
        this.name = name;
        this.id = id;
        this.category = category;
        this.builder = builder;
        this.runner = runner;
    }

    public ProjectType(String id, String name, String category) {
        this(id, name, category, null, null);
    }

    /**
     * Ges ID of this project type. Type ID supposed to be unique within IDE.
     *
     * @return ID of this project type
     */
    public String getId() {
        return id;
    }

    /**
     * Gets display name of this project type.
     *
     * @return display name of this project type
     */
    public String getName() {
        return name;
    }

    /**
     * Gets category of project type. Categories maybe used for creation group of similar project types.
     *
     * @return category of project type
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets name of builder that should be used for projects of this type.
     *
     * @return name of builder that should be used for projects of this type
     */
    public String getBuilder() {
        return builder;
    }

    /** @see #getBuilder() */
    public void setbuilder(String builder) {
        this.builder = builder;
    }

    /**
     * Gets name of runner that should be used for projects of this type.
     *
     * @return name of runner that should be used for projects of this type
     */
    public String getRunner() {
        return runner;
    }

    /** @see #getRunner() */
    public void setRunner(String runner) {
        this.runner = runner;
    }

    @Override
    public String toString() {
        return "ProjectType{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               ", builderName='" + builder + '\'' +
               ", runnerName='" + runner + '\'' +
               '}';
    }
}