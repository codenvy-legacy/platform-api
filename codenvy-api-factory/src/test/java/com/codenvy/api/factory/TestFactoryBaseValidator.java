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
import com.codenvy.api.user.server.dao.PreferenceDao;
import com.codenvy.api.user.server.dao.UserDao;

/**
 * @author Sergii Kabashniuk
 */
public class TestFactoryBaseValidator extends FactoryBaseValidator {

    public TestFactoryBaseValidator(AccountDao accountDao,
                                    UserDao userDao,
                                    PreferenceDao preferenceDao,
                                    boolean onPremises) {
        super(accountDao, userDao, preferenceDao, onPremises);
    }
}
