/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.factory.converter;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.factory.dto.Factory;

import java.util.HashMap;

/**
 * Convert placement of commit id from 'idcommit' or 'commitid' parameter to 'source.parameters.commitId'
 *
 * @author Alexander Garagatyi
 */
public class IdCommitConverter implements LegacyConverter {

    @Override
    public void convert(Factory factory) throws ApiException {
    }

    @Override
    public void convertToV1_2(Factory factory) throws ApiException {
        if (factory.getCommitid() != null && factory.getIdcommit() != null) {
            throw new ApiException("Parameters 'commitid' and 'idcommit' are mutually exclusive.");
        } else if (factory.getCommitid() == null) {
            factory.setCommitid(factory.getIdcommit());
        }
    }
}
