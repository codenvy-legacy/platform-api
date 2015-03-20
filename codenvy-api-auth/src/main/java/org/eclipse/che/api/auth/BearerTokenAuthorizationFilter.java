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

import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * @author Sergii Kabashniuk
 */
@Singleton
public class BearerTokenAuthorizationFilter implements Filter {

    private final AuthorizationManager authorizationManager;

    @Inject
    public BearerTokenAuthorizationFilter(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String bearertoken = request.getParameter("bearertoken");
        EnvironmentContext context = EnvironmentContext.getCurrent();
        if (bearertoken != null && !bearertoken.isEmpty() && context.getUser() != null) {
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            final String temporaryRole = authorizationManager
                    .getUserRoles(EnvironmentContext.getCurrent().getUser().getId(),
                                  bearertoken,
                                  httpServletRequest.getMethod(),
                                  httpServletRequest.getRequestURI());
            if (temporaryRole != null) {
                chain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
                    @Override
                    public boolean isUserInRole(String role) {
                        if (temporaryRole.equals(role)) {
                            return true;
                        }
                        return super.isUserInRole(role);
                    }
                }, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
