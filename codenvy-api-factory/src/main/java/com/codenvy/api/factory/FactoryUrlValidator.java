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
package com.codenvy.api.factory;

import com.codenvy.api.factory.dto.AdvancedFactoryUrl;
import com.codenvy.api.factory.dto.SimpleFactoryUrl;

import java.net.URL;

/**
 * Interface for different validations of factory urls
 *
 * @author Alexander Garagatyi
 */
public interface FactoryUrlValidator {

    /**
     * Retrieve factory url object using certain URL and validates it.
     * Implementation should throw {@link com.codenvy.api.factory.FactoryUrlException}
     * if factory url object is invalid or can not be retrieved.
     *
     * @param factoryUrl
     *         - factory url to validate
     * @throws FactoryUrlException
     */
    void validateUrl(URL factoryUrl) throws FactoryUrlException;

    /**
     * Validates factory url object. Implementation should throw
     * {@link com.codenvy.api.factory.FactoryUrlException} if factory url object is invalid.
     *
     * @param factoryUrl
     *         - factory url to validate
     * @throws FactoryUrlException
     */
    void validateUrl(SimpleFactoryUrl factoryUrl) throws FactoryUrlException;

    /**
     * Validates factory url object. Implementation should throw
     * {@link com.codenvy.api.factory.FactoryUrlException} if factory url is invalid.
     *
     * @param factoryUrl
     *         - factory url to validate
     * @throws FactoryUrlException
     */
    void validateUrl(AdvancedFactoryUrl factoryUrl) throws FactoryUrlException;
}
