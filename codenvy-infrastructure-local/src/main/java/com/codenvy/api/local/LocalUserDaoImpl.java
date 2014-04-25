/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
