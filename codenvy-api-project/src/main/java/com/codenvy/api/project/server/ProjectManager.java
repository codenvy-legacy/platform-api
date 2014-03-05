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
import com.codenvy.api.project.shared.dto.ProjectUpdateEvent;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.server.observation.CreateEvent;
import com.codenvy.api.vfs.server.observation.DeleteEvent;
import com.codenvy.api.vfs.server.observation.EventListener;
import com.codenvy.api.vfs.server.observation.MoveEvent;
import com.codenvy.api.vfs.server.observation.NotificationService;
import com.codenvy.api.vfs.server.observation.RenameEvent;
import com.codenvy.api.vfs.server.observation.UpdateACLEvent;
import com.codenvy.api.vfs.server.observation.UpdateContentEvent;
import com.codenvy.api.vfs.server.observation.UpdatePropertiesEvent;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(ProjectManager.class);

    static {
        NotificationService.register(new ProjectUpdateListener());
    }

    private final ProjectTypeRegistry               projectTypeRegistry;
    private final ProjectTypeDescriptionRegistry    typeDescriptionRegistry;
    private final Map<String, ValueProviderFactory> valueProviderFactories;
    private final VirtualFileSystemRegistry         fileSystemRegistry;

    @Inject
    public ProjectManager(ProjectTypeRegistry projectTypeRegistry,
                          ProjectTypeDescriptionRegistry typeDescriptionRegistry,
                          Set<ValueProviderFactory> valueProviderFactories,
                          VirtualFileSystemRegistry fileSystemRegistry) {
        this.projectTypeRegistry = projectTypeRegistry;
        this.typeDescriptionRegistry = typeDescriptionRegistry;
        this.fileSystemRegistry = fileSystemRegistry;
        this.valueProviderFactories = new HashMap<>();
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
        final AbstractVirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
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
            vfsRoot = fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
        return new FolderEntry(vfsRoot);
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

    public static class ProjectUpdateListener implements EventListener {
        @Override
        public void create(CreateEvent event) {
            try {
                DtoFactory.getInstance().createDto(ProjectUpdateEvent.class)
                          .withEventType(event.getType().toString())
                          .withFolder(event.getVirtualFile().isFolder())
                          .withMediaType(event.getVirtualFile().getMediaType())
                          .withPath(event.getVirtualFile().getPath());
            } catch (VirtualFileSystemException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void move(MoveEvent event) {
            try {
                DtoFactory.getInstance().createDto(ProjectUpdateEvent.class)
                          .withEventType(event.getType().toString())
                          .withFolder(event.getVirtualFile().isFolder())
                          .withMediaType(event.getVirtualFile().getMediaType())
                          .withPath(event.getVirtualFile().getPath())
                          .withPath(event.getOldPath());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void rename(RenameEvent event) {
            try {
                DtoFactory.getInstance().createDto(ProjectUpdateEvent.class)
                          .withEventType(event.getType().toString())
                          .withFolder(event.getVirtualFile().isFolder())
                          .withMediaType(event.getVirtualFile().getMediaType())
                          .withPath(event.getVirtualFile().getPath())
                          .withPath(event.getOldPath());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void delete(DeleteEvent event) {
            DtoFactory.getInstance().createDto(ProjectUpdateEvent.class)
                      .withEventType(event.getType().toString())
                      .withFolder(event.isFolder())
                      .withPath(event.getPath());
        }

        @Override
        public void updateContent(UpdateContentEvent event) {
            try {
                send(DtoFactory.getInstance().createDto(ProjectUpdateEvent.class)
                               .withEventType(event.getType().toString())
                               .withFolder(event.getVirtualFile().isFolder())
                               .withMediaType(event.getVirtualFile().getMediaType())
                               .withPath(event.getVirtualFile().getPath()));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void updateProperties(UpdatePropertiesEvent event) {
        }

        @Override
        public void updateACL(UpdateACLEvent event) {
        }

        private void send(ProjectUpdateEvent updateEvent) throws Exception {
//            final ChannelBroadcastMessage message = new ChannelBroadcastMessage();
//            message.setChannel("project:update");
//            message.setType(ChannelBroadcastMessage.Type.NONE);
//            message.setUuid(NameGenerator.generate(null, 16));
//            message.setBody(DtoFactory.getInstance().toJson(updateEvent));
//            WSConnectionContext.sendMessage(message);
        }
    }
}
