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
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class ValidUntilConverter implements FactoryParameterConverter {
    @Override
    public void convert(Object object) throws FactoryUrlException {
        // TODO check that user didn't used multiple version of parameter
        Factory factory = (Factory)object;

        Restriction restriction = factory.getRestriction();
        if (restriction == null) {
            restriction = DtoFactory.getInstance().createDto(Restriction.class);
            factory.setRestriction(restriction);
        }

        restriction.setValiduntil(factory.getValiduntil());
        factory.setValiduntil(0);
    }
}
