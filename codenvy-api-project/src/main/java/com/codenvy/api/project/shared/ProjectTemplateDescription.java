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
 * The description of project template.
 *
 * @author vitalka
 */
public class ProjectTemplateDescription {

    private final String id;
    private final String title;
    private final String description;
    private final String location;

    public ProjectTemplateDescription(String id, String title, String description, String location) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
    }

    /** @return project template ID supposed to be unique within IDE */
    public String getId() {
        return id;
    }

    /** @return project temple title */
    public String getTitle() {
        return title;
    }

    /** @return project template location, e.g. path to the zip */
    public String getLocation() {
        return location;
    }

    /** @return project template description */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ProjectTemplateDescription{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", description='" + description + '\'' +
               ", location='" + location + '\'' +
               '}';
    }
}