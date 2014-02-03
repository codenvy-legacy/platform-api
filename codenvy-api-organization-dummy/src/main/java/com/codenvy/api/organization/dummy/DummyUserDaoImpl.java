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
package com.codenvy.api.organization.dummy;

import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.dto.server.DtoFactory;

public class DummyUserDaoImpl implements UserDao {

    @Override
    public void create(User user) throws UserException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(User user) throws UserException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void remove(String id) throws UserException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public User getByAlias(String alias) throws UserException {
        return DtoFactory.getInstance().createDto(User.class).withId("user1").withEmail(alias)
                            .withPassword("pass1").withProfileId("profile1");

    }

    @Override
    public User getById(String id) throws UserException {
        return DtoFactory.getInstance().createDto(User.class).withId(id).withEmail(id + "@codenvy.com")
                         .withPassword("pass1").withProfileId("profile1");

    }
}
