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

/**
 * Describes body of the request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DtoType(DtoTypes.REQUEST_BODY_DESCRIPTOR_TYPE)
public class RequestBodyDescriptor {
    /** Description of request body. */
    private String description;

    public RequestBodyDescriptor(String description) {
        this.description = description;
    }

    RequestBodyDescriptor(RequestBodyDescriptor other) {
        // Full copy of other instance. Used from Link.
        this.description = other.description;
    }

    public RequestBodyDescriptor() {
    }

    /**
     * Get optional description of request body.
     *
     * @return optional description of request body
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set optional description of request body.
     *
     * @param description
     *         optional description of request body
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "RequestBodyDescriptor{" +
               "description='" + description + '\'' +
               '}';
    }
}
