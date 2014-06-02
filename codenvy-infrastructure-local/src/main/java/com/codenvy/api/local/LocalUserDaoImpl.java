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
package com.codenvy.api.local;

import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;

import javax.inject.Singleton;

@Singleton
public class LocalUserDaoImpl implements UserDao {

    @Override
    public boolean authenticate(String alias, String password)  {
        return true;
    }

    @Override
    public void create(User user)  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(User user)  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void remove(String id)  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User getByAlias(String alias)  {
        return Constants.USER;

    }

    @Override
    public User getById(String id)  {
        return Constants.USER;
    }
}
