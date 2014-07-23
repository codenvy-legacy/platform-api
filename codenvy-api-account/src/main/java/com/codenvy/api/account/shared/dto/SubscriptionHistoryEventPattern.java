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
 * Represents search pattern for subscriptions history
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionHistoryEventPattern {

    String getId();

    void setId(String id);

    SubscriptionHistoryEventPattern withId(String id);

    String getUserId();

    void setUserId(String userId);

    SubscriptionHistoryEventPattern withUserId(String userId);

    Type getType();

    void setType(Type type);

    SubscriptionHistoryEventPattern withType(Type type);

    SubscriptionDescriptor getSubscription();

    void setSubscription(SubscriptionDescriptor subscription);

    SubscriptionHistoryEventPattern withSubscription(SubscriptionDescriptor subscription);

    String getTransactionId();

    void setTransactionId(String transactionId);

    SubscriptionHistoryEventPattern withTransactionId(String transactionId);
}
