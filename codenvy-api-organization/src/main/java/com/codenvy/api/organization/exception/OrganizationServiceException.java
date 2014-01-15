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

package com.codenvy.api.organization.exception;

/**
 * Base exception in the hierarchy of organization service exceptions. It is thrown if no other specific exception type
 * matches the issue.
 */
public class OrganizationServiceException extends Exception {
    /**
     * Response status if any exception occurs,
     * <br>
     * Default value: 400
     */
    int responseStatus;

    public OrganizationServiceException() {
        this(400);
    }

    public OrganizationServiceException(String message, Throwable cause) {
        this(400, message, cause);
    }

    public OrganizationServiceException(String message) {
        this(400, message);
    }

    public OrganizationServiceException(Throwable cause) {
        this(400, cause);
    }

    public OrganizationServiceException(int responseStatus) {
        super();
        this.responseStatus = responseStatus;
    }

    public OrganizationServiceException(int responseStatus, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public OrganizationServiceException(int responseStatus, String message) {
        super(message);
        this.responseStatus = responseStatus;
    }

    public OrganizationServiceException(int responseStatus, Throwable cause) {
        super(cause);
        this.responseStatus = responseStatus;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

}
