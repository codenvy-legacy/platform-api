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

    private String builderName;
    private String runnerName;

    public ProjectType(String id, String name, String category, String builderName, String runnerName) {
        this.name = name;
        this.id = id;
        this.category = category;
        this.builderName = builderName;
        this.runnerName = runnerName;
    }

    public ProjectType(String id, String name, String category) {
        this(name, id, category, null, null);
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
    public String getBuilderName() {
        return builderName;
    }

    /** @see #getBuilderName() */
    public void setbuilderName(String builderName) {
        this.builderName = builderName;
    }

    /**
     * Gets name of runner that should be used for projects of this type.
     *
     * @return name of runner that should be used for projects of this type
     */
    public String getRunnerName() {
        return runnerName;
    }

    /** @see #getRunnerName() */
    public void setRunnerName(String runnerName) {
        this.runnerName = runnerName;
    }

    @Override
    public String toString() {
        return "ProjectType{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               ", builderName='" + builderName + '\'' +
               ", runnerName='" + runnerName + '\'' +
               '}';
    }
}