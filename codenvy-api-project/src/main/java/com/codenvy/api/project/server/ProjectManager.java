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

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.VirtualFileEvent;
import com.codenvy.commons.lang.cache.Cache;
import com.codenvy.commons.lang.cache.LoadingValueSLRUCache;
import com.codenvy.commons.lang.cache.SynchronizedCache;

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

/**
 * @author andrew00x
 */
@Singleton
public final class ProjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectManager.class);

    private static final int CACHE_NUM  = 1 << 2;
    private static final int CACHE_MASK = CACHE_NUM - 1;
    private static final int SEG_SIZE   = 32;

    private final Cache<Pair<String, String>, ProjectMisc>[] cache;

    private final ProjectTypeRegistry               projectTypeRegistry;
    private final ProjectTypeDescriptionRegistry    typeDescriptionRegistry;
    private final Map<String, ValueProviderFactory> valueProviderFactories;
    private final VirtualFileSystemRegistry         fileSystemRegistry;
    private final EventService                      eventService;

    @Inject
    @SuppressWarnings("unchecked")
    public ProjectManager(ProjectTypeRegistry projectTypeRegistry,
                          ProjectTypeDescriptionRegistry typeDescriptionRegistry,
                          Set<ValueProviderFactory> valueProviderFactories,
                          VirtualFileSystemRegistry fileSystemRegistry,
                          EventService eventService) {
        this.projectTypeRegistry = projectTypeRegistry;
        this.typeDescriptionRegistry = typeDescriptionRegistry;
        this.fileSystemRegistry = fileSystemRegistry;
        this.valueProviderFactories = new HashMap<>();
        this.eventService = eventService;
        for (ValueProviderFactory valueProviderFactory : valueProviderFactories) {
            this.valueProviderFactories.put(valueProviderFactory.getName(), valueProviderFactory);
        }
        this.cache = new Cache[CACHE_NUM];
        for (int i = 0; i < CACHE_NUM; i++) {
            cache[i] = new SynchronizedCache<>(new LoadingValueSLRUCache<Pair<String, String>, ProjectMisc>(SEG_SIZE, SEG_SIZE) {
                @Override
                protected ProjectMisc loadValue(Pair<String, String> key) {
                    try {
                        return load(key.first, key.second);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                protected void evict(Pair<String, String> key, ProjectMisc value) {
                    try {
                        save(key.first, key.second, value);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                    super.evict(key, value);
                }
            });
        }
    }

    public List<Project> getProjects(String workspace) {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final List<Project> projects = new ArrayList<>();
        for (FolderEntry f : myRoot.getChildFolders()) {
            if (f.isProjectFolder()) {
                projects.add(new Project(workspace, f, this));
            }
        }
        return projects;
    }

    public Project getProject(String workspace, String projectPath) {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final AbstractVirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
        if (child != null && child.isFolder() && ((FolderEntry)child).isProjectFolder()) {
            return new Project(workspace, (FolderEntry)child, this);
        }
        return null;
    }

    public Project createProject(String workspace, String name, ProjectDescription projectDescription) throws IOException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final FolderEntry projectFolder = myRoot.createFolder(name);
        final Project project = new Project(workspace, projectFolder, this);
        project.updateDescription(projectDescription);
        getProjectMisc(workspace, projectFolder.getPath()).setCreationDate(System.currentTimeMillis());
        return project;
    }

    public FolderEntry getProjectsRoot(String workspace) {
        final VirtualFile vfsRoot;
        try {
            vfsRoot = fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
        return new FolderEntry(vfsRoot);
    }

    ProjectMisc getProjectMisc(String workspace, String project) {
        final Pair<String, String> key = Pair.of(workspace, project);
        return cache[key.hashCode() & CACHE_MASK].get(key);
    }

    public ProjectTypeRegistry getProjectTypeRegistry() {
        return projectTypeRegistry;
    }

    public ProjectTypeDescriptionRegistry getTypeDescriptionRegistry() {
        return typeDescriptionRegistry;
    }

    public Map<String, ValueProviderFactory> getValueProviderFactories() {
        return valueProviderFactories;
    }

    public VirtualFileSystemRegistry getVirtualFileSystemRegistry() {
        return fileSystemRegistry;
    }

    @PostConstruct
    private void start() {
        eventService.subscribe(new EventSubscriber<VirtualFileEvent>() {
            @Override
            public void onEvent(VirtualFileEvent event) {
                final String workspace = event.getWorkspaceId();
                final String path = event.getPath();
                final int length = path.length();
                switch (event.getType()) {
                    case CONTENT_UPDATED:
                    case CREATED:
                    case DELETED:
                    case MOVED:
                    case RENAMED: {
                        for (int i = 1; i < length && (i = path.indexOf('/', i)) > 0; i++) {
                            final String projectPath = path.substring(0, i);
                            if (getProject(workspace, projectPath) == null) {
                                break;
                            }
                            final Pair<String, String> key = Pair.of(workspace, projectPath);
                            cache[key.hashCode() & CACHE_MASK].get(key).setModificationDate(System.currentTimeMillis());
                        }
                        break;
                    }
                }
            }
        });
    }

    @PreDestroy
    private void stop() {
        for (Cache<Pair<String, String>, ProjectMisc> e : cache) {
            e.clear();
        }
    }

    private ProjectMisc load(String workspace, String projectPath) throws IOException {
        final Project project = getProject(workspace, projectPath);
        if (project == null) {
            return null;
        }
        final FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_FOLDER + "/misc.xml");
        if (miscFile != null) {
            try (InputStream in = miscFile.getInputStream()) {
                final Properties properties = new Properties();
                properties.loadFromXML(in);
                return new ProjectMisc(properties);
            }
        }
        return new ProjectMisc();
    }

    void save(String workspace, String projectPath, ProjectMisc misc) throws IOException {
        if (misc.isUpdated()) {
            final Project project = getProject(workspace, projectPath);
            // be sure project exists
            if (project != null) {
                final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                misc.asProperties().storeToXML(bout, null);
                final FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_FOLDER + "/misc.xml");
                if (miscFile != null) {
                    miscFile.updateContent(bout.toByteArray(), "application/xml");
                } else {
                    final FolderEntry codenvy = (FolderEntry)project.getBaseFolder().getChild(Constants.CODENVY_FOLDER);
                    if (codenvy != null) {
                        codenvy.createFile("misc.xml", bout.toByteArray(), "application/xml");
                    }
                }
                LOG.debug("Save misc file of project {} in {}", projectPath, workspace);
            }
        }
    }
}
