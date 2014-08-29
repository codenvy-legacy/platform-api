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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The description of project template.
 *
 * @author vitalka
 */
public class ProjectTemplateDescription {

    public final static String defaultCategory = "Samples";

    private final String                                       category;
    private final String                                       importerType;
    private final String                                       displayName;
    private final String                                       description;
    private final String                                       location;
    private final Map<String, BuilderEnvironmentConfiguration> builderEnvConfigs;
    private final Map<String, RunnerEnvironmentConfiguration>  runnerEnvConfigs;

    private String defaultBuilderEnvironment;
    private String defaultRunnerEnvironment;

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param category
     *         category of this template. Categories maybe used for creation group of similar templates
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     * @param defaultBuilderEnvironment
     *         ID of default builder environment that should be used for projects created from this template
     * @param defaultRunnerEnvironment
     *         ID of default runner environment that should be used for projects created from this template
     */
    public ProjectTemplateDescription(String category,
                                      String importerType,
                                      String displayName,
                                      String description,
                                      String location,
                                      String defaultBuilderEnvironment,
                                      String defaultRunnerEnvironment,
                                      Map<String, RunnerEnvironmentConfiguration> runnerEnvConfigs) {
        this.category = category;
        this.importerType = importerType;
        this.displayName = displayName;
        this.description = description;
        this.location = location;
        this.defaultBuilderEnvironment = defaultBuilderEnvironment;
        this.defaultRunnerEnvironment = defaultRunnerEnvironment;
        this.runnerEnvConfigs = runnerEnvConfigs;
        builderEnvConfigs = new LinkedHashMap<>();
    }

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param category
     *         category of this template. Categories maybe used for creation group of similar templates
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     */
    public ProjectTemplateDescription(String category, String importerType, String displayName, String description, String location) {
        this(category, importerType, displayName, description, location, null, null, null);
    }

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     */
    public ProjectTemplateDescription(String importerType, String displayName, String description, String location) {
        this(defaultCategory, importerType, displayName, description, location, null, null, null);
    }

    /**
     * Gets type of "importer" that can recognize sources template, sources located at specified {@code location}.
     *
     * @return type of "importer" that can recognize sources template
     */
    public String getImporterType() {
        return importerType;
    }

    /**
     * Gets display name of this project template.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets location of this project template.
     *
     * @return location, e.g. path to the zip or git URL
     */
    public String getLocation() {
        return location;
    }

    /**
     * Get description of this project template.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get category of this project template. Categories maybe used for creation group of similar templates.
     *
     * @return category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets ID of default builder environment that should be used for projects created from this template.
     *
     * @return ID of default builder environment that should be used for projects created from this template
     */
    public String getDefaultBuilderEnvironment() {
        return defaultBuilderEnvironment;
    }

    /** @see #getDefaultBuilderEnvironment() */
    public void setDefaultBuilderEnvironment(String defaultBuilderEnvironment) {
        this.defaultBuilderEnvironment = defaultBuilderEnvironment;
    }

    /**
     * Gets ID of default runner environment that should be used for projects created from this template.
     *
     * @return ID of default runner environment that should be used for projects created from this template
     */
    public String getDefaultRunnerEnvironment() {
        return defaultRunnerEnvironment;
    }

    /** @see #getDefaultRunnerEnvironment() */
    public void setDefaultRunnerEnvironment(String defaultRunnerEnvironment) {
        this.defaultRunnerEnvironment = defaultRunnerEnvironment;
    }

    /**
     * Gets predefined configuration for builder environment by environment's ID. Configuration may contains some recommended parameters
     * for builder environments. Builder may use own configuration parameters if this method returns {@code null} or if returned
     * configuration doesn't contains required parameters or if parameters specified in configuration are not applicable.
     */
    public BuilderEnvironmentConfiguration getBuilderEnvironmentConfiguration(String env) {
        return builderEnvConfigs.get(env);
    }

    /**
     * Gets predefined configuration for runner environment by environment's ID. Configuration may contains some recommended parameters
     * for runner environments. Runner may use own configuration parameters if this method returns {@code null} or if returned
     * configuration doesn't contains required parameters or if parameters specified in configuration are not applicable.
     */
    public RunnerEnvironmentConfiguration getRunnerEnvironmentConfiguration(String env) {
        return runnerEnvConfigs.get(env);
    }

    /**
     * Gets predefined configuration for builder environment.
     *
     * @see #getBuilderEnvironmentConfiguration(String)
     */
    public Map<String, BuilderEnvironmentConfiguration> getBuilderEnvironmentConfigurations() {
        if (builderEnvConfigs == null) new LinkedHashMap<>();
        return new LinkedHashMap<>(builderEnvConfigs);
    }

    /**
     * Gets predefined configurations for runner environment.
     *
     * @see #getRunnerEnvironmentConfiguration(String)
     */
    public Map<String, RunnerEnvironmentConfiguration> getRunnerEnvironmentConfigurations() {
        if (runnerEnvConfigs == null) return new LinkedHashMap<>();
        return new LinkedHashMap<>(runnerEnvConfigs);
    }

    @Override
    public String toString() {
        return "ProjectTemplateDescription{" +
               "category='" + category + '\'' +
               ", importerType='" + importerType + '\'' +
               ", displayName='" + displayName + '\'' +
               ", description='" + description + '\'' +
               ", location='" + location + '\'' +
               ", defaultBuilderEnvironment='" + defaultBuilderEnvironment + '\'' +
               ", defaultRunnerEnvironment='" + defaultRunnerEnvironment + '\'' +
               ", builderEnvConfigs=" + builderEnvConfigs +
               ", runnerEnvConfigs=" + runnerEnvConfigs +
               '}';
    }
}