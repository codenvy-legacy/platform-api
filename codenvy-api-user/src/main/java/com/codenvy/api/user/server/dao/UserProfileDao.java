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


import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Profile;

import java.io.IOException;

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
     * @throws UserProfileException
     *         if any issue occurred during performing an operation
     */
    void create(Profile profile) throws UserProfileException;

    /**
     * Updates already present in persistent layer profile.
     *
     * @param profile
     *         - POJO representation of profile entity
     * @throws UserProfileException
     *         if any issue occurred during performing an operation
     */
    void update(Profile profile) throws UserProfileException;

    /**
     * Removes profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     * @throws UserProfileException
     *         if any issue occurred during performing an operation
     */
    void remove(String id) throws UserProfileException;

    /**
     * Gets profile from persistent layer.
     *
     * @param id
     *         profile identifier
     * @return profile POJO, or <code>null</code> if nothing is found
     * @throws UserProfileException
     *         if any issue occurred during performing an operation
     */
    Profile getById(String id) throws UserProfileException;

    /**
     * @param id
     *         profile identifier
     * @param filter
     *         reg-exp for filtering preferences keys
     * @return profile POJO, or <code>null</code> if profile not found
     * @throws UserProfileException
     *         if any issue occurred during performing an operation
     */
    Profile getById(String id, String filter) throws UserProfileException;
}