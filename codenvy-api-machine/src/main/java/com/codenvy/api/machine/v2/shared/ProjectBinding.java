/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.v2.shared;

/**
 * @author gazarenkov
 */
public class ProjectBinding {
    private final String workspaceId;
    private final String path;

    public ProjectBinding(String workspaceId, String path) {
        this.workspaceId = workspaceId;
        this.path = path;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "ProjectBinding{" +
               "workspaceId='" + workspaceId + '\'' +
               ", path='" + path + '\'' +
               '}';
    }
}
