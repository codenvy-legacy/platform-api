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
