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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of Project. Project description filled in a course of project creating and stored with a Project.
 *
 * @author gazarenkov
 */
public class ProjectDescription {
    private ProjectType                                  projectType;
    private String                                       builder;
    private String                                       runner;
    private String                                       defaultBuilderEnvironment;
    private String                                       defaultRunnerEnvironment;
    private Map<String, BuilderEnvironmentConfiguration> builderEnvConfigs;
    private Map<String, RunnerEnvironmentConfiguration>  runnerEnvConfigs;
    private Map<String, Attribute>                       attributes;
    private String                                       description;

    public ProjectDescription(ProjectType projectType) {
        this.attributes = new LinkedHashMap<>();
        this.builderEnvConfigs = new LinkedHashMap<>();
        this.runnerEnvConfigs = new LinkedHashMap<>();
        setProjectType(projectType);
    }

    public ProjectDescription() {
        this(new ProjectType("nameless", "nameless", "nameless"));
    }

    public ProjectDescription(ProjectDescription origin) {
        this.projectType = new ProjectType(origin.getProjectType());
        this.builder = origin.getBuilder();
        this.runner = origin.getRunner();
        this.defaultBuilderEnvironment = origin.getDefaultBuilderEnvironment();
        this.defaultRunnerEnvironment = origin.getDefaultRunnerEnvironment();
        this.attributes = new LinkedHashMap<>();
        this.builderEnvConfigs = new LinkedHashMap<>();
        this.runnerEnvConfigs = new LinkedHashMap<>();
        this.description = origin.getDescription();
        for (Attribute attribute : origin.getAttributes()) {
            final Attribute copy = new Attribute(attribute);
            attributes.put(copy.getName(), copy);
        }
        for (Map.Entry<String, BuilderEnvironmentConfiguration> e : origin.getBuilderEnvironmentConfigurations().entrySet()) {
            builderEnvConfigs.put(e.getKey(), new BuilderEnvironmentConfiguration(e.getValue()));
        }
        for (Map.Entry<String, RunnerEnvironmentConfiguration> e : origin.getRunnerEnvironmentConfigurations().entrySet()) {
            runnerEnvConfigs.put(e.getKey(), new RunnerEnvironmentConfiguration(e.getValue()));
        }
    }

    /** @return Project type */
    public ProjectType getProjectType() {
        return projectType;
    }

    /** @see #getProjectType() */
    public void setProjectType(ProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("Project type may not be null. ");
        }
        this.projectType = projectType;
    }

    /**
     * Gets name of builder that should be used for this project.
     *
     * @return name of builder that should be used for this project
     */
    public String getBuilder() {
        return builder;
    }

    /** @see #getBuilder() */
    public void setBuilder(String builder) {
        this.builder = builder;
    }

    /**
     * Gets name of runner that should be used for this project.
     *
     * @return name of runner that should be used for this project
     */
    public String getRunner() {
        return runner;
    }

    /** @see #getRunner() */
    public void setRunner(String runner) {
        this.runner = runner;
    }

    /**
     * Gets ID of default builder environment that should be used for this project.
     *
     * @return ID of default builder environment that should be used for this project
     */
    public String getDefaultBuilderEnvironment() {
        return defaultBuilderEnvironment;
    }

    /** @see #getDefaultBuilderEnvironment() */
    public void setDefaultBuilderEnvironment(String defaultBuilderEnvironment) {
        this.defaultBuilderEnvironment = defaultBuilderEnvironment;
    }

    /**
     * Gets ID of default runner environment that should be used for this project.
     *
     * @return ID of default runner environment that should be used for this project
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
        return new LinkedHashMap<>(builderEnvConfigs);
    }

    public void setBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfiguration> builderEnvConfigs) {
        if (!(builderEnvConfigs == null || builderEnvConfigs.isEmpty())) {
            this.builderEnvConfigs.putAll(builderEnvConfigs);
        }
    }

    /**
     * Gets predefined configurations for runner environment.
     *
     * @see #getRunnerEnvironmentConfiguration(String)
     */
    public Map<String, RunnerEnvironmentConfiguration> getRunnerEnvironmentConfigurations() {
        return new LinkedHashMap<>(runnerEnvConfigs);
    }

    public void setRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfiguration> runnerEnvConfigs) {
        if (!(runnerEnvConfigs == null || runnerEnvConfigs.isEmpty())) {
            this.runnerEnvConfigs.putAll(runnerEnvConfigs);
        }
    }

    /**
     * Get all attributes of project. Modifications to the returned {@code List} will not affect the internal state.
     * <p/>
     * Note: attributes are stored within the project as a combination of persisted properties and "implicit" metainfo inside the project
     *
     * @return attributes
     * @see Attribute
     */
    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    /** Get unmodifiable list of attributes of project which names are started with specified prefix. */
    public List<Attribute> getAttributes(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.unmodifiableList(getAttributes());
        }
        final List<Attribute> result = new ArrayList<>();
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Get single attribute of project with specified name. */
    public Attribute getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.get(name) != null;
    }

    /** Get single value of attribute with specified name. If attribute has multiple value then this method returns first value in the list. */
    public String getAttributeValue(String name) {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /** Get values of attribute with specified name. */
    public List<String> getAttributeValues(String name) {
        final Attribute attribute = attributes.get(name);
        if (attribute == null) {
            return null;
        }
        return attribute.getValues();
    }

    /** Set attributes. New attributes will override exited attributes with the same names. */
    public void setAttributes(List<Attribute> list) {
        if (!(list == null || list.isEmpty())) {
            for (Attribute attribute : list) {
                attributes.put(attribute.getName(), attribute);
            }
        }
    }

    /** Set single attribute. New attribute will override exited attribute with the same name. */
    public void setAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    public Attribute removeAttribute(String name) {
        return attributes.remove(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ProjectDescription{" +
               "projectType=" + projectType +
               ", builder='" + builder + '\'' +
               ", runner='" + runner + '\'' +
               ", defaultBuilderEnvironment='" + defaultBuilderEnvironment + '\'' +
               ", defaultRunnerEnvironment='" + defaultRunnerEnvironment + '\'' +
               ", builderEnvConfigs=" + builderEnvConfigs +
               ", runnerEnvConfigs=" + runnerEnvConfigs +
               ", attributes=" + attributes +
               ", description='" + description + '\'' +
               '}';
    }
}