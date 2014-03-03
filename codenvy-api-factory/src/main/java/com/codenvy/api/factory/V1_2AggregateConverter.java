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

import java.lang.reflect.Method;

/**
 * @author Alexander Garagatyi
 */
public class V1_2AggregateConverter implements CompatibilityConverter {
    @Override
    public void convert(Method method, Object object) throws FactoryUrlException {
        Factory factory = (Factory)object;
        // TODO
        switch (method.getName()) {
            case "getPname":
                convertPname(factory);
                break;
            case "getPtype":
                convertPtype(factory);
                break;
            case "getIdCommit":
                convertIdCommit(factory);
                break;
            case "getValidsince":
                convertValidsince(factory);
                break;
            case "getValiduntil":
                convertValiduntil(factory);
                break;
            default:
                throw new FactoryUrlException(String.format("%s can't process method %s", getClass().getName(), method.getName()));
        }
    }

    private void convertIdCommit(Factory factory) {

    }

    private void convertPname(Factory factory) {

    }

    private void convertPtype(Factory factory) {

    }

    private void convertValidsince(Factory factory) {

    }

    private void convertValiduntil(Factory factory) {

    }
}
