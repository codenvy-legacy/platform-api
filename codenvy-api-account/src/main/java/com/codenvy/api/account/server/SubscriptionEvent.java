/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.account.server;

import com.codenvy.api.account.shared.dto.Subscription;

/**
 * @author Eugene Voevodin
 */
public class SubscriptionEvent {

    public static enum EventType {
        CREATE,
        CHECK,
        REMOVE
    }

    private final Subscription subscription;
    private final EventType    eventType;

    public SubscriptionEvent(Subscription subscription, EventType eventType) {
        this.eventType = eventType;
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public EventType getEventType() {
        return eventType;
    }
}
