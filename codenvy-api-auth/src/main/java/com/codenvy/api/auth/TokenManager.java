/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.auth;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager to handle access token for login process
 * <p/>
 *
 * @author Andrey Parfonov
 * @author Sergey Kabashniuk
 */
@Singleton
public class TokenManager {
    private final Map<String, String> tokens;
    private final TokenGenerator      tokenGenerator;

    @Inject
    public TokenManager(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        tokens = new HashMap<>();
    }

    /**
     * Create new access token and associate with given userid.
     *
     * @param userId
     *         user id
     * @return access token.
     */
    public String createToken(String userId) {
        String token = tokenGenerator.generate();
        tokens.put(token, userId);
        return token;
    }

    /**
     * @param token
     *         access token.
     * @return userId associated with token
     */
    public String getUserId(String token) {
        return tokens.get(token);
    }

    /**
     * Remove access token from manager.
     *
     * @param token
     *         unique token to remove
     * @return userId associated with token
     */
    public String removeToken(String token) {
        return tokens.remove(token);
    }
}
