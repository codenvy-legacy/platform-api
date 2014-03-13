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
package com.codenvy.api.vfs.server.observation;

/**
 * @author andrew00x
 */
public abstract class VirtualFileEvent {
    public static enum ChangeType {
        ACL_UPDATED("acl_updated"),
        CONTENT_UPDATED("content_updated"),
        CREATED("created"),
        DELETED("deleted"),
        MOVED("moved"),
        PROPERTIES_UPDATED("properties_updated"),
        RENAMED("renamed");

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
    private String     path;
    private ChangeType type;

    protected VirtualFileEvent(String workspaceId, String path, ChangeType type) {
        this.workspaceId = workspaceId;
        this.path = path;
        this.type = type;
    }

    protected VirtualFileEvent() {
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getPath() {
        return path;
    }

    public ChangeType getType() {
        return type;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(ChangeType type) {
        this.type = type;
    }
}
