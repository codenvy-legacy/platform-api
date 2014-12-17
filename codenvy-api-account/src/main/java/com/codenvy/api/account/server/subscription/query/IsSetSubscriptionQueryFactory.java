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
package com.codenvy.api.account.server.subscription.query;

import javax.inject.Inject;

/**
 * @author Alexander Garagatyi
 */
public class IsSetSubscriptionQueryFactory {
    @Inject
    private IsSetSubscriptionQuery isSetSubscriptionQuery;

    public <T> IsSetSubscriptionQuery<T> create(T value) {
        return isSetSubscriptionQuery.create(value);
    }

    public static abstract class IsSetSubscriptionQuery<T> implements SubscriptionQuery<T> {
        protected abstract IsSetSubscriptionQuery<T> create(T value);
    }
}
