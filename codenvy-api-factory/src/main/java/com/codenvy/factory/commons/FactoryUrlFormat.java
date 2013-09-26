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
package com.codenvy.factory.commons;

import java.net.URL;

/** Provide a way of factory url parameters validation and parsing */
public interface FactoryUrlFormat {
    /**
     * Parse factory url parameters
     *
     * @param url
     *         - factory url to parse
     * @return <code>FactoryUrl</code> with factory url parameters if url is valid
     * @throws FactoryUrlInvalidArgumentException
     *         - if url satisfy format, but arguments is invalid
     * @throws FactoryUrlInvalidFormatException
     *         - if url doesn't satisfy format
     * @throws FactoryUrlException
     *         - if other exceptions occurs
     */
    public FactoryUrl parse(URL url) throws FactoryUrlException;
}
