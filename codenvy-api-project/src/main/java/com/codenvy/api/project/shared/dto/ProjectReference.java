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

import com.codenvy.api.core.rest.shared.dto.ObjectStatus;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Reference to the IDE Project.
 *
 * @author andrew00x
 */
@DTO
@ApiModel(description = "Short information about project, it doesn't contain any project attributes.")
public interface ProjectReference {
    /** Get name of project. */
    @ApiModelProperty(value = "Name of the project", position = 1)
    String getName();

    /** Set name of project. */
    void setName(String name);

    ProjectReference withName(String name);

    /** Get path of project. */
    @ApiModelProperty(value = "Full path of the project", position = 2)
    String getPath();

    /** Set path of project. */
    void setPath(String path);

    ProjectReference withPath(String path);

    /** Get unique ID of type of project. */
    @ApiModelProperty(value = "Unique ID of project's type", position = 3)
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectReference withProjectTypeId(String id);

    /** Get display name of type of project. */
    @ApiModelProperty(value = "Display name of project's type", position = 4)
    String getProjectTypeName();

    /** Set display name of type of project. */
    void setProjectTypeName(String name);

    ProjectReference withProjectTypeName(String name);

    /** Get URL for getting detailed information about project. */
    @ApiModelProperty(value = "URL for getting detailed information about the project", position = 5)
    String getUrl();

    /** Set URL for getting detailed information about project. */
    void setUrl(String url);

    ProjectReference withUrl(String url);

    /** Get URL for opening project in Codenvy IDE. */
    @ApiModelProperty(value = "URL for opening project in Codenvy IDE", position = 6)
    String getIdeUrl();

    /** Set URL for  opening project in Codenvy IDE. */
    void setIdeUrl(String url);

    ProjectReference withIdeUrl(String url);

    /** Get id of workspace this project belongs to. */
    @ApiModelProperty(value = "ID of workspace which the project belongs to", position = 7)
    String getWorkspaceId();

    /** Set id of workspace this project belongs to. */
    void setWorkspaceId(String id);

    ProjectReference withWorkspaceId(String id);

    /** Get name of workspace this project belongs to. */
    @ApiModelProperty(value = "Name of workspace which the project belongs to", position = 8)
    String getWorkspaceName();

    /** Set name of workspace this project belongs to. */
    void setWorkspaceName(String name);

    ProjectReference withWorkspaceName(String name);

    /** Get project visibility, e.g. private or public. */
    @ApiModelProperty(value = "Visibility of the project", allowableValues = "public,private", position = 9)
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectReference withVisibility(String visibility);

    /** Get creation date of project. */
    @ApiModelProperty(value = "Time that the project was created or -1 if creation time in unknown", dataType = "long", position = 10)
    long getCreationDate();

    /** Set creation date of project. */
    void setCreationDate(long date);

    ProjectReference withCreationDate(long date);

    /** Get modification date of project. */
    @ApiModelProperty(value = "Time that the project was last modified or -1 if modification time date in unknown",
                      dataType = "long", position = 11)
    long getModificationDate();

    /** Set modification date of project. */
    void setModificationDate(long date);

    ProjectReference withModificationDate(long date);

    /** Get optional description of project. */
    @ApiModelProperty(value = "Optional description of the project", position = 12)
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    ProjectReference withDescription(String description);

    ObjectStatus getObjectStatus();

    void setObjectStatus(ObjectStatus status);

    ProjectReference withObjectStatus(ObjectStatus status);
}
