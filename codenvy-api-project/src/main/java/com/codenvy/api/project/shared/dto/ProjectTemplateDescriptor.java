/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
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

/**
 * @author Vitaly Parfonov
 */
@DTO
public interface ProjectTemplateDescriptor {

    /** Get unique ID of template. */
    String getTemplateId();

    /** Set unique ID of template. */
    void setTemplateId(String id);

    ProjectTemplateDescriptor withTemplateId(String id);

    /** Get ID of type of project. */
    String getProjectTypeId();

    /** Set ID of type of project. */
    void setProjectTypeId(String id);

    ProjectTemplateDescriptor withProjectTypeId(String id);

    /** Get display name of project template. */
    String getTemplateTitle();

    /** Set display name of project template. */
    void setTemplateTitle(String title);

    ProjectTemplateDescriptor withTemplateTitle(String title);

    /** Get description of project template. */
    String getTemplateDescription();

    /** Set description of project template. */
    void setTemplateDescription(String description);

    String getTemplateLocation();

    void setTemplateLocation(String location);

    ProjectTemplateDescriptor withTemplateLocation(String location);
}
