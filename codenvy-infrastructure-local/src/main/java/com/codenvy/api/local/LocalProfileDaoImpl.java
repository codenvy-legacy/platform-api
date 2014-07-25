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

import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.api.user.server.dao.UserProfileDao;

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
    public void create(Profile profile) {
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
    public void remove(String id) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Profile getById(String id) {
        try {
            return profileStorage.get(id);
        } catch (IOException e) {
            LOG.error("It is not possible to get profile", e);
        }
        return null;
    }

    @Override
    public Profile getById(String id, String filter) {
        throw new RuntimeException("Not implemented");
    }
}
