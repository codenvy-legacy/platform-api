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

import java.util.ArrayList;
import java.util.List;

/**
 * Describes resource.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DtoType(DtoTypes.LINK_TYPE)
public class Link {
    /** URL of resource link. */
    private String                    href;
    /** Short description of resource link. */
    private String                    rel;
    /** HTTP method to use with resource, e.g. GET, POST. */
    private String                    method;
    /** Media type produced by resource. */
    private String                    produces;
    /** Media type consumed by resource. Makes no sense for GET method. */
    private String                    consumes;
    /** Request body description. Makes no sense for GET method. */
    private RequestBodyDescriptor     requestBody;
    /** Description of the query parameters (if any) of request. */
    private List<ParameterDescriptor> parameters;

    public Link(String href, String rel, String method, String produces, String consumes) {
        this.href = href;
        this.rel = rel;
        this.method = method;
        this.produces = produces;
        this.consumes = consumes;
    }

    public Link(String href, String rel, String method, String produces) {
        this.href = href;
        this.rel = rel;
        this.method = method;
        this.produces = produces;
    }

    public Link(String href, String rel, String method) {
        this.href = href;
        this.rel = rel;
        this.method = method;
    }

    public Link(Link other) {
        // full copy of other instance
        this.href = other.href;
        this.rel = other.rel;
        this.method = other.method;
        this.produces = other.produces;
        this.consumes = other.consumes;
        if (other.requestBody != null) {
            this.requestBody = new RequestBodyDescriptor(other.getRequestBody());
        }
        List<ParameterDescriptor> otherParameters = other.parameters;
        if (otherParameters != null) {
            this.parameters = new ArrayList<>(otherParameters.size());
            for (ParameterDescriptor otherParameter : otherParameters) {
                this.parameters.add(new ParameterDescriptor(otherParameter));
            }
        }
    }

    public Link() {
    }

    /**
     * Get URL of resource link.
     *
     * @return URL of resource link.
     */
    public String getHref() {
        return href;
    }

    /**
     * Set URL of resource link.
     *
     * @param href
     *         URL of resource link.
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Get short description of resource link.
     *
     * @return short description of resource link
     */
    public String getRel() {
        return rel;
    }

    /**
     * Set short description of resource link.
     *
     * @param rel
     *         short description of resource link
     */
    public void setRel(String rel) {
        this.rel = rel;
    }

    /**
     * Get HTTP method to use with resource.
     *
     * @return HTTP method to use with resource
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set HTTP method to use with resource.
     *
     * @param method
     *         HTTP method to use with resource
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Get media type produced by resource.
     *
     * @return media type produced by resource
     */
    public String getProduces() {
        return produces;
    }

    /**
     * Set media type produced by resource.
     *
     * @param produces
     *         media type produced by resource
     */
    public void setProduces(String produces) {
        this.produces = produces;
    }

    /**
     * Get media type consumed by resource.
     *
     * @return media type consumed by resource
     */
    public String getConsumes() {
        return consumes;
    }

    /**
     * Set media type consumed by resource.
     *
     * @param consumes
     *         media type consumed by resource
     */
    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    /**
     * Get description of the query parameters (if any) of request.
     *
     * @return description of the query parameters (if any) of request
     */
    public List<ParameterDescriptor> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        return parameters;
    }

    /**
     * Set description of the query parameters (if any) of request.
     *
     * @param parameters
     *         description of the query parameters (if any) of request
     */
    public void setParameters(List<ParameterDescriptor> parameters) {
        if (parameters == null) {
            this.parameters = null;
        } else {
            this.parameters = new ArrayList<>(parameters);
        }
    }

    /**
     * Get request body description.
     *
     * @return request body description
     */
    public RequestBodyDescriptor getRequestBody() {
        return requestBody;
    }

    /**
     * Set request body description.
     *
     * @param requestBody
     *         request body description
     */
    public void setRequestBody(RequestBodyDescriptor requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public String toString() {
        return "Link{" +
               "href='" + href + '\'' +
               ", consumes='" + consumes + '\'' +
               ", produces='" + produces + '\'' +
               ", rel='" + rel + '\'' +
               ", method='" + method + '\'' +
               ", requestBody=" + requestBody +
               ", parameters=" + parameters +
               '}';
    }
}
