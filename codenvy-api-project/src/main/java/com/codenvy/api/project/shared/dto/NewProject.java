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

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Data transfer object (DTO) for create project.
 *
 * @author andrew00x
 */
@DTO
@ApiModel(description = "New project")
public interface NewProject {
    /** Get unique ID of type of project. */
    @ApiModelProperty(value = "Unique ID of project's type", position = 1, required = true)
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "type")
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    NewProject withProjectTypeId(String id);

    /** Get optional description of project. */
    @ApiModelProperty(value = "Optional description for new project", position = 2)
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "description")
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    NewProject withDescription(String description);

    /** Get attributes of project. */
//    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "attributes")
    Map<String, List<String>> getAttributes();

    @ApiModelProperty(value = "Attributes for new project", position = 3)
    /** Set attributes of project. */
    void setAttributes(Map<String, List<String>> attributes);

    NewProject withAttributes(Map<String, List<String>> attributes);

    @ApiModelProperty(value = "Visibility for new project", allowableValues = "public,private", position = 4)
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "visibility")
    /** Get project visibility, e.g. private or public. */
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    NewProject withVisibility(String visibility);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "builderType")
    String getBuilder();

    NewProject withBuilder(String builder);

    void setBuilder(String builder);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "runnerType")
    String getRunner();

    NewProject withRunner(String runner);

    void setRunner(String runner);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "defaultBuilder")
    String getDefaultBuilderEnvironment();

    NewProject withDefaultBuilderEnvironment(String envId);

    void setDefaultBuilderEnvironment(String envId);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "defaultRunner")
    String getDefaultRunnerEnvironment();

    NewProject withDefaultRunnerEnvironment(String envId);

    void setDefaultRunnerEnvironment(String envId);

    Map<String, BuilderEnvironmentConfigurationDescriptor> getBuilderEnvironmentConfigurations();

    NewProject withBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    void setBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    Map<String, RunnerEnvironmentConfigurationDescriptor> getRunnerEnvironmentConfigurations();

    NewProject withRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    void setRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);
}
