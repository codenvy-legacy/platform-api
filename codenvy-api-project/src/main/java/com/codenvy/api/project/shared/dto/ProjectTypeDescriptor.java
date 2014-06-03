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
 * Data transfer object (DTO) for com.codenvy.api.project.shared.ProjectTypeDescription.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectTypeDescriptor {
    /** Get unique ID of type of project. */
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectTypeDescriptor withProjectTypeId(String id);

    /** Get display name of type of project. */
    String getProjectTypeName();

    /** Set display name of type of project. */
    void setProjectTypeName(String name);

    ProjectTypeDescriptor withProjectTypeName(String name);

    /** Get project type category. */
    String getProjectTypeCategory();

    /** Set project type category. */
    void setProjectTypeCategory(String category);

    ProjectTypeDescriptor withProjectTypeCategory(String category);

    List<AttributeDescriptor> getAttributeDescriptors();

    void setAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    ProjectTypeDescriptor withAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    List<ProjectTemplateDescriptor> getTemplates();

    void setTemplates(List<ProjectTemplateDescriptor> templates);

    ProjectTypeDescriptor withTemplates(List<ProjectTemplateDescriptor> templates);

    Map<String, String> getIconRegistry();

    void setIconRegistry(Map<String, String> iconRegistry);

    ProjectTypeDescriptor withIconRegistry(Map<String, String> iconRegistry);
}
