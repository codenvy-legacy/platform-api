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
package com.codenvy.api.factory;

import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Policies;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;

/**
 * Factory URL creation stage builder.
 */
@Singleton
public class FactoryUrlCreateValidatorImpl extends FactoryUrlBaseValidator implements FactoryUrlCreateValidator {
    @Inject
    public FactoryUrlCreateValidatorImpl(AccountDao accountDao,
                                         UserDao userDao,
                                         UserProfileDao profileDao,
                                         @Named("subscription.orgaddon.enabled") boolean onPremises) {
        super(accountDao,userDao,profileDao, onPremises);
    }

    @Override
    public void validateOnCreate(Factory factory) throws ApiException {
        validateSource(factory);
        validateProjectName(factory);
        validateOrgid(factory);
        validateTrackedFactoryAndParams(factory);
        validateCurrentTimeBeforeSinceUntil(factory);
    }
}
