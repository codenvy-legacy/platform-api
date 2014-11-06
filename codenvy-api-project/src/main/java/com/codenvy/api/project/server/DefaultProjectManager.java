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
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

    private final ProjectTypeDescriptionRegistry    typeDescriptionRegistry;
    private final Map<String, ValueProviderFactory> valueProviderFactories;
    private final VirtualFileSystemRegistry         fileSystemRegistry;
    private final EventService                      eventService;
    private final EventSubscriber<VirtualFileEvent> vfsSubscriber;

    @Inject
    @SuppressWarnings("unchecked")
    public DefaultProjectManager(ProjectTypeDescriptionRegistry typeDescriptionRegistry,
                                 Set<ValueProviderFactory> valueProviderFactories,
                                 VirtualFileSystemRegistry fileSystemRegistry,
                                 EventService eventService) {
        this.typeDescriptionRegistry = typeDescriptionRegistry;
        this.fileSystemRegistry = fileSystemRegistry;
        this.valueProviderFactories = new HashMap<>();
        this.eventService = eventService;
        for (ValueProviderFactory valueProviderFactory : valueProviderFactories) {
            this.valueProviderFactories.put(valueProviderFactory.getName(), valueProviderFactory);
        }
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


    @Override
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

    @Override
    public Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final VirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
        if (child != null && child.isFolder() && ((FolderEntry)child).isProjectFolder()) {
            return new Project((FolderEntry)child, this);
        }
        return null;
    }

    @Override
    public Project createProject(String workspace, String name, ProjectDescription projectDescription)
            throws ConflictException, ForbiddenException, ServerException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final FolderEntry projectFolder = myRoot.createFolder(name);
        final Project project = new Project(projectFolder, this);
        project.updateDescription(projectDescription);
        getProjectMisc(project).setCreationDate(System.currentTimeMillis());
        return project;
    }

    @Override
    public FolderEntry getProjectsRoot(String workspace) throws ServerException {
        return new FolderEntry(workspace, fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot());
    }

    @Override
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

    @Override
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

    @Override
    public ProjectTypeDescriptionRegistry getTypeDescriptionRegistry() {
        return typeDescriptionRegistry;
    }

    @Override
    public Map<String, ValueProviderFactory> getValueProviderFactories() {
        return valueProviderFactories;
    }

    @Override
    public VirtualFileSystemRegistry getVirtualFileSystemRegistry() {
        return fileSystemRegistry;
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
}
