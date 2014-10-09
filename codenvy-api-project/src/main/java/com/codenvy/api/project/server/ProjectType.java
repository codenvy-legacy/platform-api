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

/**
 * ProjectType is the named group of attributes for Project Description.
 *
 * @author gazarenkov
 */
public final class ProjectType {
    public static final ProjectType BLANK = new ProjectType(com.codenvy.api.project.shared.Constants.BLANK_ID,
                                                            com.codenvy.api.project.shared.Constants.BLANK_PROJECT_TYPE,
                                                            com.codenvy.api.project.shared.Constants.BLANK_CATEGORY);

    /** Unique ID of type of project. */
    private final String id;
    /** Display name of project type. */
    private final String name;
    /** Category of project type. */
    private final String category;

    public ProjectType(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public ProjectType(String id) {
        this(id, id, id);
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

    @Override
    public String toString() {
        return "ProjectType{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", category='" + category + '\'' +
               '}';
    }
}