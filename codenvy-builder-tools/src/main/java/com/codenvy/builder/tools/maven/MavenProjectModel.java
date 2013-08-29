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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class MavenProjectModel {
    private String            groupId;
    private String            artifactId;
    private String            version;
    private String            packaging;
    private String            name;
    private String            description;
    private MavenProjectModel parent;
    private java.io.File      pomFile;
    private java.io.File      projectDirectory;

    public MavenProjectModel(String groupId,
                             String artifactId,
                             String version,
                             String packaging,
                             String name,
                             String description,
                             MavenProjectModel parent,
                             java.io.File pomFile,
                             java.io.File projectDirectory) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.name = name;
        this.description = description;
        this.parent = parent;
        this.pomFile = pomFile;
        this.projectDirectory = projectDirectory;
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

    /**
     * Get the type of artifact this project produces, e.g.
     * <ul>
     * <li>jar</li>
     * <li>war</li>
     * <li>ear</li>
     * <li>pom</li>
     * </ul>
     */
    public String getPackaging() {
        return packaging;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MavenProjectModel getParent() {
        return parent;
    }

    public java.io.File getPomFile() {
        return pomFile;
    }

    public java.io.File getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public String toString() {
        return "MavenProjectModel{" +
               "groupId='" + groupId + '\'' +
               ", artifactId='" + artifactId + '\'' +
               ", version='" + version + '\'' +
               ", packaging='" + packaging + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", parent=" + parent +
               ", pomFile=" + pomFile +
               ", projectDirectory=" + projectDirectory +
               '}';
    }
}
