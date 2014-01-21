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
package com.codenvy.api.organization.dao;


import com.codenvy.api.organization.exception.OrganizationServiceException;
import com.codenvy.api.organization.shared.dto.Profile;

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
     * @throws OrganizationServiceException
     *         if any issue occurred during performing an operation
     */
    void create(Profile profile) throws OrganizationServiceException;

    /**
     * Updates already present in persistent layer profile.
     *
     * @param profile
     *         - POJO representation of profile entity
     * @throws OrganizationServiceException
     *         if any issue occurred during performing an operation
     */
    void update(Profile profile) throws OrganizationServiceException;

    /**
     * Removes profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     * @throws OrganizationServiceException
     *         if any issue occurred during performing an operation
     */
    void remove(String id) throws OrganizationServiceException;

    /**
     * Gets profile from persistent layer.
     *
     * @param id
     *         - profile identifier
     * @return profile POJO, or <code>null</code> if nothing is found
     * @throws OrganizationServiceException
     *         if any issue occurred during performing an operation
     */
    Profile getById(String id) throws OrganizationServiceException;

}
