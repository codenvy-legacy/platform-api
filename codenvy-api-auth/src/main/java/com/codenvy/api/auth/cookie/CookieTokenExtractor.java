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

import com.codenvy.api.auth.QueryParameterTokenExtractor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Extract token from cookies.
 *
 * @author Sergii Kabashniuk
 */
public class CookieTokenExtractor extends QueryParameterTokenExtractor {

    private static final String SECRET_TOKEN_ACCESS_COOKIE = "session-access-key";

    @Override
    public String getToken(HttpServletRequest req) {
        String token = super.getToken(req);
        if (token == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (SECRET_TOKEN_ACCESS_COOKIE.equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return token;
    }
}
