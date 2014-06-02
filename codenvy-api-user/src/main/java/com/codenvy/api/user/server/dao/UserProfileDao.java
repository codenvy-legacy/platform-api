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