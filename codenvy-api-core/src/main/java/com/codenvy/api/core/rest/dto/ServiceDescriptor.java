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
package com.codenvy.api.core.rest.dto;

import com.codenvy.api.core.rest.Constants;
import com.codenvy.api.core.rest.dto.DtoType;
import com.codenvy.api.core.rest.dto.DtoTypes;
import com.codenvy.api.core.rest.dto.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes capabilities of the Service.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.Service
 */
@DtoType(DtoTypes.SERVICE_DESCRIPTOR_TYPE)
public class ServiceDescriptor {
    /** Location to get this service descriptor in JSON format. */
    private String     href;
    /** Description which can help client to understand purpose of the Service. */
    private String     description;
    /**
     * Links to resources provided by the Service. Client selects link according to own needs and uses it to navigate to the required
     * resource.
     */
    private List<Link> links;
    /** API version. */
    private String version = Constants.API_VERSION;

    public ServiceDescriptor(String href, String description, List<Link> links) {
        this.href = href;
        this.description = description;
        if (links != null) {
            this.links = new ArrayList<>(links);
        }
    }

    public ServiceDescriptor() {
    }

    /**
     * Get location to get this service descriptor in JSON format.
     *
     * @return location to get this service descriptor in JSON format
     */
    public String getHref() {
        return href;
    }

    /**
     * Set location to get this service descriptor in JSON format.
     *
     * @param href
     *         location to get this service descriptor in JSON format
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Get description of the Service.
     *
     * @return description of the Service
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description of the Service.
     *
     * @param description
     *         description of the Service
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get API version.
     *
     * @return API version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get API version.
     *
     * @param version
     *         API version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get links to the resources provided by the Service,
     *
     * @return set of links to the resources provided by the Service
     */
    public List<Link> getLinks() {
        if (links == null) {
            links = new ArrayList<>();
        }
        return links;
    }

    /**
     * Set links to the resources provided by the Service,
     *
     * @param links
     *         links to the resources provided by the Service
     */
    public void setLinks(List<Link> links) {
        if (links == null) {
            this.links = null;
        } else {
            this.links = new ArrayList<>(links);
        }
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
               "href='" + href + '\'' +
               ", description='" + description + '\'' +
               ", version='" + version + '\'' +
               '}';
    }
}
