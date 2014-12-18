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

import java.util.List;
import java.util.Map;

/**
 *
 * @author gazarenkov
 *
 */
@DTO
public interface ProjectTypeDefinition {
    /** Get unique ID of type of project. */
    String getId();

    /** Set unique ID of type of project. */
    void setId(String id);

    ProjectTypeDefinition withId(String id);

    /** Get display name of type of project. */
    String getDisplayName();

    /** Set display name of type of project. */
    void setDisplayName(String name);

    ProjectTypeDefinition withDisplayName(String name);

    List<AttributeDescriptor> getAttributeDescriptors();

    void setAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    ProjectTypeDefinition withAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    /** Gets runner configurations. */
    List<String> getRunnerCategories();

    void setRunnerCategories(List<String> runnerCategories);

    ProjectTypeDefinition withRunnerCategories(List<String> runnerCategories);


    String getDefaultBuilder();

    void setDefaultBuilder(String builder);

    ProjectTypeDefinition withDefaultBuilder(String builder);

    String getDefaultRunner();

    void setDefaultRunner(String runner);

    ProjectTypeDefinition withDefaultRunner(String runner);

}
