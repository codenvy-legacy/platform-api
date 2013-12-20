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
package com.codenvy.builder.tools.maven;

/**
 * Contains information about a dependency of the project.
 *
 * @author <a href="mailto:azatsarynnyy@codenvy.com">Artem Zatsarynnyy</a>
 */
public class MavenDependency {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public MavenDependency(String groupId,
                           String artifactId,
                           String version,
                           String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "MavenDependency{" +
               "groupId='" + groupId + '\'' +
               ", artifactId='" + artifactId + '\'' +
               ", version='" + version + '\'' +
               ", scope=" + scope +
               '}';
    }
}
