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

    public ProjectMisc getProjectMisc(String workspace, String project) {
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
                            cache[key.hashCode() & CACHE_MASK].get(key).data.setLong(ProjectMisc.UPDATED, System.currentTimeMillis());
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
                return new ProjectMisc(new InternalMisc(properties));
            }
        }
        return new ProjectMisc(new InternalMisc());
    }

    private void save(String workspace, String projectPath, ProjectMisc misc) throws IOException {
        final Project project = getProject(workspace, projectPath);
        if (project != null) {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            misc.data.properties.storeToXML(bout, null);
            final FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_FOLDER + "/misc.xml");
            if (miscFile != null) {
                miscFile.updateContent(bout.toByteArray(), "application/xml");
            } else {
                final FolderEntry codenvy = (FolderEntry)project.getBaseFolder().getChild(Constants.CODENVY_FOLDER);
                if (codenvy != null) {
                    codenvy.createFile("misc.xml", bout.toByteArray(), "application/xml");
                }
            }
        }
    }

    public static class ProjectMisc {
        static final String UPDATED    = "updated";

        private final InternalMisc data;

        private ProjectMisc(InternalMisc data) {
            this.data = data;
        }

        public long getModificationDate() {
            return data.getLong(UPDATED, -1L);
        }

        public void setValue(String name, boolean value) {
            data.setBoolean(name, value);
        }

        public boolean getBooleanValue(String name) {
            return data.getBoolean(name);
        }
    }

    private static class InternalMisc {
        final Properties properties;

        InternalMisc() {
            this(new Properties());
        }

        InternalMisc(Properties properties) {
            this.properties = properties;
        }

        String get(String name) {
            return properties.getProperty(name);
        }

        void set(String name, String value) {
            if (name == null) {
                throw new IllegalArgumentException("The name of property may not be null. ");
            }
            if (value == null) {
                properties.remove(name);
            } else {
                properties.setProperty(name, value);
            }
        }

        void setIfNotSet(String name, String value) {
            if (get(name) == null) {
                set(name, value);
            }
        }

        boolean getBoolean(String name) {
            return getBoolean(name, false);
        }

        boolean getBoolean(String name, boolean defaultValue) {
            final String str = get(name);
            return str == null ? defaultValue : Boolean.parseBoolean(str);
        }

        void setBoolean(String name, boolean value) {
            set(name, String.valueOf(value));
        }

        int getInt(String name) {
            return getInt(name, 0);
        }

        int getInt(String name, int defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setInt(String name, int value) {
            set(name, String.valueOf(value));
        }

        long getLong(String name) {
            return getLong(name, 0L);
        }

        long getLong(String name, long defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setLong(String name, long value) {
            set(name, String.valueOf(value));
        }

        float getFloat(String name) {
            return getFloat(name, 0.0F);
        }

        float getFloat(String name, float defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Float.parseFloat(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setFloat(String name, float value) {
            set(name, String.valueOf(value));
        }


        double getDouble(String name) {
            return getDouble(name, 0.0);
        }

        double getDouble(String name, double defaultValue) {
            final String str = get(name);
            if (str == null)
                return defaultValue;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        void setDouble(String name, double value) {
            set(name, String.valueOf(value));
        }

        Set<String> getNames() {
            return properties.stringPropertyNames();
        }

        int size() {
            return properties.size();
        }

        void clear() {
            properties.clear();
        }
    }
}
