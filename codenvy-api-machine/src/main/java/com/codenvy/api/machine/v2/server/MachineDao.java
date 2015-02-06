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

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.v2.shared.Machine;

import java.util.List;

/**
 * DAO interface for CRUD operations with metadata of machines
 *
 * @author Alexander Garagatyi
 */
public interface MachineDao {
    Machine getById(String machineId) throws NotFoundException, ServerException;

    void add(Machine machine) throws ServerException;

    void remove(String machineId) throws NotFoundException, ServerException;

    List<Machine> findByUserWorkspaceProject(String userId, String workspaceId, String project)
            throws ServerException, ForbiddenException;

    void update(Machine machine) throws NotFoundException, ServerException;
}
