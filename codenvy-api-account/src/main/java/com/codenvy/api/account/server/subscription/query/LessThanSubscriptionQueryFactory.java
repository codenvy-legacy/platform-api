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
public class LessThanSubscriptionQueryFactory {
    @Inject
    private LessThanSubscriptionQuery query;

    public <T> LessThanSubscriptionQuery<T> create(T value, boolean isEqualsMatch) {
        return query.create(value, isEqualsMatch);
    }

    public static abstract class LessThanSubscriptionQuery<T> implements SubscriptionQuery<T> {
        protected abstract LessThanSubscriptionQuery<T> create(T value, boolean isEqualsMatch);
    }
}
