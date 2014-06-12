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
    String getId();

    void setId(String id);

    ProjectDescriptor withId(String id);

    /** Get name of project. */
    String getName();

    /** Set name of project. */
    void setName(String name);

    ProjectDescriptor withName(String name);

    /** Get path of project. */
    String getPath();

    /** Set path of project. */
    void setPath(String path);

    ProjectDescriptor withPath(String path);

    String getBaseUrl();

    void setBaseUrl(String url);

    ProjectDescriptor withBaseUrl(String url);

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

    /** Get creation date of project. */
    long getCreationDate();

    /** Set creation date of project. */
    void setCreationDate(long date);

    ProjectDescriptor withCreationDate(long date);

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
