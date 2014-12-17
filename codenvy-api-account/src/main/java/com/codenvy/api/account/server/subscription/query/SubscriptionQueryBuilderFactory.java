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
import java.util.Date;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public class SubscriptionQueryBuilderFactory {
    @Inject
    private SubscriptionQueryBuilder subscriptionQueryBuilder;

    public SubscriptionQueryBuilder create() {
        return subscriptionQueryBuilder.create();
    }

    public SubscriptionQueryBuilder copy() {
        return subscriptionQueryBuilder.copy();
    }

    public static abstract class SubscriptionQueryBuilder {
        public abstract SubscriptionQueryBuilder setId(SubscriptionQuery<String> query);

        public abstract SubscriptionQueryBuilder setAccountId(SubscriptionQuery<String> query);

        public abstract SubscriptionQueryBuilder setServiceId(SubscriptionQuery<String> query);

        public abstract SubscriptionQueryBuilder setPlanId(SubscriptionQuery<String> query);

        public abstract SubscriptionQueryBuilder setProperties(SubscriptionQuery<Map<String, String>> query);

        public abstract SubscriptionQueryBuilder setState(SubscriptionQuery<String> query);

        public abstract SubscriptionQueryBuilder setStartDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setEndDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setTrialStartDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setTrialEndDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setUsePaymentSystem(SubscriptionQuery<Boolean> query);

        public abstract SubscriptionQueryBuilder setBillingStartDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setBillingEndDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setNextBillingDate(SubscriptionQuery<Date> query);

        public abstract SubscriptionQueryBuilder setBillingCycle(SubscriptionQuery<Integer> query);

        public abstract SubscriptionQueryBuilder setBillingCycleType(SubscriptionQuery<Integer> query);

        public abstract SubscriptionQueryBuilder setBillingContractTerm(SubscriptionQuery<Integer> query);

        public abstract SubscriptionQueryBuilder setDescription(SubscriptionQuery<String> query);

        public abstract Object build();

        protected abstract SubscriptionQueryBuilder create();

        protected abstract SubscriptionQueryBuilder copy();
    }
}
