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
package com.codenvy.api.machine.server;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.shared.ProjectBinding;

import java.util.List;

/**
 * @author andrew00x
 */
public interface SnapshotStorage {
    Snapshot getSnapshot(String snapshotId) throws NotFoundException, ServerException;

    void saveSnapshot(Snapshot snapshot) throws ServerException, ForbiddenException;

    List<Snapshot> findSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException;

    void removeSnapshot(String snapshotId) throws NotFoundException, ServerException;
}
