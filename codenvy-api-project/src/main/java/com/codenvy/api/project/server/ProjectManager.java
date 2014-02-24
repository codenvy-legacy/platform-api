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

import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
@Singleton
public final class ProjectManager {
    private final ProjectTypeRegistry               projectTypeRegistry;
    private final ProjectTypeDescriptionRegistry    typeDescriptionRegistry;
    private final Map<String, ValueProviderFactory> valueProviderFactories;
    private final VirtualFileSystemRegistry         registry;

    @Inject
    public ProjectManager(ProjectTypeRegistry projectTypeRegistry,
                          ProjectTypeDescriptionRegistry typeDescriptionRegistry,
                          Set<ValueProviderFactory> valueProviderFactories,
                          VirtualFileSystemRegistry registry) {
        this.projectTypeRegistry = projectTypeRegistry;
        this.typeDescriptionRegistry = typeDescriptionRegistry;
        this.valueProviderFactories = new HashMap<>();
        this.registry = registry;
        for (ValueProviderFactory valueProviderFactory : valueProviderFactories) {
            this.valueProviderFactories.put(valueProviderFactory.getName(), valueProviderFactory);
        }
    }

    public List<Project> getProjects(String workspace) {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final List<Project> projects = new ArrayList<>();
        for (FolderEntry f : myRoot.getChildFolders()) {
            if (f.isProjectFolder()) {
                projects.add(new Project(f, this));
            }
        }
        return projects;
    }

    public Project getProject(String workspace, String projectPath) {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final AbstractVirtualFileEntry child = myRoot.getChild(projectPath);
        if (child != null && child.isFolder() && ((FolderEntry)child).isProjectFolder()) {
            return new Project((FolderEntry)child, this);
        }
        return null;
    }

    public Project createProject(String workspace, String name, ProjectDescription projectDescription) throws IOException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final FolderEntry projectFolder = myRoot.createFolder(name);
        final Project project = new Project(projectFolder, this);
        project.updateDescription(projectDescription);
        return project;
    }

    public FolderEntry getProjectsRoot(String workspace) {
        final VirtualFile vfsRoot;
        try {
            vfsRoot = registry.getProvider(workspace).getMountPoint(true).getRoot();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
        return new FolderEntry(vfsRoot);
    }

    ProjectTypeRegistry getProjectTypeRegistry() {
        return projectTypeRegistry;
    }

    ProjectTypeDescriptionRegistry getTypeDescriptionRegistry() {
        return typeDescriptionRegistry;
    }

    Map<String, ValueProviderFactory> getValueProviderFactories() {
        return valueProviderFactories;
    }
}
