/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.project.server.handlers.CreateModuleHandler;
import com.codenvy.api.project.server.handlers.CreateProjectHandler;
import com.codenvy.api.project.server.handlers.ProjectHandlerRegistry;
import com.codenvy.api.project.server.type.*;
import com.codenvy.api.project.shared.dto.GeneratorDescription;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.observation.VirtualFileEvent;

import com.codenvy.commons.lang.Pair;
import com.codenvy.commons.lang.cache.Cache;
import com.codenvy.commons.lang.cache.SLRUCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author andrew00x
 */
@Singleton
public final class DefaultProjectManager implements ProjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectManager.class);

    private static final int CACHE_NUM  = 1 << 2;
    private static final int CACHE_MASK = CACHE_NUM - 1;
    private static final int SEG_SIZE   = 32;

    private final Lock[]                                     miscLocks;
    private final Cache<Pair<String, String>, ProjectMisc>[] miscCaches;

    private final VirtualFileSystemRegistry         fileSystemRegistry;
    private final EventService                      eventService;
    private final EventSubscriber<VirtualFileEvent> vfsSubscriber;
    private final ProjectTypeRegistry projectTypeRegistry;
    private final ProjectHandlerRegistry handlers;



    @Inject
    @SuppressWarnings("unchecked")
    public DefaultProjectManager(VirtualFileSystemRegistry fileSystemRegistry,
                                 EventService eventService,
                                 ProjectTypeRegistry projectTypeRegistry,
                                 ProjectHandlerRegistry handlers) {

        this.fileSystemRegistry = fileSystemRegistry;
        this.eventService = eventService;
        this.projectTypeRegistry = projectTypeRegistry;
        //this.handler = handler;
        this.handlers = handlers;


        this.miscCaches = new Cache[CACHE_NUM];
        this.miscLocks = new Lock[CACHE_NUM];
        for (int i = 0; i < CACHE_NUM; i++) {
            miscLocks[i] = new ReentrantLock();
            miscCaches[i] = new SLRUCache<Pair<String, String>, ProjectMisc>(SEG_SIZE, SEG_SIZE) {
                @Override
                protected void evict(Pair<String, String> key, ProjectMisc value) {
                    if (value.isUpdated()) {
                        final int index = key.hashCode() & CACHE_MASK;
                        miscLocks[index].lock();
                        try {
                            writeProjectMisc(value.getProject(), value);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        } finally {
                            miscLocks[index].unlock();
                        }
                        super.evict(key, value);
                    }
                }
            };
        }

        vfsSubscriber = new EventSubscriber<VirtualFileEvent>() {
            @Override
            public void onEvent(VirtualFileEvent event) {
                final String workspace = event.getWorkspaceId();
                final String path = event.getPath();
                if (path.endsWith(Constants.CODENVY_DIR + "/misc.xml")) {
                    return;
                }
                switch (event.getType()) {
                    case CONTENT_UPDATED:
                    case CREATED:
                    case DELETED:
                    case MOVED:
                    case RENAMED: {
                        final int length = path.length();
                        for (int i = 1; i < length && (i = path.indexOf('/', i)) > 0; i++) {
                            final String projectPath = path.substring(0, i);
                            try {
                                final Project project = getProject(workspace, projectPath);
                                if (project != null) {
                                    getProjectMisc(project).setModificationDate(System.currentTimeMillis());
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                        break;
                    }
                }
            }
        };
    }


    /**
     * Gets the list of projects in {@code workspace}.
     *
     * @param workspace
     *         id of workspace
     * @return the list of projects in specified workspace.
     * @throws ServerException
     *         if an error occurs
     */
    public List<Project> getProjects(String workspace) throws ServerException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final List<Project> projects = new ArrayList<>();
        for (FolderEntry folder : myRoot.getChildFolders()) {
            if (folder.isProjectFolder()) {
                projects.add(new Project(folder, this));
            }
        }
        return projects;
    }

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
    public Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final VirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
        if (child != null && child.isFolder() && ((FolderEntry)child).isProjectFolder()) {
            return new Project((FolderEntry)child, this);
        }
        return null;
    }


    /**
     *
     * Creates new project.
     *
     * @param workspace
     *         id of workspace
     * @param name
     *         project's name
     * @param projectConfig
     *         project description
     * @return newly created project
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict if project with specified name already exists
     * @throws ForbiddenException
     *         if user which perform operation doesn't have required permissions
     * @throws ServerException
     *         if other error occurs
     */
    public Project createProject(String workspace, String name, ProjectConfig projectConfig, Map<String, String> options,
                                 String visibility)
            throws ConflictException, ForbiddenException, ServerException, ProjectTypeConstraintException {


        final FolderEntry myRoot = getProjectsRoot(workspace);
        final FolderEntry projectFolder = myRoot.createFolder(name);
        final Project project = new Project(projectFolder, this);

        final CreateProjectHandler generator = handlers.getCreateProjectHandler(projectConfig.getTypeId());

        if (generator != null) {
            generator.onCreateProject(project.getBaseFolder(),
                    projectConfig.getAttributes(), options);
        }

        project.updateConfig(projectConfig);

        final ProjectMisc misc = project.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!

        if (visibility != null) {
            project.setVisibility(visibility);
        }

        return project;
    }

    public Project addModule(String workspace, String projectPath, String modulePath, ProjectConfig moduleConfig, Map<String,
            String> options, String visibility)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {

        String absModulePath = modulePath.startsWith("/")?modulePath:projectPath+"/"+modulePath;
        Project module = getProject(workspace, absModulePath);

        if(module == null) {

            if(moduleConfig == null)
                throw new ConflictException("Module not found on "+absModulePath+" and module configuration is not defined");

            String parentPath = com.codenvy.api.vfs.server.Path.fromString(absModulePath).getParent().toString();
            String name = com.codenvy.api.vfs.server.Path.fromString(modulePath).getName();
            final VirtualFileEntry parentFolder = getProjectsRoot(workspace).getChild(parentPath);
            if(parentFolder == null || parentFolder.isFile())
                throw new NotFoundException("Parent Folder not found "+parentPath);

            VirtualFileEntry moduleFolder = ((FolderEntry)parentFolder).getChild(name);
            if(moduleFolder == null)
                moduleFolder = ((FolderEntry)parentFolder).createFolder(name);
            else if(moduleFolder.isFile())
                throw new ConflictException("Item exists on "+absModulePath+" but is not a folder or project");

            module = new Project((FolderEntry)moduleFolder, this);


            module.updateConfig(moduleConfig);

            final ProjectMisc misc = module.getMisc();
            misc.setCreationDate(System.currentTimeMillis());
            misc.save(); // Important to save misc!!

            if (visibility != null) {
                module.setVisibility(visibility);
            }

            final CreateProjectHandler generator = this.getHandlers().getCreateProjectHandler(moduleConfig.getTypeId());
            if (generator != null) {
                generator.onCreateProject(module.getBaseFolder(), module.getConfig().getAttributes(), options);
            }

        }

        Project parentProject = getProject(workspace, projectPath);
        parentProject.getModules().add(modulePath);


        CreateModuleHandler moduleHandler = this.getHandlers().getCreateModuleHandler(parentProject.getConfig().getTypeId());
        if (moduleHandler != null) {
            moduleHandler.onCreateModule(parentProject.getBaseFolder(), absModulePath, module.getConfig(), options);
        }
        return module;

    }

    /**
     * Gets root folder od project tree.
     *
     * @param workspace
     *         id of workspace
     * @return root folder
     * @throws ServerException
     *         if an error occurs
     */
    public FolderEntry getProjectsRoot(String workspace) throws ServerException {
        return new FolderEntry(workspace, fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot());
    }

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
    public ProjectMisc getProjectMisc(Project project) throws ServerException {
        final String workspace = project.getWorkspace();
        final String path = project.getPath();
        final Pair<String, String> key = Pair.of(workspace, path);
        final int index = key.hashCode() & CACHE_MASK;
        miscLocks[index].lock();
        try {
            ProjectMisc misc = miscCaches[index].get(key);
            if (misc == null) {
                miscCaches[index].put(key, misc = readProjectMisc(project));
            }
            return misc;
        } finally {
            miscLocks[index].unlock();
        }
    }

    private ProjectMisc readProjectMisc(Project project) throws ServerException {
        try {
            ProjectMisc misc;
            final FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_DIR + "/misc.xml");
            if (miscFile != null) {
                try (InputStream in = miscFile.getInputStream()) {
                    final Properties properties = new Properties();
                    properties.loadFromXML(in);
                    misc = new ProjectMisc(properties, project);
                } catch (IOException e) {
                    throw new ServerException(e.getMessage(), e);
                }
            } else {
                misc = new ProjectMisc(project);
            }
            return misc;
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
    }

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
    public void saveProjectMisc(Project project, ProjectMisc misc) throws ServerException {
        if (misc.isUpdated()) {
            final String workspace = project.getWorkspace();
            final String path = project.getPath();
            final Pair<String, String> key = Pair.of(workspace, path);
            final int index = key.hashCode() & CACHE_MASK;
            miscLocks[index].lock();
            try {
                miscCaches[index].remove(key);
                writeProjectMisc(project, misc);
                miscCaches[index].put(key, misc);
            } finally {
                miscLocks[index].unlock();
            }
        }
    }

    private void writeProjectMisc(Project project, ProjectMisc misc) throws ServerException {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                misc.asProperties().storeToXML(bout, null);
            } catch (IOException e) {
                throw new ServerException(e.getMessage(), e);
            }
            FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_DIR + "/misc.xml");
            if (miscFile != null) {
                miscFile.updateContent(bout.toByteArray(), null);
            } else {
                FolderEntry codenvy = (FolderEntry)project.getBaseFolder().getChild(Constants.CODENVY_DIR);
                if (codenvy == null) {
                    try {
                        codenvy = project.getBaseFolder().createFolder(Constants.CODENVY_DIR);
                    } catch (ConflictException e) {
                        // Already checked existence of folder ".codenvy".
                        throw new ServerException(e.getServiceError());
                    }
                }
                try {
                    codenvy.createFile("misc.xml", bout.toByteArray(), null);
                } catch (ConflictException e) {
                    // Not expected, existence of file already checked
                    throw new ServerException(e.getServiceError());
                }
            }
            LOG.debug("Save misc file of project {} in {}", project.getPath(), project.getWorkspace());
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
    }



    @PostConstruct
    void start() {
        eventService.subscribe(vfsSubscriber);
    }

    @PreDestroy
    void stop() {
        eventService.unsubscribe(vfsSubscriber);
        for (int i = 0, length = miscLocks.length; i < length; i++) {
            miscLocks[i].lock();
            try {
                miscCaches[i].clear();
            } finally {
                miscLocks[i].unlock();
            }
        }
    }


    public VirtualFileSystemRegistry getVirtualFileSystemRegistry() {
        return fileSystemRegistry;
    }

    public ProjectTypeRegistry getProjectTypeRegistry() {
        return this.projectTypeRegistry;
    }

    public ProjectHandlerRegistry getHandlers() {
        return handlers;
    }

    public Map<String, AttributeValue> estimateProject(String workspace, String path, String projectTypeId)
            throws ServerException, ForbiddenException, NotFoundException, ValueStorageException,
            ProjectTypeConstraintException {


        ProjectType projectType = projectTypeRegistry.getProjectType(projectTypeId);
        if(projectType == null)
            throw new NotFoundException("Project Type "+projectTypeId+" not found.");

        final VirtualFileEntry baseFolder = getProjectsRoot(workspace).getChild(path.startsWith("/") ? path.substring(1) : path);
        if (!baseFolder.isFolder()) {
            throw new NotFoundException("Not a folder: "+path);
        }

        Map<String, AttributeValue> attributes = new HashMap<>();

        for (Attribute attr : projectType.getAttributes()) {

            if (attr.isVariable() && ((Variable) attr).getValueProviderFactory() != null) {

                Variable var = (Variable) attr;

                try {
                    attributes.put(attr.getName(), var.getValue((FolderEntry) baseFolder));
                } catch (ValueStorageException e) {
                    if(var.isRequired())
                        throw e;
                    else
                        attributes.put(attr.getName(), null);
                }
            }

        }

        return attributes;

    }
}
