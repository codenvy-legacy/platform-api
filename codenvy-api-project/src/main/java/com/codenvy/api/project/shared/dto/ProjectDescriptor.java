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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object (DTO) for com.codenvy.api.project.shared.ProjectDescription.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectDescriptor {

    /** Get name of project. */
    @ApiModelProperty(value = "Project name", position = 1)
    String getName();

    /** Set name of project. */
    void setName(String name);

    ProjectDescriptor withName(String name);

    /** Get path of project. */
    @ApiModelProperty(value = "Project path", position = 2)
    String getPath();

    /** Set path of project. */
    void setPath(String path);

    ProjectDescriptor withPath(String path);

    /** Id of workspace which projects belongs to. */
    @ApiModelProperty(value = "Workspace ID", position = 3)
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    ProjectDescriptor withWorkspaceId(String workspaceId);

    @ApiModelProperty(value = "Project URL", position = 4)
    String getBaseUrl();

    void setBaseUrl(String url);

    ProjectDescriptor withBaseUrl(String url);

    /** Get unique ID of type of project. */
    @ApiModelProperty(value = "Project type ID", position = 5)
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectDescriptor withProjectTypeId(String id);

    /** Get display name of type of project. */
    @ApiModelProperty(value = "Name of a project type", position = 6)
    String getProjectTypeName();

    /** Set display name of type of project. */
    void setProjectTypeName(String name);

    ProjectDescriptor withProjectTypeName(String name);

    /** Get project visibility, e.g. private or public. */
    @ApiModelProperty(value = "Project privacy. Projects are public by default", allowableValues = "private,public", position = 7)
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectDescriptor withVisibility(String visibility);

    /** Get optional description of project. */
    @ApiModelProperty(value = "Project description", position = 8)
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    ProjectDescriptor withDescription(String description);

    /** Get creation date of project. */
    @ApiModelProperty(value = "Creation date in UNIX Epoch format", dataType = "long", position = 9)
    long getCreationDate();

    /** Set creation date of project. */
    void setCreationDate(long date);

    ProjectDescriptor withCreationDate(long date);

    /** Get modification date of project. */
    @ApiModelProperty(value = "Most recent modification date", dataType = "long", position = 10)
    long getModificationDate();

    /** Set modification date of project. */
    void setModificationDate(long date);

    ProjectDescriptor withModificationDate(long date);

    /** Get attributes of project. */
    @ApiModelProperty(value = "Project attributes", position = 11)
    Map<String, List<String>> getAttributes();

    /** Set attributes of project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectDescriptor withAttributes(Map<String, List<String>> attributes);

    /** Get permissions of current user on this project. Current user is user who retrieved this object. */
    @ApiModelProperty(value = "Permissions in the project", position = 12)
    List<String> getCurrentUserPermissions();

    void setCurrentUserPermissions(List<String> permissions);

    ProjectDescriptor withCurrentUserPermissions(List<String> permissions);

    String getBuilder();

    ProjectDescriptor withBuilder(String builder);

    void setBuilder(String builder);

    String getRunner();

    ProjectDescriptor withRunner(String runner);

    void setRunner(String runner);

    String getDefaultBuilderEnvironment();

    ProjectDescriptor withDefaultBuilderEnvironment(String envId);

    void setDefaultBuilderEnvironment(String envId);

    String getDefaultRunnerEnvironment();

    ProjectDescriptor withDefaultRunnerEnvironment(String envId);

    void setDefaultRunnerEnvironment(String envId);

    Map<String, BuilderEnvironmentConfigurationDescriptor> getBuilderEnvironmentConfigurations();

    ProjectDescriptor withBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    void setBuilderEnvironmentConfigurations(Map<String, BuilderEnvironmentConfigurationDescriptor> configs);

    Map<String, RunnerEnvironmentConfigurationDescriptor> getRunnerEnvironmentConfigurations();

    ProjectDescriptor withRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    void setRunnerEnvironmentConfigurations(Map<String, RunnerEnvironmentConfigurationDescriptor> configs);

    List<Link> getLinks();

    ProjectDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);

    /**
     * Get URL for opening project in Codenvy IDE.
     * @return URL for opening project in Codenvy IDE.
     */
    @ApiModelProperty(value = "URL for opening project in Codenvy IDE", position = 6)
    String getIdeUrl();

    /**
     * Sets URL for  opening project in Codenvy IDE.
     * @param url the IDE URL
     */
    void setIdeUrl(String url);

    /**
     * Sets URL for  opening project in Codenvy IDE.
     * @param url the IDE URL
     * @return {@Link ProjectDescriptor}
     */
    ProjectDescriptor withIdeUrl(String url);

    /**
     * Get name of workspace this project belongs to.
     * @return name of workspace this project belongs to
     */
    @ApiModelProperty(value = "Name of workspace which the project belongs to", position = 8)
    String getWorkspaceName();

    /**
     * Sets name of workspace this project belongs to.
     * @param name the name of the workspace
     */
    void setWorkspaceName(String name);

    /**
     * Sets name of workspace this project belongs to.
     * @param name the name of the workspace
     * @return {@Link ProjectDescriptor}
     */
    ProjectDescriptor withWorkspaceName(String name);


}
