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
package com.codenvy.api.machine.shared.dto;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
public interface SnapshotDescriptor {
    String getId();

    void setId(String id);

    SnapshotDescriptor withId(String id);

    String getOwner();

    void setOwner(String owner);

    SnapshotDescriptor withOwner(String owner);

    String getImageType();

    void setImageType(String imageType);

    SnapshotDescriptor withImageType(String imageType);

    String getDescription();

    void setDescription(String description);

    SnapshotDescriptor withDescription(String description);

    long getCreationDate();

    void setCreationDate(long creationDate);

    SnapshotDescriptor withCreationDate(long creationDate);

    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    SnapshotDescriptor withProjects(List<ProjectBindingDescriptor> projects);
}
