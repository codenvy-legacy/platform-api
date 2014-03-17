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

/**
 * Proxies response from remote server to the client. It helps to avoid download response from remote server and resend info to the client.
 * Instead implementation of this interface may pump information from the remote server directly to the client.
 *
 * @author andrew00x
 */
public interface HttpOutputProvider extends OutputProvider {
    /** Set HTTP status. */
    void setStatus(int status);

    /**
     * Add HTTP header.
     *
     * @param name
     *         name of header
     * @param value
     *         value of header
     */
    void addHttpHeader(String name, String value);
}
