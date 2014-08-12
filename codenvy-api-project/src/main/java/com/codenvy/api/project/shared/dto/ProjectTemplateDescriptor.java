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
package com.codenvy.api.project.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/** @author Vitaly Parfonov */
@DTO
public interface ProjectTemplateDescriptor {

    /** Get category of project template. */
    String getCategory();

    /** Set category of project template. */
    void setCategory(String category);

    ProjectTemplateDescriptor withCategory(String category);

    ImportSourceDescriptor getSource();

    void setSource(ImportSourceDescriptor sources);

    ProjectTemplateDescriptor withSource(ImportSourceDescriptor sources);

    /** Get display name of project template. */
    String getDisplayName();

    /** Set display name of project template. */
    void setDisplayName(String displayName);

    ProjectTemplateDescriptor withDisplayName(String displayName);

    String getDefaultBuilderEnvironment();

    ProjectTemplateDescriptor withDefaultBuilderEnvironment(String envId);

    void setDefaultBuilderEnvironment(String envId);

    String getDefaultRunnerEnvironment();

    ProjectTemplateDescriptor withDefaultRunnerEnvironment(String envId);

    void setDefaultRunnerEnvironment(String envId);

    Map<String, BuilderEnvironmentConfigurationDescriptor> getBuilderEnvironmentConfigurations();

    ProjectTemplateDescriptor withBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    void setBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    Map<String, RunnerEnvironmentConfigurationDescriptor> getRunnerEnvironmentConfigurations();

    ProjectTemplateDescriptor withRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    void setRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    /** Get description of project template. */
    String getDescription();

    /** Set description of project template. */
    void setDescription(String description);

    ProjectTemplateDescriptor withDescription(String description);
}
