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
package com.codenvy.api.project.server;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectProperties {
    public static ProjectProperties load(Project project) throws ServerException {
        final VirtualFileEntry projectFile;
        try {
            projectFile = project.getBaseFolder().getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
        if (projectFile == null || !projectFile.isFile()) {
            return new ProjectProperties();
        }
        try (InputStream inputStream = ((FileEntry)projectFile).getInputStream()) {
            return load(inputStream);
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    public static ProjectProperties load(InputStream inputStream) throws IOException {
        try {
            return JsonHelper.fromJson(inputStream, ProjectProperties.class, null);
        } catch (JsonParseException e) {
            throw new IOException("Unable to parse the project's property file.  " +
                                  "Check the project.json file for corruption or modification.  Consider reloading the project. " +
                                  e.getMessage());
        }
    }

    public void save(Project project) throws ServerException {
        try {
            final FolderEntry baseFolder = project.getBaseFolder();
            VirtualFileEntry projectFile = baseFolder.getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
            if (projectFile != null) {
                if (!projectFile.isFile()) {
                    throw new ServerException(String.format(
                            "Unable to save the project's properties to the file system. Path %s/%s exists but is not a file.",
                            baseFolder.getPath(), Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
                }
                ((FileEntry)projectFile).updateContent(JsonHelper.toJson(this).getBytes());
            } else {
                VirtualFileEntry codenvyDir = baseFolder.getChild(Constants.CODENVY_FOLDER);
                if (codenvyDir == null) {
                    try {
                        codenvyDir = baseFolder.createFolder(Constants.CODENVY_FOLDER);
                    } catch (ConflictException e) {
                        // Already checked existence of folder ".codenvy".
                        throw new ServerException(e.getServiceError());
                    }
                    // Need to be able update files in .codenvy folder independently to user actions.
                    final List<AccessControlEntry> acl = new ArrayList<>(1);
                    acl.add(DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                      .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                               .withName("any")
                                                               .withType(Principal.Type.USER))
                                      .withPermissions(Arrays.asList("all")));
                    codenvyDir.getVirtualFile().updateACL(acl, true, null);
                } else if (!codenvyDir.isFolder()) {
                    throw new ServerException(String.format(
                            "Unable to save the project's properties to the file system. Path %s/%s exists but is not a folder.",
                            baseFolder.getPath(), Constants.CODENVY_FOLDER));
                }
                try {
                    ((FolderEntry)codenvyDir)
                            .createFile(Constants.CODENVY_PROJECT_FILE, JsonHelper.toJson(this).getBytes(), "application/json");
                } catch (ConflictException e) {
                    // Already checked existence of file ".codenvy/project.json".
                    throw new ServerException(e.getServiceError());
                }
            }
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
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

    public String getPropertyValue(String name) {
        final ProjectProperty myProperty = findProperty(name);
        if (myProperty != null) {
            final List<String> value = myProperty.getValue();
            if (value != null && !value.isEmpty()) {
                return value.get(0);
            }
        }
        return null;
    }

    public List<String> getPropertyValues(String name) {
        final ProjectProperty myProperty = findProperty(name);
        if (myProperty != null) {
            final List<String> value = myProperty.getValue();
            if (value != null) {
                return new ArrayList<>(value);
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
