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
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.machine.v2.server.spi.ImageKey;
import com.codenvy.api.machine.v2.shared.ProjectBinding;

import java.util.List;

/**
 * Saved state of {@link com.codenvy.api.machine.v2.server.spi.Machine}.
 *
 * @author andrew00x
 */
public class Snapshot {
    private final String               id;
    private final ImageKey             imageKey;
    private final String               createdBy;
    private final long                 creationDate;
    private final String               workspaceId;
    private final List<ProjectBinding> projects;

    public Snapshot(String id, ImageKey imageKey, String createdBy, long creationDate, String workspaceId, List<ProjectBinding> projects) {
        this.id = id;
        this.imageKey = imageKey;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
        this.workspaceId = workspaceId;
        this.projects = java.util.Collections.unmodifiableList(projects);
    }

    public String getId() {
        return id;
    }

    public ImageKey getImageKey() {
        return imageKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public List<ProjectBinding> getProjects() {
        return projects;
    }
}
