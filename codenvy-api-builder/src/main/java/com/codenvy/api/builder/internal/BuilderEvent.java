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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("builder")
public class BuilderEvent {
    public enum EventType {
        BEGIN("begin"),
        DONE("done");

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

    public static BuilderEvent beginEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.BEGIN, taskId, workspace, project);
    }

    public static BuilderEvent doneEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.DONE, taskId, workspace, project);
    }

    private EventType type;
    private long      taskId;
    private String    workspace;
    private String    project;

    public BuilderEvent(EventType type, long taskId, String workspace, String project) {
        this.type = type;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
    }

    public BuilderEvent() {
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
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

    @Override
    public String toString() {
        return "BuilderEvent{" +
               "type=" + type +
               ", taskId=" + taskId +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               '}';
    }
}
