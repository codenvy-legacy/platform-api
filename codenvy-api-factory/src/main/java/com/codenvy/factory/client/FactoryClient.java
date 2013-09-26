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
package com.codenvy.factory.client;

import com.codenvy.factory.commons.AdvancedFactoryUrl;
import com.codenvy.factory.commons.FactoryUrlException;

import java.net.URL;

/** Allows to get factory from factories storage. */
public interface FactoryClient {
    /**
     * Get factory from storage by id.
     * @param factoryUrl - factory URL
     * @param id - factory id
     * @return - stored factory if id is correct, null otherwise
     * @throws FactoryUrlException
     */
    public AdvancedFactoryUrl getFactory(URL factoryUrl, String id) throws FactoryUrlException;
}
