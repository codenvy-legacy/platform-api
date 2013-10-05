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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceDescriptor;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
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
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see Service
 * @see #getHref()
 * @see #getDescription()
 * @see #getLinks()
 */
public abstract class RemoteServiceDescriptor {

    protected final String baseUrl;

    // will be initialized when it is needed
    private volatile ServiceDescriptor serviceDescriptor;

    protected RemoteServiceDescriptor(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** @see ServiceDescriptor#getHref() */
    public String getHref() throws RemoteException, IOException {
        return getServiceDescriptor().getHref();
    }

    /** @see ServiceDescriptor#getDescription() */
    public String getDescription() throws RemoteException, IOException {
        return getServiceDescriptor().getDescription();
    }

    /** @see ServiceDescriptor#getVersion() */
    public String getVersion() throws RemoteException, IOException {
        return getServiceDescriptor().getVersion();
    }

    /** @see ServiceDescriptor#getLinks() */
    public List<Link> getLinks() throws RemoteException, IOException {
        final List<Link> links = getServiceDescriptor().getLinks();
        // always copy list and links itself!
        final List<Link> copy = new ArrayList<>(links.size());
        for (Link link : links) {
            copy.add(DtoFactory.getInstance().clone(link));
        }
        return copy;
    }

    public Link getLink(String rel) throws IOException, RemoteException {
        for (Link link : getServiceDescriptor().getLinks()) {
            if (rel.equals(link.getRel())) {
                return DtoFactory.getInstance().clone(link);
            }
        }
        return null;
    }

    private ServiceDescriptor getServiceDescriptor() throws IOException, RemoteException {
        ServiceDescriptor myServiceDescriptor = serviceDescriptor;
        if (myServiceDescriptor == null) {
            synchronized (this) {
                myServiceDescriptor = serviceDescriptor;
                if (myServiceDescriptor == null) {
                    myServiceDescriptor = serviceDescriptor = HttpJsonHelper.get(ServiceDescriptor.class, baseUrl);
                }
            }
        }
        return myServiceDescriptor;
    }
}
