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

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.shared.dto.StorageMachine;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
public interface MachineDao {
    StorageMachine getById(String machineId) throws NotFoundException, ServerException;

    void create(StorageMachine machine) throws ServerException;

    void remove(String machineId) throws NotFoundException, ServerException;

    List<StorageMachine> findByUserWorkspaceProject(String userId, String wsId, String projectName) throws ServerException;
}
