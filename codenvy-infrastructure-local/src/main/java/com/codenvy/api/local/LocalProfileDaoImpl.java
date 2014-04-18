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
import com.codenvy.api.user.shared.dto.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class LocalProfileDaoImpl implements UserProfileDao {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileStorage.class);
    private ProfileStorage profileStorage;

    @Inject
    public LocalProfileDaoImpl(ProfileStorage profileStorage) {
        this.profileStorage = profileStorage;
    }

    @Override
    public void create(Profile profile)  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void update(Profile profile) {
        try {
            profileStorage.update(profile);
        } catch (IOException e) {
            LOG.error("It is not possible to update profile", e);
        }
    }

    @Override
    public void remove(String id)  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Profile getById(String id)  {
        try {
            return profileStorage.get(id);
        } catch (IOException e) {
            LOG.error("It is not possible to get profile", e);
        }
        return null;
    }

    @Override
    public Profile getById(String id, String filter)  {
        throw new RuntimeException("Not implemented");
    }
}
