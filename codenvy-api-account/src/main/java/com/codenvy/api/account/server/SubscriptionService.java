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
package com.codenvy.api.account.server;


import com.codenvy.api.account.shared.dto.Subscription;
import com.codenvy.api.core.ApiException;

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

    public abstract void beforeCreateSubscription(Subscription subscription) throws ApiException;

    public abstract void afterCreateSubscription(Subscription subscription) throws ApiException;

    public abstract void onRemoveSubscription(Subscription subscription) throws ApiException;

    public abstract void onCheckSubscription(Subscription subscription) throws ApiException;

    public abstract void onUpdateSubscription(Subscription oldSubscription, Subscription newSubscription) throws ApiException;

    public abstract double tarifficate(Subscription subscription) throws ApiException;

    public String getServiceId() {
        return serviceId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
