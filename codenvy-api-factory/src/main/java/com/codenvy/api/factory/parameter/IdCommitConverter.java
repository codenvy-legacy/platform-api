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

/**
 * Convert placement of commit id from 'idcommit' parameter to 'commitid'.
 *
 * @author Alexander Garagatyi
 */
public class IdCommitConverter implements LegacyConverter {

    @Override
    public void convert(Factory factory) throws FactoryUrlException {

        if (factory.getCommitid() != null && factory.getIdcommit() != null) {
            throw new FactoryUrlException("Parameters 'commitid' and 'idcommit' are mutually exclusive.");
        } else if (factory.getCommitid() == null) {
            factory.setCommitid(factory.getIdcommit());
            factory.setIdcommit(null);
        }
    }
}
