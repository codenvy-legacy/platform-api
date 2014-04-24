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
        for (Link link : getServiceDescriptor().getLinks()) {
            if (rel.equals(link.getRel())) {
                return DtoFactory.getInstance().clone(link);
            }
        }
        return null;
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
