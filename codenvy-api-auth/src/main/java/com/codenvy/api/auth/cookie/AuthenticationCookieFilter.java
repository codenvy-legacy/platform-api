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

import com.codenvy.api.auth.TokenExtractor;
import com.codenvy.api.auth.TokenManager;
import com.codenvy.api.auth.shared.dto.Token;
import com.codenvy.dto.server.DtoFactory;
import com.google.common.io.ByteStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Sergii Kabashniuk
 */
@Singleton
public class AuthenticationCookieFilter implements Filter {

    private final TokenManager                 tokenManager;
    private final TokenExtractor               tokenExtractor;
    private final AuthenticationCookiesBuilder cookiesBuilder;

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationCookieFilter.class);

    @Inject
    public AuthenticationCookieFilter(TokenManager tokenManager, TokenExtractor tokenExtractor,
                                      AuthenticationCookiesBuilder cookiesBuilder) {
        this.tokenManager = tokenManager;
        this.tokenExtractor = tokenExtractor;
        this.cookiesBuilder = cookiesBuilder;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        String token = tokenExtractor.getToken((HttpServletRequest)request);
        final ByteArrayPrintWriter pw = new ByteArrayPrintWriter();
        final HttpServletResponse httpServletResponse = (HttpServletResponse)response;
        HttpServletResponse wrappedResp = new HttpServletResponseWrapper(httpServletResponse) {
            public PrintWriter getWriter() {
                return pw.getWriter();
            }

            public ServletOutputStream getOutputStream() {
                return pw.getStream();
            }

        };

        try {
            chain.doFilter(request, wrappedResp);
        } finally {
            try {
                if (httpServletResponse.getStatus() == Response.Status.OK.getStatusCode()) {
                    String responseToken =
                            DtoFactory.getInstance().createDtoFromJson(new ByteArrayInputStream(pw.toByteArray()), Token.class).getValue();

                    for (Cookie cookie : cookiesBuilder.buildCookies(responseToken)) {
                        httpServletResponse.addCookie(cookie);
                    }
                    if (token != null) {
                        String requestUserId = tokenManager.getUserId(token);

                        if (requestUserId.equals(tokenManager.getUserId(responseToken))) {
                            tokenManager.removeToken(requestUserId);
                            for (Cookie cookie : cookiesBuilder.removeCookies(responseToken)) {
                                httpServletResponse.addCookie(cookie);
                            }
                        }
                    }
                }
            } finally {
                try (OutputStream os = response.getOutputStream()) {
                    ByteStreams.copy(new ByteArrayInputStream(pw.toByteArray()), os);
                    os.flush();
                }
            }
        }


    }

    @Override
    public void destroy() {

    }


}
