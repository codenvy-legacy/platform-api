/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
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
package org.eclipse.che.api.auth;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Extract token first from query parameter then from session-access-key cookie.
 *
 * @author Sergii Kabashniuk
 */
public class CookiesTokenExtractor extends QueryParameterTokenExtractor {
    @Override
    public String getToken(HttpServletRequest req) {
        String token = super.getToken(req);
        if (token == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if ("session-access-key".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return token;
    }
}
