/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
