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

/**
 * Reference to the IDE Project.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectReference {
    /** Get name of project. */
    String getName();

    /** Set name of project. */
    void setName(String name);

    ProjectReference withName(String name);

    /** Get unique ID of type of project. */
    String getProjectTypeId();

    /** Set unique ID of type of project. */
    void setProjectTypeId(String id);

    ProjectReference withProjectTypeId(String id);

    /** Get display name of type of project. */
    String getProjectTypeName();

    /** Set display name of type of project. */
    void setProjectTypeName(String name);

    ProjectReference withProjectTypeName(String name);

    /** Get URL for getting detailed information about project. */
    String getUrl();

    /** Set URL for getting detailed information about project. */
    void setUrl(String url);

    ProjectReference withUrl(String url);

    /** Get URL for opening project in Codenvy IDE. */
    String getIdeUrl();

    /** Set URL for  opening project in Codenvy IDE. */
    void setIdeUrl(String url);

    ProjectReference withIdeUrl(String url);

    /** Get id of workspace this project belongs to. */
    String getWorkspaceId();

    /** Set id of workspace this project belongs to. */
    void setWorkspaceId(String id);

    ProjectReference withWorkspaceId(String id);

    /** Get name of workspace this project belongs to. */
    String getWorkspaceName();

    /** Set name of workspace this project belongs to. */
    void setWorkspaceName(String name);

    ProjectReference withWorkspaceName(String name);

    /** Get project visibility, e.g. private or public. */
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectReference withVisibility(String visibility);

    /** Get creation date of project. */
    long getCreationDate();

    /** Set creation date of project. */
    void setCreationDate(long date);

    ProjectReference withCreationDate(long date);

    /** Get modification date of project. */
    long getModificationDate();

    /** Set modification date of project. */
    void setModificationDate(long date);

    ProjectReference withModificationDate(long date);

    /** Get optional description of project. */
    String getDescription();

    /** Set optional description of project. */
    void setDescription(String description);

    ProjectReference withDescription(String description);
}
