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
package com.codenvy.api.account.server.dao;

import java.util.Objects;

/**
 * @author Eugene Voevodin
 */
public class SubscriptionHistoryEvent {

    public enum Type {
        CREATE,
        UPDATE,
        DELETE
    }

    private long                time;
    private String              id;
    private String              userId;
    private Type                type;
    private Subscription        subscription;
    private SubscriptionPayment subscriptionPayment;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionHistoryEvent withId(String id) {
        this.id = id;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SubscriptionHistoryEvent withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public SubscriptionHistoryEvent.Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public SubscriptionHistoryEvent withType(Type type) {
        this.type = type;
        return this;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public SubscriptionHistoryEvent withTime(long time) {
        this.time = time;
        return this;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public SubscriptionHistoryEvent withSubscription(Subscription subscription) {
        this.subscription = subscription;
        return this;
    }

    public SubscriptionPayment getSubscriptionPayment() {
        return subscriptionPayment;
    }

    public void setSubscriptionPayment(SubscriptionPayment subscriptionPayment) {
        this.subscriptionPayment = subscriptionPayment;
    }

    public SubscriptionHistoryEvent withSubscriptionPayment(SubscriptionPayment subscriptionPayment) {
        this.subscriptionPayment = subscriptionPayment;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubscriptionHistoryEvent)) {
            return false;
        }
        final SubscriptionHistoryEvent other = (SubscriptionHistoryEvent)obj;
        return time == other.time &&
               Objects.equals(id, other.id) &&
               Objects.equals(userId, other.userId) &&
               Objects.equals(type, other.type) &&
               Objects.equals(subscription, other.subscription) &&
               Objects.equals(subscriptionPayment, other.subscriptionPayment);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int)(time ^ (time >>> 32));
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(userId);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(subscription);
        hash = 31 * hash + Objects.hashCode(subscriptionPayment);
        return hash;
    }
}
