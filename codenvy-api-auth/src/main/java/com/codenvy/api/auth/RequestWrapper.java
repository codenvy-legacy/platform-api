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
package com.codenvy.api.auth;

import com.codenvy.commons.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.security.Principal;

/**
 * Wraps HttpServletRequest and provide correct answers for
 * getRemoteUser, isUserInRole and getUserPrincipal getSession.
 */
public class RequestWrapper extends HttpServletRequestWrapper {
    private final HttpSession session;
    private final User        user;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException
     *         if the request is null
     */
    public RequestWrapper(HttpServletRequest request, HttpSession session, User user) {
        super(request);
        this.session = session;
        this.user = user;
    }

    @Override
    public String getRemoteUser() {
        return user.getName();
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.isMemberOf(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return user.getName();
            }
        };
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

}