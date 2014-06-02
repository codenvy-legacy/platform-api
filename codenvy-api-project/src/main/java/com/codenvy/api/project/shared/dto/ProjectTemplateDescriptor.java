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

/** @author Vitaly Parfonov */
@DTO
public interface ProjectTemplateDescriptor {
    ImportSourceDescriptor getSources();

    void setSources(ImportSourceDescriptor sources);

    ProjectTemplateDescriptor withSources(ImportSourceDescriptor sources);

    /** Get display name of project template. */
    String getDisplayName();

    /** Set display name of project template. */
    void setDisplayName(String displayName);

    ProjectTemplateDescriptor withDisplayName(String displayName);

    /** Get description of project template. */
    String getDescription();

    /** Set description of project template. */
    void setDescription(String description);

    ProjectTemplateDescriptor withDescription(String description);
}
