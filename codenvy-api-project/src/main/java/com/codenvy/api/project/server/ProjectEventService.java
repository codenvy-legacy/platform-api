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
import com.codenvy.api.vfs.server.observation.MoveEvent;
import com.codenvy.api.vfs.server.observation.RenameEvent;
import com.codenvy.api.vfs.server.observation.VirtualFileEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author andrew00x
 */
@Singleton
public final class ProjectEventService {
    private final EventService                    eventService;
    private final Set<VirtualFileEventSubscriber> subscribers;

    @Inject
    public ProjectEventService(EventService eventService) {
        this.eventService = eventService;
        subscribers = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(String workspace, String project, ProjectEventListener listener) {
        final VirtualFileEventSubscriber subscriber = new VirtualFileEventSubscriber(workspace, project, listener);
        if (subscribers.add(subscriber)) {
            eventService.subscribe(subscriber);
            return true;
        }
        return false;
    }

    public boolean removeListener(String workspace, String project, ProjectEventListener listener) {
        VirtualFileEventSubscriber mySubscriber = null;
        for (VirtualFileEventSubscriber subscriber : subscribers) {
            if (workspace.equals(subscriber.workspace) && project.equals(subscriber.project) && listener.equals(subscriber.listener)) {
                mySubscriber = subscriber;
                break;
            }
        }
        if (mySubscriber != null) {
            subscribers.remove(mySubscriber);
            eventService.unsubscribe(mySubscriber);
            return true;
        }
        return false;
    }

    private static class VirtualFileEventSubscriber implements EventSubscriber<VirtualFileEvent> {
        final String               workspace;
        final String               project;
        final ProjectEventListener listener;

        String projectPath;

        VirtualFileEventSubscriber(String workspace, String project, ProjectEventListener listener) {
            this.workspace = workspace;
            this.project = project;
            this.listener = listener;
            projectPath = project;
            if (!projectPath.startsWith("/")) {
                projectPath = '/' + projectPath;
            }
            if (!projectPath.endsWith("/")) {
                projectPath = projectPath + '/';
            }
        }

        @Override
        public void onEvent(VirtualFileEvent event) {
            final VirtualFileEvent.ChangeType eventType = event.getType();
            final String eventWorkspace = event.getWorkspaceId();
            if (workspace.equals(eventWorkspace)) {
                final String eventPath = event.getPath();
                if (eventPath.startsWith(projectPath)) {
                    if (eventType == VirtualFileEvent.ChangeType.CONTENT_UPDATED) {
                        listener.onEvent(new ProjectEvent(ProjectEvent.EventType.UPDATED, workspace, project,
                                                          eventPath.substring(projectPath.length())));
                    } else if (eventType == VirtualFileEvent.ChangeType.CREATED) {
                        listener.onEvent(new ProjectEvent(ProjectEvent.EventType.CREATED, workspace, project,
                                                          eventPath.substring(projectPath.length())));
                    } else if (eventType == VirtualFileEvent.ChangeType.DELETED) {
                        listener.onEvent(new ProjectEvent(ProjectEvent.EventType.DELETED, workspace, project,
                                                          eventPath.substring(projectPath.length())));
                    } else if (eventType == VirtualFileEvent.ChangeType.MOVED) {
                        listener.onEvent(new ProjectEvent(ProjectEvent.EventType.CREATED, workspace, project,
                                                          eventPath.substring(projectPath.length())));
                    } else if (eventType == VirtualFileEvent.ChangeType.RENAMED) {
                        listener.onEvent(new ProjectEvent(ProjectEvent.EventType.CREATED, workspace, project,
                                                          eventPath.substring(projectPath.length())));
                    }
                }
                String eventOldPath = null;
                // rename and move are treated as create and delete
                if (eventType == VirtualFileEvent.ChangeType.MOVED) {
                    eventOldPath = ((MoveEvent)event).getOldPath();
                } else if (eventType == VirtualFileEvent.ChangeType.RENAMED) {
                    eventOldPath = ((RenameEvent)event).getOldPath();
                }
                if (eventOldPath != null && eventOldPath.startsWith(projectPath)) {
                    listener.onEvent(new ProjectEvent(ProjectEvent.EventType.DELETED, workspace, project,
                                                      eventOldPath.substring(projectPath.length())));
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof VirtualFileEventSubscriber)) {
                return false;
            }
            final VirtualFileEventSubscriber other = (VirtualFileEventSubscriber)o;
            return listener.equals(other.listener) && project.equals(other.project) && workspace.equals(other.workspace);
        }

        @Override
        public int hashCode() {
            int hashCode = 7;
            hashCode = 31 * hashCode + workspace.hashCode();
            hashCode = 31 * hashCode + project.hashCode();
            hashCode = 31 * hashCode + listener.hashCode();
            return hashCode;
        }
    }
}
