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
