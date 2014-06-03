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