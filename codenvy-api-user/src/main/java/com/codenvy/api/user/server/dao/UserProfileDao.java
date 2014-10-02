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
package com.codenvy.api.user.server.dao;


import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;

/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.user.server.dao.Profile} data.
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
public interface UserProfileDao {

    /**
     * Adds profile to persistent layer.
     *
     * @param profile
     *         profile to setPreferences
     */
    void create(Profile profile) throws ConflictException, ServerException;

    /**
     * Updates already present in persistent layer profile.
     *
     * @param profile
     *         profile to update
     */
    void update(Profile profile) throws NotFoundException, ServerException;

    /**
     * Removes profile from persistent layer.
     *
     * @param id
     *         profile identifier
     */
    void remove(String id) throws NotFoundException, ServerException;

    /**
     * Gets profile from persistent layer.
     *
     * @param id
     *         profile identifier
     * @return profile with given {@code id}
     * @throws com.codenvy.api.core.NotFoundException
     *         when profile doesn't exist
     */
    Profile getById(String id) throws NotFoundException, ServerException;
}