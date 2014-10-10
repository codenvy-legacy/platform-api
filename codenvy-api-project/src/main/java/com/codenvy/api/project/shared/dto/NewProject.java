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
public interface NewProject extends ProjectUpdate {
    /** Gets name of project. */
    @ApiModelProperty(value = "Project name", position = 1)
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "name")
    String getName();

    /** Sets name of project. */
    void setName(String name);

    // For method call chain

    NewProject withName(String name);

    NewProject withType(String type);

    NewProject withBuilders(BuildersDescriptor builders);

    NewProject withRunners(RunnersDescriptor runners);

    NewProject withDescription(String description);

    NewProject withAttributes(Map<String, List<String>> attributes);

    NewProject withVisibility(String visibility);
}
