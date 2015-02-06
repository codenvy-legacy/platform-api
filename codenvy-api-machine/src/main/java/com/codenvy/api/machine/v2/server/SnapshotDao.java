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

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.machine.v2.shared.ProjectBinding;

import java.util.List;

/**
 * @author andrew00x
 */
public interface SnapshotDao {
    Snapshot getSnapshot(String snapshotId) throws NotFoundException;

    void saveSnapshot(Snapshot snapshot);

    List<Snapshot> findSnapshots(String owner, ProjectBinding project);

    void removeSnapshot(String snapshotId) throws NotFoundException;
}
