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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("runner")
public class RunnerEvent {
    public enum EventType {
        STARTED("started"),
        STOPPED("stopped"),
        ERROR("error");

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
    private long      taskId;
    private String    workspace;
    private String    project;
    private String    errorMessage;

    public RunnerEvent(EventType type, long taskId, String workspace, String project, String errorMessage) {
        this.type = type;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
        this.errorMessage = errorMessage;
    }

    public RunnerEvent(EventType type, long taskId, String workspace, String project) {
        this(type, taskId, workspace, project, null);
    }

    public RunnerEvent() {
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "RunnerEvent{" +
               "type=" + type +
               ", taskId=" + taskId +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }
}
