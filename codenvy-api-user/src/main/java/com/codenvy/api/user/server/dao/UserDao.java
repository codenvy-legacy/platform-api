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
import com.codenvy.api.user.shared.dto.User;

/**
 * DAO interface offers means to perform CRUD operations with {@link com.codenvy.api.user.shared.dto.User} data. The implementation is not
 * required to be responsible for persistent layer data dto integrity. It simply transfers data from one layer to another, so if
 * you're going to call any of implemented methods it is considered that all needed verifications are already done. <p>
 * <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency, but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 */
public interface UserDao {

    /**
     * Authenticate user.
     *
     * @param alias
     *         user name or alias
     * @param password
     *         password
     * @return {@code true} if authentication is successful or {@code false} otherwise
     */
    boolean authenticate(String alias, String password) throws NotFoundException, ServerException;

    /**
     * Adds user to persistent layer.
     *
     * @param user
     *         - POJO representation of user entity
     */
    void create(User user) throws ConflictException, ServerException;

    /**
     * Updates already present in persistent layer user.
     *
     * @param user
     *         - POJO representation of user entity
     */
    void update(User user) throws NotFoundException, ServerException;

    /**
     * Removes user from persistent layer by his identifier.
     *
     * @param id
     *         - user identifier
     */
    void remove(String id) throws NotFoundException, ServerException;

    /**
     * Gets user from persistent layer by any of his aliases
     *
     * @param alias
     *         - user name or alias
     * @return user POJO, or <code>null</code> if nothing is found
     */
    User getByAlias(String alias) throws NotFoundException, ServerException;

    /**
     * Gets user from persistent layer by his identifier
     *
     * @param id
     *         - user name or identifier
     * @return user POJO, or <code>null</code> if nothing is found
     */
    User getById(String id) throws NotFoundException, ServerException;
}
