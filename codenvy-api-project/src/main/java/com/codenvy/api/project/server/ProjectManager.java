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
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.google.inject.ImplementedBy;

import java.util.List;
import java.util.Map;

/**
 * A manager for codenvy projects.
 *
 * @author andrew00x
 */
@ImplementedBy(DefaultProjectManager.class)
public interface ProjectManager {
    /**
     * Gets the list of projects in {@code workspace}.
     *
     * @param workspace
     *         id of workspace
     * @return the list of projects in specified workspace.
     * @throws ServerException
     *         if an error occurs
     */
    List<Project> getProjects(String workspace) throws ServerException;

    /**
     * Gets single project by id of workspace and project's path in this workspace.
     *
     * @param workspace
     *         id of workspace
     * @param projectPath
     *         project's path
     * @return requested project or {@code null} if project was not found
     * @throws ForbiddenException
     *         if user which perform operation doesn't have access to the requested project
     * @throws ServerException
     *         if other error occurs
     */
    Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException;

    /**
     * Creates new project.
     *
     * @param workspace
     *         id of workspace
     * @param name
     *         project's name
     * @param projectDescription
     *         project description
     * @return newly created project
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict if project with specified name already exists
     * @throws ForbiddenException
     *         if user which perform operation doesn't have required permissions
     * @throws ServerException
     *         if other error occurs
     */
    Project createProject(String workspace, String name, ProjectDescription projectDescription)
            throws ConflictException, ForbiddenException, ServerException;

    /**
     * Gets root folder od project tree.
     *
     * @param workspace
     *         id of workspace
     * @return root folder
     * @throws ServerException
     *         if an error occurs
     */
    FolderEntry getProjectsRoot(String workspace) throws ServerException;

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @return ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    ProjectMisc getProjectMisc(Project project) throws ServerException;

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @param misc
     *         ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    void saveProjectMisc(Project project, ProjectMisc misc) throws ServerException;

    /**
     * Gets ProjectTypeDescriptionRegistry.
     *
     * @see ProjectTypeDescriptionRegistry
     */
    ProjectTypeDescriptionRegistry getTypeDescriptionRegistry();

    Map<String, ValueProviderFactory> getValueProviderFactories();

    VirtualFileSystemRegistry getVirtualFileSystemRegistry();
}
