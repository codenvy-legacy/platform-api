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
               '}';
    }
}