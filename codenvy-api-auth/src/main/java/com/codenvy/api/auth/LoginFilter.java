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


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpReq = (HttpServletRequest)request;
        final HttpServletResponse httpResp = (HttpServletResponse)response;
        String token = tokenExtractor.getToken(httpReq);
        if (token != null) {
            HttpSession session = null;
            synchronized (sessionStore) {
                session = sessionStore.getSession(token);
                if (session == null) {
                    session = httpReq.getSession();
                    sessionStore.saveSession(token, session);
                }
            }

            handleValidToken(request, response, chain, session, null);
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
