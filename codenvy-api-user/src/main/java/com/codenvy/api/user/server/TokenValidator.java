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
package com.codenvy.api.user.server;

import com.codenvy.api.user.server.exception.UserException;

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
     * @throws UserException
     *         when token is not valid
     */
    String validateToken(String token) throws UserException;
}
