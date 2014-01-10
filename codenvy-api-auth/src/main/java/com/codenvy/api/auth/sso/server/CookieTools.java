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
package com.codenvy.api.auth.sso.server;

import com.codenvy.api.auth.AuthenticationService;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * It's a temporary solution used to avoid copying of code.
 *
 * @author Alexander Garagatyi
 */
public class CookieTools {
    private static final String ACCESS_COOKIE_PATH = "/api/internal/sso/server";

    public static void clearCookies(Response.ResponseBuilder builder, String token, boolean secure) {
        builder.header("Set-Cookie", new NewCookie("token-access-key", token, ACCESS_COOKIE_PATH, null, null, 0, secure) + ";HttpOnly");
        builder.header("Set-Cookie", new NewCookie("session-access-key", token, "/", null, null, 0, secure) + ";HttpOnly");
        builder.cookie(new NewCookie("logged_in", "true", "/", null, null, 0, secure));
    }

    public static void setCookies(Response.ResponseBuilder builder, String token, boolean secure, boolean isAnonymous) {
        builder.header("Set-Cookie",
                       new NewCookie("token-access-key", token, ACCESS_COOKIE_PATH, null, null, (int)AuthenticationService.TICKET_LIFE_TIME_SECONDS, secure) +
                       ";HttpOnly");
        builder.header("Set-Cookie", new NewCookie("session-access-key", token, "/", null, null, -1, secure) + ";HttpOnly");
        if (!isAnonymous) {
            builder.cookie(new NewCookie("logged_in", "true", "/", null, null, (int)AuthenticationService.TICKET_LIFE_TIME_SECONDS, secure));
        }
    }
}
