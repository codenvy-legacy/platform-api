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

import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.user.User;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @author Sergii Kabashniuk
 */
public class LoginFilter implements Filter {

    @Inject
    protected TokenExtractor tokenExtractor;

    @Inject
    protected SessionStore sessionStore;

    @Inject
    protected UserProvider userProvider;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest)request;
        String token = tokenExtractor.getToken(httpRequest);
        if (token != null) {
            HttpSession session = null;
            synchronized (sessionStore) {
                session = sessionStore.getSession(token);
                if (session == null) {
                    session = httpRequest.getSession();
                    sessionStore.saveSession(token, session);
                }
            }

            handleValidToken(request, response, chain, session, userProvider.getUser(token));
            return;
        } else {
            //token not exists
            handleMissingToken(request, response, chain);
        }

    }

    protected void handleValidToken(ServletRequest request, ServletResponse response, FilterChain chain, HttpSession session, User user)
            throws IOException, ServletException {
        EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        environmentContext.setUser(user);
        chain.doFilter(new RequestWrapper((HttpServletRequest)request, session, user), response);
    }


    protected void handleInvalidToken(ServletRequest request, ServletResponse response, FilterChain chain, String token)
            throws IOException, ServletException {
    }


    protected void handleMissingToken(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

    }


    @Override
    public void destroy() {

    }
}
