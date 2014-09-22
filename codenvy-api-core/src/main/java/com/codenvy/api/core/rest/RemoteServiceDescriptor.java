/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.core.rest;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceDescriptor;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides basic functionality to access remote {@link Service Service}. Basically provides next information about {@code Service}:
 * <ul>
 * <li>URL of {@code Service}</li>
 * <li>Version of API</li>
 * <li>Optional description of {@code Service}</li>
 * <li>Set of {@link com.codenvy.api.core.rest.shared.dto.Link Link} to access {@code Service} functionality</li>
 * </ul>
 *
 * @author andrew00x
 * @see Service
 * @see #getLinks()
 */
public class RemoteServiceDescriptor {

    protected final String baseUrl;
    private final   URL    baseUrlURL;

    // will be initialized when it is needed
    private volatile ServiceDescriptor serviceDescriptor;

    /**
     * Creates new descriptor of remote RESTful service.
     *
     * @throws java.lang.IllegalArgumentException
     *         if URL is invalid
     */
    public RemoteServiceDescriptor(String baseUrl) throws IllegalArgumentException {
        this.baseUrl = baseUrl;
        try {
            baseUrlURL = new URL(baseUrl);
            final String protocol = baseUrlURL.getProtocol();
            if (!(protocol.equals("http") || protocol.equals("https"))) {
                throw new IllegalArgumentException(String.format("Invalid URL: %s", baseUrl));
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Invalid URL: %s", baseUrl));
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** @see ServiceDescriptor#getLinks() */
    public List<Link> getLinks() throws ServerException, IOException {
        final List<Link> links = getServiceDescriptor().getLinks();
        // always copy list and links itself!
        final List<Link> copy = new ArrayList<>(links.size());
        for (Link link : links) {
            copy.add(DtoFactory.getInstance().clone(link));
        }
        return copy;
    }

    public Link getLink(String rel) throws ServerException, IOException {
        final Link link = getServiceDescriptor().getLink(rel);
        return link == null ? null : DtoFactory.getInstance().clone(link);
    }

    public ServiceDescriptor getServiceDescriptor() throws IOException, ServerException {
        ServiceDescriptor myServiceDescriptor = serviceDescriptor;
        if (myServiceDescriptor == null) {
            synchronized (this) {
                myServiceDescriptor = serviceDescriptor;
                if (myServiceDescriptor == null) {
                    try {
                        myServiceDescriptor = serviceDescriptor = HttpJsonHelper.options(ServiceDescriptor.class, baseUrl);
                    } catch (NotFoundException | ConflictException | UnauthorizedException | ForbiddenException e) {
                        throw new ServerException(e.getServiceError());
                    }
                }
            }
        }
        return myServiceDescriptor;
    }

    /** Checks service availability. */
    public boolean isAvailable() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)baseUrlURL.openConnection();
            conn.setConnectTimeout(3 * 1000);
            conn.setReadTimeout(3 * 1000);
            conn.setRequestMethod("OPTIONS");
            return 200 == conn.getResponseCode();
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
