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

    public final static String defaultCategory = "Samples";

    private final String category;
    private final String importerType;
    private final String displayName;
    private final String description;
    private final String location;

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory    
     *
     * @param importerType importer name like git, zip
     * @param displayName
     * @param description
     * @param location
     */
    public ProjectTemplateDescription(String importerType, String displayName, String description, String location) {
        this(defaultCategory, importerType, displayName, description, location);

    }

    public ProjectTemplateDescription(String category, String importerType, String displayName, String description, String location) {
        this.category = category;
        this.importerType = importerType;
        this.displayName = displayName;
        this.description = description;
        this.location = location;
    }


    /** Get type of "importer" that can recognize sources template sources located at specified {@code location}. */
    public String getImporterType() {
        return importerType;
    }

    /** @return project template display name */
    public String getDisplayName() {
        return displayName;
    }

    /** @return project template location, e.g. path to the zip */
    public String getLocation() {
        return location;
    }

    /** @return project template description */
    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ProjectTemplateDescription{" +
               "category='" + category + '\'' +
               ", importerType='" + importerType + '\'' +
               ", displayName='" + displayName + '\'' +
               ", description='" + description + '\'' +
               ", location='" + location + '\'' +
               '}';
    }
}