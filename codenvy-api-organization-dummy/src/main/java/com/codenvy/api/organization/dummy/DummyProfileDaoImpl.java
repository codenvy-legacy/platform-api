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

import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.dto.server.DtoFactory;

import java.util.Arrays;

public class DummyProfileDaoImpl implements UserProfileDao {
    @Override
    public void create(Profile profile) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Profile profile) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void remove(String id) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Profile getById(String id) throws UserProfileException {
        return DtoFactory.getInstance().createDto(Profile.class).withId(id).withUserId("user1").withAttributes(
                Arrays.asList(
                        DtoFactory.getInstance().createDto(Attribute.class).withName("First Name").withValue("Felix")
                                  .withDescription("User's first name"),
                        DtoFactory.getInstance().createDto(Attribute.class).withName("Last Name").withValue("Baumgartner")
                                  .withDescription("User's last name"),
                        DtoFactory.getInstance().createDto(Attribute.class).withName("Phone").withValue("+380441112233")
                                  .withDescription("User's phone")));
    }
}
