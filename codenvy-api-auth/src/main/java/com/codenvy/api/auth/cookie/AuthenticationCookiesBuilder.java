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
package com.codenvy.api.auth.cookie;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Cookie;

/**
 * @author Sergii Kabashniuk
 */
public class AuthenticationCookiesBuilder {
    private final String accessTokenPath;
    private final int    tokenLifeTimeSeconds;

    @Inject
    public AuthenticationCookiesBuilder(@Named("auth.accesstoken.path") String accessTokenPath,
                                        @Named("auth.accesstoken.lifetime.seconds") int tokenLifeTimeSeconds) {
        this.accessTokenPath = accessTokenPath;
        this.tokenLifeTimeSeconds = tokenLifeTimeSeconds;
    }


    public Cookie[] buildCookies(String token) {
        Cookie persistentCookie = new Cookie("token-access-key", token);
        persistentCookie.setPath(accessTokenPath);
        persistentCookie.setHttpOnly(true);
        persistentCookie.setMaxAge(tokenLifeTimeSeconds);

        Cookie sessionCookie = new Cookie("session-access-key", token);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(-1);

        Cookie loggedInCookie = new Cookie("logged_in", "true");
        loggedInCookie.setPath("/");
        loggedInCookie.setHttpOnly(false);
        loggedInCookie.setMaxAge(tokenLifeTimeSeconds);


        return new Cookie[]{persistentCookie, sessionCookie, loggedInCookie};
    }

    public Cookie[] removeCookies(String token) {
        Cookie[] cookies = buildCookies(token);
        for (Cookie cookie : cookies) {
            cookie.setMaxAge(0);
        }
        return cookies;
    }
}
