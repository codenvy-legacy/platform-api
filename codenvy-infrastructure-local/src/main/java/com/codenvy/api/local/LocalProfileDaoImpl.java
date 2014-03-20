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

import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Profile;

import javax.inject.Inject;

public class LocalProfileDaoImpl implements UserProfileDao {

    private ProfileStorage profileStorage;

    @Inject
    public LocalProfileDaoImpl(ProfileStorage profileStorage){
        this.profileStorage = profileStorage;
    }
    @Override
    public void create(Profile profile) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Profile profile) throws UserProfileException {
        profileStorage.update(profile);
    }

    @Override
    public void remove(String id) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Profile getById(String id) throws UserProfileException {
        return profileStorage.get(id);
    }

    @Override
    public Profile getById(String id, String filter) throws UserProfileException {
        throw new RuntimeException("Not implemented");
    }
}
