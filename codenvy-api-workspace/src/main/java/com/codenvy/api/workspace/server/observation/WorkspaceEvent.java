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
package com.codenvy.api.workspace.server.observation;

/** @author Sergii Leschenko */
public abstract class WorkspaceEvent {
    public static enum ChangeType {
        CREATED("created"),
        DELETED("deleted");

        private final String value;

        private ChangeType(String value) {
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

    private String     workspaceId;
    private ChangeType type;

    protected WorkspaceEvent(String workspaceId, ChangeType type) {
        this.workspaceId = workspaceId;
        this.type = type;
    }

    protected WorkspaceEvent() {
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public ChangeType getType() {
        return type;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setType(ChangeType type) {
        this.type = type;
    }
}
