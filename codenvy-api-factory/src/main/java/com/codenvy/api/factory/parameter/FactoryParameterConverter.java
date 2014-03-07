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
package com.codenvy.api.factory.parameter;

import com.codenvy.api.factory.FactoryUrlException;

/**
 * @author Alexander Garagatyi
 */
public abstract class FactoryParameterConverter {
    protected final Object object;

    public FactoryParameterConverter(Object object) {
        this.object = object;
    }

    public abstract void convert() throws FactoryUrlException;

    public static class DefaultFactoryParameterConverter extends FactoryParameterConverter {
        public DefaultFactoryParameterConverter(Object object) {
            super(object);
        }

        @Override
        public void convert() throws FactoryUrlException {
            // do nothing
        }
    }
}
