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


import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.shared.dto.Profile;

/**
 * DAO interface offers means to perform CRUD operations with {@link Profile} data.
 *
 */
public interface UserProfileDao {

    /**
     * Adds profile to persistent layer.
     *
     * @param profile
     *         - POJO representation of profile entity
     * @throws com.codenvy.api.user.server.exception.UserException
     *         if any issue occurred during performing an operation
     */
    void create(Profile profile) throws UserException;

    /**
     * Updates already present in persistent layer profile.
     *
     * @param profile
     *         - POJO representation of profile entity
     * @throws UserException
     *         if any issue occurred during performing an operation
     */
    void update(Profile profile) throws UserException;

    /**
     * Removes profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     * @throws UserException
     *         if any issue occurred during performing an operation
     */
    void remove(String id) throws UserException;

    /**
     * Gets profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     * @return profile POJO, or <code>null</code> if nothing is found
     * @throws UserException
     *         if any issue occurred during performing an operation
     */
    Profile getById(String id) throws UserException;

}
