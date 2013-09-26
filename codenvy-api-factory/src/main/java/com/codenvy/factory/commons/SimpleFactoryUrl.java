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
package com.codenvy.factory.commons;

/** Holds factory url parameters values, used in factory v1.0 */
public class SimpleFactoryUrl extends FactoryUrl {
    private String projectName;
    private String workspaceName;

    public SimpleFactoryUrl() {
        super();
    }

    public SimpleFactoryUrl(String version, String vcs, String vcsUrl, String commitId, String projectName, String workspaceName) {
        super(version, vcs, vcsUrl, commitId);
        this.projectName = projectName;
        this.workspaceName = workspaceName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleFactoryUrl)) return false;
        if (!super.equals(o)) return false;

        SimpleFactoryUrl that = (SimpleFactoryUrl)o;

        if (projectName != null ? !projectName.equals(that.projectName) : that.projectName != null) return false;
        if (workspaceName != null ? !workspaceName.equals(that.workspaceName) : that.workspaceName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (projectName != null ? projectName.hashCode() : 0);
        result = 31 * result + (workspaceName != null ? workspaceName.hashCode() : 0);
        return result;
    }
}
