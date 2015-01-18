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
package com.codenvy.api.account.server.dao;

import com.codenvy.api.core.ServerException;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
public interface SubscriptionQueryBuilder {
    public static interface SubscriptionQuery {
        List<Subscription> execute() throws ServerException;
    }

    SubscriptionQuery getTrialQuery(String service, String accountId);

    SubscriptionQuery getChargeQuery(String service);

    SubscriptionQuery getExpiringQuery(String service, int days);

    SubscriptionQuery getExpiredQuery(String service, int days);

    SubscriptionQuery getTrialExpiredQuery(String service);
}
