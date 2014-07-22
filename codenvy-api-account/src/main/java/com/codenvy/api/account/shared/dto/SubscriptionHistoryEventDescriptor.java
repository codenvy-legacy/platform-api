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

import com.codenvy.api.account.server.dao.SubscriptionHistoryEvent.Type;
import com.codenvy.dto.shared.DTO;

/**
 * Represents entry of subscriptions states history
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionHistoryEventDescriptor {

    String getId();

    void setId(String id);

    SubscriptionHistoryEventDescriptor withId(String id);

    String getUserId();

    void setUserId(String userId);

    SubscriptionHistoryEventDescriptor withUserId(String userId);

    Type getType();

    void setType(Type type);

    SubscriptionHistoryEventDescriptor withType(Type type);

    long getTime();

    void setTime(long date);

    SubscriptionHistoryEventDescriptor withTime(long date);

    SubscriptionDescriptor getSubscription();

    void setSubscription(SubscriptionDescriptor subscription);

    SubscriptionHistoryEventDescriptor withSubscription(SubscriptionDescriptor subscription);

    String getTransactionId();

    void setTransactionId(String transactionId);

    SubscriptionHistoryEventDescriptor withTransactionId(String transactionId);

    double getAmount();

    void setAmount(double amount);

    SubscriptionHistoryEventDescriptor withAmount(double amount);
}
