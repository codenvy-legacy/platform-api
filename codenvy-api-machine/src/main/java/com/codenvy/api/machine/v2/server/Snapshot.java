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
    private final String               owner;
    private final long                 creationDate;
    private final List<ProjectBinding> projects;
    private final String description;

    public Snapshot(String id, ImageKey imageKey, String owner, long creationDate, List<ProjectBinding> projects, String description) {
        this.id = id;
        this.imageKey = imageKey;
        this.owner = owner;
        this.creationDate = creationDate;
        this.projects = java.util.Collections.unmodifiableList(projects);
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public ImageKey getImageKey() {
        return imageKey;
    }

    public String getOwner() {
        return owner;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public List<ProjectBinding> getProjects() {
        return projects;
    }

    public String getDescription() {
        return description;
    }
}
