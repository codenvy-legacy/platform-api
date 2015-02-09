/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.factory;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Factory URL accept stage builder.
 */
@Singleton
public class FactoryAcceptValidatorImpl extends FactoryBaseValidator implements FactoryAcceptValidator {
    @Inject
    public FactoryAcceptValidatorImpl(AccountDao accountDao,
                                      UserDao userDao,
                                      UserProfileDao profileDao) {

        super(accountDao, userDao, profileDao);
    }

    @Override
    public void validateOnAccept(Factory factory, boolean encoded) throws ApiException {
        if (!encoded) {
            validateSource(factory);
            validateProjectName(factory);
        }
        validateWorkspace(factory);
        validateCurrentTimeBetweenSinceUntil(factory);
        validateProjectActions(factory);
    }
}
