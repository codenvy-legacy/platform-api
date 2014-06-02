/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.project.server;

/**
 * @author andrew00x
 */
public class ProjectEvent {
    public static enum EventType {
        UPDATED("updated"),
        CREATED("created"),
        DELETED("deleted");

        private final String value;

        private EventType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private EventType type;
    private String    workspace;
    private String    project;
    private String    path;

    public ProjectEvent(EventType type, String workspace, String project, String path) {
        this.type = type;
        this.workspace = workspace;
        this.project = project;
        this.path = path;
    }

    public ProjectEvent() {
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ProjectEvent{" +
               "type=" + type +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               ", path='" + path + '\'' +
               '}';
    }
}
