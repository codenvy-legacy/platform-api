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
package com.codenvy.api.user.server.dao;


import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.user.shared.dto.Profile;

/**
 * DAO interface offers means to perform CRUD operations with {@link Profile} data.
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
public interface UserProfileDao {

    /**
     * Adds profile to persistent layer.
     *
     * @param profile
     *         - POJO representation of profile entity
     */
    void create(Profile profile) throws ConflictException, ServerException;

    /**
     * Updates already present in persistent layer profile.
     *
     * @param profile
     *         - POJO representation of profile entity
     */
    void update(Profile profile) throws NotFoundException, ServerException;

    /**
     * Removes profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     */
    void remove(String id) throws NotFoundException, ServerException;

    /**
     * Gets profile from persistent layer.
     *
     * @param id
     *         profile identifier
     * @return profile POJO
     * @throws com.codenvy.api.core.NotFoundException
     *         when profile doesn't exist
     */
    Profile getById(String id) throws NotFoundException, ServerException;

    /**
     * @param id
     *         profile identifier
     * @param filter
     *         reg-exp for filtering preferences keys
     * @return profile POJO
     * @throws com.codenvy.api.core.NotFoundException
     *         when profile doesn't exist
     */
    Profile getById(String id, String filter) throws NotFoundException, ServerException;
}