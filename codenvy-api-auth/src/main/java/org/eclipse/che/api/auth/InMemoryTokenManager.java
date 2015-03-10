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
package org.eclipse.che.api.auth;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Hold information about tokens in memory.
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class InMemoryTokenManager implements TokenManager {
    private final Map<String, String> tokens;
    private final TokenGenerator      tokenGenerator;


    @Inject
    public InMemoryTokenManager(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        this.tokens = new HashMap<>();
    }

    @Override
    public String createToken(String userId) {

        String token = tokenGenerator.generate();
        tokens.put(token, userId);
        return token;
    }

    @Override
    public String getUserId(String token) {
        return tokens.get(token);
    }

    @Override
    public boolean isValid(String token) {
        return tokens.containsKey(token);
    }

    @Override
    public String invalidateToken(String token) {
        return tokens.remove(token);
    }
}
