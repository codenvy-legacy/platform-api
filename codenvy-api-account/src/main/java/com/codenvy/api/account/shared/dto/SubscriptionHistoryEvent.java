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
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Represents entry of subscriptions states history
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionHistoryEvent {

    public enum Type {
        CREATE, UPDATE, DELETE
    }

    String getId();

    void setId(String id);

    SubscriptionHistoryEvent withId(String id);

    String getUserId();

    void setUserId(String userId);

    SubscriptionHistoryEvent withUserId(String userId);

    Type getType();

    void setType(Type type);

    SubscriptionHistoryEvent withType(Type type);

    long getTime();

    void setTime(long date);

    SubscriptionHistoryEvent withTime(long date);

    Subscription getSubscription();

    void setSubscription(Subscription subscription);

    SubscriptionHistoryEvent withSubscription(Subscription subscription);

    String getTransactionId();

    void setTransactionId(String transactionId);

    SubscriptionHistoryEvent withTransactionId(String transactionId);

    double getAmount();

    void setAmount(double amount);

    SubscriptionHistoryEvent withAmount(double amount);
}
