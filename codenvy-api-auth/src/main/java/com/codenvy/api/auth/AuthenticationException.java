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
package com.codenvy.api.auth;

@SuppressWarnings("serial")
public class AuthenticationException extends Exception {
    /**
     * Response status if any exception occurs,
     * <br>
     * Default value: 400
     */
    int responseStatus;

    public AuthenticationException() {
        this(400);
    }

    public AuthenticationException(String message, Throwable cause) {
        this(400, message, cause);
    }

    public AuthenticationException(String message) {
        this(400, message);
    }

    public AuthenticationException(Throwable cause) {
        this(400, cause);
    }

    public AuthenticationException(int responseStatus) {
        super();
        this.responseStatus = responseStatus;
    }

    public AuthenticationException(int responseStatus, String message, Throwable cause) {
        super(message, cause);
        this.responseStatus = responseStatus;
    }

    public AuthenticationException(int responseStatus, String message) {
        super(message);
        this.responseStatus = responseStatus;
    }

    public AuthenticationException(int responseStatus, Throwable cause) {
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
