/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectProperties {
    public static ProjectProperties load(Project project) throws IOException {
        final AbstractVirtualFileEntry projectFile = project.getBaseFolder().getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        if (projectFile == null || !projectFile.isFile()) {
            return new ProjectProperties();
        }
        try (InputStream inputStream = ((FileEntry)projectFile).getInputStream()) {
            return JsonHelper.fromJson(inputStream, ProjectProperties.class, null);
        } catch (JsonParseException e) {
            throw new ProjectStructureConstraintException("Unable parse project properties. " + e.getMessage());
        }
    }

    public void save(Project project) throws IOException {
        final FolderEntry baseFolder = project.getBaseFolder();
        AbstractVirtualFileEntry projectFile = baseFolder.getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        if (projectFile != null) {
            if (!projectFile.isFile()) {
                throw new ProjectStructureConstraintException(
                        String.format("Unable save project properties. Path %s/%s exists but is not a file.",
                                      baseFolder.getPath(), Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
            }
            ((FileEntry)projectFile).updateContent(JsonHelper.toJson(this).getBytes());
        } else {
            AbstractVirtualFileEntry codenvyDir = baseFolder.getChild(Constants.CODENVY_FOLDER);
            if (codenvyDir == null) {
                codenvyDir = baseFolder.createFolder(Constants.CODENVY_FOLDER);
            } else if (!codenvyDir.isFolder()) {
                throw new ProjectStructureConstraintException(
                        String.format("Unable save project properties. Path %s/%s exists but is not a folder.",
                                      baseFolder.getPath(), Constants.CODENVY_FOLDER));
            }
            ((FolderEntry)codenvyDir).createFile(Constants.CODENVY_PROJECT_FILE,
                                                 JsonHelper.toJson(this).getBytes(),
                                                 "application/json");
        }
    }

    private String                type;
    private String                description;
    private List<ProjectProperty> properties;

    public ProjectProperties() {
    }

    public ProjectProperties(String type, String description, List<ProjectProperty> properties) {
        this.type = type;
        this.description = description;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ProjectProperties withType(String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectProperties withDescription(String description) {
        this.description = description;
        return this;
    }

    public List<ProjectProperty> getProperties() {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        return properties;
    }

    public void setProperties(List<ProjectProperty> properties) {
        this.properties = properties;
    }

    public ProjectProperties withProperties(List<ProjectProperty> properties) {
        this.properties = properties;
        return this;
    }

    public ProjectProperty findProperty(String name) {
        List<ProjectProperty> myProperties = this.properties;
        for (ProjectProperty property : myProperties) {
            if (name.equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ProjectProperties{" +
               "type='" + type + '\'' +
               ", description='" + description + '\'' +
               ", properties=" + properties +
               '}';
    }
}
