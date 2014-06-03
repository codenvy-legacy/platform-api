/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.account.server;


import com.codenvy.api.account.shared.dto.Subscription;

import java.math.BigDecimal;

/**
 * Base class for any service which may communicate with account via subscriptions
 *
 * @author Eugene Voevodin
 */
public abstract class SubscriptionService {

    private final String serviceId;
    private final String displayName;

    public SubscriptionService(String serviceId, String displayName) {
        this.serviceId = serviceId;
        this.displayName = displayName;
    }

    public abstract void onCreateSubscription(Subscription subscription);

    public abstract void onRemoveSubscription(Subscription subscription);

    public abstract void onCheckSubscription(Subscription subscription);

    public abstract void onUpdateSubscription(Subscription oldSubscription, Subscription newSubscription);

    public abstract BigDecimal tarifficate(Subscription subscription);

    public String getServiceId() {
        return serviceId;
    }

    public String getDisplayName() {
        return displayName;
    }
}