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
 * ProjectType
 * the named group of attributes for Project Description
 *
 * @author gazarenkov
 */
public class ProjectType {

    private final String id;
    private final String name;
    private final String category;

    public ProjectType(String id, String name, String category) {
        this.name = name;
        this.id = id;
        this.category = category;
    }

    /** @return type ID supposed to be unique within IDE */
    public String getId() {
        return id;
    }

    /** @return type display name */
    public String getName() {
        return name;
    }


    /** @return the project type category */
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