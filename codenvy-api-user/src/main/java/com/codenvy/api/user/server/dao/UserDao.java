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
     *         POJO representation of user entity
     */
    void update(User user) throws NotFoundException, ServerException;

    /**
     * Removes user from persistent layer by his identifier.
     *
     * @param id
     *         user identifier
     */
    void remove(String id) throws NotFoundException, ServerException, ConflictException;

    /**
     * Gets user from persistent layer by any of his aliases
     *
     * @param alias
     *         user name or alias
     * @return user POJO
     * @throws com.codenvy.api.core.NotFoundException
     *         when user doesn't exist
     */
    User getByAlias(String alias) throws NotFoundException, ServerException;

    /**
     * Gets user from persistent layer by his identifier
     *
     * @param id
     *         user name or identifier
     * @return user POJO
     * @throws com.codenvy.api.core.NotFoundException
     *         when user doesn't exist
     */
    User getById(String id) throws NotFoundException, ServerException;
}
