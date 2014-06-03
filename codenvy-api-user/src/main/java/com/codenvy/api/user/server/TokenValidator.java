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
package com.codenvy.api.user.server;

import com.codenvy.api.core.ConflictException;

/**
 * Validates token.
 *
 * @author Eugene Voevodin
 * @see com.codenvy.api.user.server.UserService
 */
public interface TokenValidator {

    /**
     * Validates {@param token}
     *
     * @return user email
     * @throws ConflictException
     *         when token is not valid
     */
    String validateToken(String token) throws ConflictException;
}
