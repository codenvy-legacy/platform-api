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

/**
 * @author Alexander Garagatyi
 */
public class ValidUntilConverter implements LegacyConverter {
    @Override
    public void convert(Factory factory) throws FactoryUrlException {
        if (factory.getValiduntil() > 0) {
            Restriction restriction = factory.getRestriction();
            if (restriction == null || restriction.getValiduntil() == 0) {
                restriction = restriction == null ? DtoFactory.getInstance().createDto(Restriction.class) : restriction;
                restriction.setValidsince(factory.getValiduntil());
                factory.setRestriction(restriction);
                factory.setValiduntil(0);
            } else if (restriction.getValiduntil() != 0) {
                throw new FactoryUrlException(
                        "Parameters 'validuntill' and 'restriction.validuntill' are mutually exclusive.");
            }
        }
    }
}
