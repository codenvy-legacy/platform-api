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

    private final String importerType;
    private final String title;
    private final String description;
    private final String location;

    public ProjectTemplateDescription(String importerType, String title, String description, String location) {
        this.importerType = importerType;
        this.title = title;
        this.description = description;
        this.location = location;
    }

    /** Get type of "importer" that can recognize sources template sources located at specified {@code location}. */
    public String getImporterType() {
        return importerType;
    }

    /** @return project template display name */
    public String getDisplayName() {
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
               "importerType='" + importerType + '\'' +
               ", title='" + title + '\'' +
               ", description='" + description + '\'' +
               ", location='" + location + '\'' +
               '}';
    }
}