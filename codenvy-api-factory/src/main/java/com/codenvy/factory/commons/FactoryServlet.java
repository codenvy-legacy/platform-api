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
package com.codenvy.factory.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URL;

/** Servlet to handle factory URL's. */
public abstract class FactoryServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            URL currentUrl = UriBuilder.fromUri(req.getRequestURL().toString()).replaceQuery(req.getQueryString()).build().toURL();

            FactoryUrl factoryUrl = FactoryUrlParser.parse(currentUrl);

            createTempWorkspaceAndRedirect(req, resp, factoryUrl);
        } catch (FactoryUrlException e) {
            LOG.warn(e.getLocalizedMessage(), e);
            throw new ServletException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Create temporary workspace for current factory URL and redirect user to this workspace
     *
     * @param req
     *         - an HttpServletRequest object that contains the request the client has made of the servlet
     * @param resp
     *         - an HttpServletResponse object that contains the response the servlet sends to the client
     * @param factoryUrl
     *         - factory URL for temporary workspace creation
     * @throws ServletException
     * @throws IOException
     */
    protected abstract void createTempWorkspaceAndRedirect(HttpServletRequest req, HttpServletResponse resp, FactoryUrl factoryUrl)
            throws ServletException, IOException;
}
