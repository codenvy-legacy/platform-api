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
import com.wordnik.swagger.annotations.ApiModel;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object (DTO) for update project.
 *
 * @author andrew00x
 */
@DTO
@ApiModel(description = "Update project")
public interface ProjectUpdate {
    /** Get unique ID of type of project. */
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectUpdate withProjectTypeId(String id);

    /** Get optional description of project. */
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    ProjectUpdate withDescription(String description);

    /** Get attributes of project. */
    Map<String, List<String>> getAttributes();

    /** Set attributes of project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectUpdate withAttributes(Map<String, List<String>> attributes);

    String getBuilder();

    ProjectUpdate withBuilder(String builder);

    void setBuilder(String builder);

    String getRunner();

    ProjectUpdate withRunner(String runner);

    void setRunner(String runner);

    String getDefaultBuilderEnvironment();

    ProjectUpdate withDefaultBuilderEnvironment(String envId);

    void setDefaultBuilderEnvironment(String envId);

    String getDefaultRunnerEnvironment();

    ProjectUpdate withDefaultRunnerEnvironment(String envId);

    void setDefaultRunnerEnvironment(String envId);

    Map<String, BuilderEnvironmentConfigurationDescriptor> getBuilderEnvironmentConfigurations();

    ProjectUpdate withBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    void setBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    Map<String, RunnerEnvironmentConfigurationDescriptor> getRunnerEnvironmentConfigurations();

    ProjectUpdate withRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    void setRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);
}
