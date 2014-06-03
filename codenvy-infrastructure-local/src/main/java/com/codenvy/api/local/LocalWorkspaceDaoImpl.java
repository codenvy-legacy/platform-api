/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.local;

import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.shared.dto.Workspace;


import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
public class LocalWorkspaceDaoImpl implements WorkspaceDao {
    @Override
    public void create(Workspace workspace) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Workspace workspace) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void remove(String id) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Workspace getById(String id) {
        return Constants.WORKSPACE;
    }

    @Override
    public Workspace getByName(String name) {
        return Constants.WORKSPACE;

    }

    @Override
    public List<Workspace> getByAccount(String accountId) {
        return Arrays.asList(Constants.WORKSPACE);
    }
}