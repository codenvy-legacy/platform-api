/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes capabilities of the Service.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.Service
 */
@DTO
public interface ServiceDescriptor {

    /**
     * Get location to get this service descriptor in JSON format.
     *
     * @return location to get this service descriptor in JSON format
     */
    String getHref();

    ServiceDescriptor withHref(String href);

    /**
     * Set location to get this service descriptor in JSON format.
     *
     * @param href
     *         location to get this service descriptor in JSON format
     */
    void setHref(String href);

    /**
     * Get description of the Service.
     *
     * @return description of the Service
     */
    String getDescription();

    ServiceDescriptor withDescription(String description);

    /**
     * Set description of the Service.
     *
     * @param description
     *         description of the Service
     */
    void setDescription(String description);

    /**
     * Get API version.
     *
     * @return API version
     */
    String getVersion();

    ServiceDescriptor withVersion(String version);

    /**
     * Get API version.
     *
     * @param version
     *         API version
     */
    void setVersion(String version);

    /**
     * Get links to the resources provided by the Service. Client selects link according to own needs and uses it to navigate to the
     * required resource.
     *
     * @return set of links to the resources provided by the Service
     */
    List<Link> getLinks();

    ServiceDescriptor withLinks(List<Link> links);

    /**
     * Set links to the resources provided by the Service,
     *
     * @param links
     *         links to the resources provided by the Service
     */
    void setLinks(List<Link> links);
}
