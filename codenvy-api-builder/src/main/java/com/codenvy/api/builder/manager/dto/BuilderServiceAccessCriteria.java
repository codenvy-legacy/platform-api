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
package com.codenvy.api.builder.manager.dto;

import com.codenvy.api.core.rest.dto.DtoType;

/**
 * Resource access criteria. Basically resource may be assigned to {@code workspace}, {@code project} in some workspace or {@code
 * username}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DtoType(BuilderManagerDtoTypes.BUILDER_SERVICE_ACCESS_CRITERIA_TYPE)
public class BuilderServiceAccessCriteria {
    private String username;
    private String workspace;
    private String project;

    public BuilderServiceAccessCriteria(String username, String workspace, String project) {
        this.username = username;
        this.workspace = workspace;
        this.project = project;
    }

    public BuilderServiceAccessCriteria() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public String toString() {
        return "BuilderServiceAccessCriteria{" +
               "username='" + username + '\'' +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               '}';
    }
}
