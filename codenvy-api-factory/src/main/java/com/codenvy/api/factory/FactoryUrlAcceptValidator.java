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


import com.codenvy.api.factory.dto.Factory;

/**
 * Interface for validations of factory urls on accept stage.
 *
 **/

public interface FactoryUrlAcceptValidator {

    /**
     * Validates factory url object on accept stage. Implementation should throw
     * {@link com.codenvy.api.factory.FactoryUrlException} if factory url object is invalid.
     *
     * @param factory
     *         factory object to validate
     * @throws FactoryUrlException
     *         - in case if factory is not valid
     */
    void validateOnAccept(Factory factory, boolean encoded) throws FactoryUrlException;
}
