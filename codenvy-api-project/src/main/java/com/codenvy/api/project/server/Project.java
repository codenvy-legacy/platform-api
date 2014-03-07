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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.dto.AccessControlEntry;
import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author andrew00x
 */
public class Project {

    private final FolderEntry    baseFolder;
    private final ProjectManager manager;

    public Project(FolderEntry baseFolder, ProjectManager manager) {
        this.baseFolder = baseFolder;
        this.manager = manager;
    }

    public String getName() {
        return baseFolder.getName();
    }

    public FolderEntry getBaseFolder() {
        return baseFolder;
    }

    public List<Project> getModules() {
        final List<Project> modules = new ArrayList<>();
        for (FolderEntry child : baseFolder.getChildFolders()) {
            if (child.isProjectFolder()) {
                modules.add(new Project(child, manager));
            }
        }
        return modules;
    }

    public Project createModule(String name, ProjectDescription projectDescription) throws IOException {
        final FolderEntry projectFolder = baseFolder.createFolder(name);
        final Project module = new Project(projectFolder, manager);
        module.updateDescription(projectDescription);
        return module;
    }

    public final ProjectDescription getDescription() throws IOException {
        // Create copy of original project descriptor.
        // Mainly do that to be independent to type of ValueProvider.
        // Caller gets description of project update some attributes or(and) type of project than caller sends description back with method
        // updateDescriptor.
        return new ProjectDescription(doGetDescription());
    }

    protected ProjectDescription doGetDescription() throws IOException {
        final ProjectProperties projectProperties = getProperties();
        final String projectTypeId = projectProperties.getType();
        if (projectTypeId == null) {
            return new ProjectDescription();
        }
        ProjectType projectType = manager.getProjectTypeRegistry().getProjectType(projectTypeId);
        if (projectType == null) {
            // TODO : Decide how should we treat such situation?
            // For now just show type what is set in configuration of project.
            projectType = new ProjectType(projectTypeId, projectTypeId);
        }
        final ProjectDescription projectDescription = new ProjectDescription(projectType);
        final ProjectTypeDescription projectTypeDescription = manager.getTypeDescriptionRegistry().getDescription(projectType);
        final List<Attribute> tmpList = new ArrayList<>();
        // Merge project's attributes.
        // 1. predefined
        for (Attribute attribute : manager.getTypeDescriptionRegistry().getPredefinedAttributes(projectType)) {
            tmpList.add(attribute);
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 2. "calculated"
        for (AttributeDescription attributeDescription : projectTypeDescription.getAttributeDescriptions()) {
            final ValueProviderFactory factory = manager.getValueProviderFactories().get(attributeDescription.getName());
            if (factory != null) {
                tmpList.add(new Attribute(attributeDescription.getName(), factory.newInstance(this)));
            }
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        // 3. persistent
        for (ProjectProperty property : projectProperties.getProperties()) {
            tmpList.add(new Attribute(property.getName(), property.getValue()));
        }
        projectDescription.setAttributes(tmpList);
        tmpList.clear();
        projectDescription.setDescription(projectProperties.getDescription());
        return projectDescription;
    }

    public final void updateDescription(ProjectDescription projectDescriptionUpdate) throws IOException {
        final ProjectDescription thisProjectDescription = doGetDescription();
        final ProjectProperties projectProperties = new ProjectProperties();
        projectProperties.setType(projectDescriptionUpdate.getProjectType().getId());
        for (Attribute attributeUpdate : projectDescriptionUpdate.getAttributes()) {
            final String attributeUpdateName = attributeUpdate.getName();
            Attribute thisAttribute = null;
            if (thisProjectDescription != null) {
                thisAttribute = thisProjectDescription.getAttribute(attributeUpdateName);
            }
            if (thisAttribute != null) {
                // Don't store attributes as properties of project if have specific ValueProvider.
                if (manager.getValueProviderFactories().get(attributeUpdateName) == null) {
                    // If don't have special ValueProvider then store attribute as project's property.
                    projectProperties.getProperties().add(new ProjectProperty(attributeUpdateName, attributeUpdate.getValues()));
                } else {
                    thisAttribute.setValues(attributeUpdate.getValues());
                }
            } else {
                // New attribute - save it in properties.
                projectProperties.getProperties().add(new ProjectProperty(attributeUpdateName, attributeUpdate.getValues()));
            }
        }
        projectProperties.setDescription(projectDescriptionUpdate.getDescription());
        setProperties(projectProperties);
    }

    protected ProjectProperties getProperties() throws IOException {
        final AbstractVirtualFileEntry projectFile = baseFolder.getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        if (projectFile == null || !projectFile.isFile()) {
            return new ProjectProperties();
        }
        try (InputStream inputStream = ((FileEntry)projectFile).getInputStream()) {
            return JsonHelper.fromJson(inputStream, ProjectProperties.class, null);
        } catch (JsonParseException e) {
            throw new ProjectStructureConstraintException("Unable parse project properties. " + e.getMessage());
        }
    }

    protected void setProperties(ProjectProperties properties) throws IOException {
        AbstractVirtualFileEntry projectFile = baseFolder.getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        if (projectFile != null) {
            if (!projectFile.isFile()) {
                throw new ProjectStructureConstraintException(
                        String.format("Unable save project properties. Path %s/%s exists but is not a file.",
                                      baseFolder.getPath(), Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
            }
            ((FileEntry)projectFile).updateContent(JsonHelper.toJson(properties).getBytes());
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
                                                 JsonHelper.toJson(properties).getBytes(),
                                                 "application/json");
        }
    }

    public String getVisibility() {
        try {
            final Principal guest = DtoFactory.getInstance().createDto(Principal.class)
                                              .withName("any")
                                              .withType(Principal.Type.USER);
            final List<AccessControlEntry> acl = baseFolder.getVirtualFile().getACL();
            if (acl.isEmpty()) {
                return "public";
            }
            for (AccessControlEntry ace : acl) {
                if (guest.equals(ace.getPrincipal()) && ace.getPermissions().contains("read")) {
                    return "public";
                }
            }
            return "private";
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public void setVisibility(String projectVisibility) {
        try {
            switch (projectVisibility) {
                case "private":
                    final List<AccessControlEntry> acl = new ArrayList<>(2);
                    final Principal developer = DtoFactory.getInstance().createDto(Principal.class)
                                                          .withName("workspace/developer")
                                                          .withType(Principal.Type.GROUP);
                    acl.add(DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                      .withPrincipal(developer)
                                      .withPermissions(Arrays.asList("all")));
                    baseFolder.getVirtualFile().updateACL(acl, true, null);
                    break;
                case "public":
                    // Remove ACL. Default behaviour of underlying virtual filesystem: everyone can read but can't update.
                    baseFolder.getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
                    break;
            }
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }
}
