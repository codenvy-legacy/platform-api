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
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Map;

/**
 * Data transfer object (DTO) for generate project.
 *
 * @author Vladyslav Zhukovskiy
 */
@DTO
@ApiModel(description = "Generate new project")
public interface GenerateDescriptor {
    /** Get name of project generator. */
    @ApiModelProperty(value = "Name of project generator", position = 1, required = true)
    String getGeneratorName();

    /** Set name of project generator. */
    void setGeneratorName(String generatorName);

    GenerateDescriptor withGeneratorName(String generatorName);

    /** Get options needed for generator. */
    @ApiModelProperty(value = "Options needed for generator", position = 2)
    Map<String, String> getOptions();

    /** Set options needed for generator. */
    void setOptions(Map<String, String> options);

    GenerateDescriptor withOptions(Map<String, String> options);

    /** Get project visibility, e.g. private or public. */
    @ApiModelProperty(value = "Visibility for new project", allowableValues = "public,private", position = 3)
    String getProjectVisibility();

    /** Set project visibility, e.g. private or public. */
    void setProjectVisibility(String projectVisibility);

    GenerateDescriptor withProjectVisibility(String projectVisibility);
}
