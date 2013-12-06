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

/**
 * Data transfer object (DTO) for com.codenvy.api.project.shared.ProjectDescription.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
@DTO
public interface ProjectDescriptor {
    String getName();

    void setName(String name);

    ProjectDescriptor withName(String name);

    String getProjectTypeId();

    void setProjectTypeId(String id);

    ProjectDescriptor withProjectTypeId(String id);

    String getProjectTypeName();

    void setProjectTypeName(String name);

    ProjectDescriptor withProjectTypeName(String name);

    List<AttributeDTO> getAttributes();

    ProjectDescriptor withAttributes(List<AttributeDTO> attributes);

    void setAttributes(List<AttributeDTO> attributes);
}
