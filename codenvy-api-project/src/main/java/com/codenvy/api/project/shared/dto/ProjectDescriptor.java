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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object (DTO) for com.codenvy.api.project.shared.ProjectDescription.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectDescriptor {
    String getBaseUrl();

    void setBaseUrl(String url);

    ProjectDescriptor withBaseUrl(String url);

    String getId();

    void setId(String id);

    ProjectDescriptor withId(String id);

    /** Get unique ID of type of project. */
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectDescriptor withProjectTypeId(String id);

    /** Get display name of type of project. */
    String getProjectTypeName();

    /** Set display name of type of project. */
    void setProjectTypeName(String name);

    ProjectDescriptor withProjectTypeName(String name);

    /** Get project visibility, e.g. private or public. */
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectDescriptor withVisibility(String visibility);

    /** Get optional description of project. */
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    ProjectDescriptor withDescription(String description);

    /** Get modification date of project. */
    long getModificationDate();

    /** Set modification date of project. */
    void setModificationDate(long date);

    ProjectDescriptor withModificationDate(long date);

    /** Get attributes of project. */
    Map<String, List<String>> getAttributes();

    /** Set attributes of project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectDescriptor withAttributes(Map<String, List<String>> attributes);

    List<Link> getLinks();

    ProjectDescriptor withLinks(List<Link> links);

    void setLinks(List<Link> links);
}
