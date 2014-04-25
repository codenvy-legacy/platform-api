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

/**
 * @author Eugene Voevodin
 */
public class SubscriptionEvent {

    public static enum EventType {
        CREATE,
        CHANGE,
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
