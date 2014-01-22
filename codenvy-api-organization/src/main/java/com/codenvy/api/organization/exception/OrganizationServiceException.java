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
     * @param message
     *         the detail message
     */
    public OrganizationServiceException(String message) {
        super(message);
    }

    /**
     * @param message
     *         the detail message
     * @param cause
     *         the cause
     */
    public OrganizationServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     *         the cause
     */
    public OrganizationServiceException(Throwable cause) {
        super(cause);
    }

}
