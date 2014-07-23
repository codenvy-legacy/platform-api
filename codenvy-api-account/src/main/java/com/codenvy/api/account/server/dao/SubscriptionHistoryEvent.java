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

    private long         time;
    private double       amount;
    private String       id;
    private String       userId;
    private Type         type;
    private Subscription subscription;
    private String       transactionId;

    public SubscriptionHistoryEvent() {
    }

    public SubscriptionHistoryEvent(SubscriptionHistoryEvent other) {
        this.time = other.time;
        this.id = other.id;
        this.userId = other.userId;
        this.type = other.type;
        this.subscription = new Subscription(other.subscription);
        this.transactionId = other.transactionId;
        this.amount = other.amount;
    }

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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public SubscriptionHistoryEvent withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public SubscriptionHistoryEvent withAmount(double amount) {
        this.amount = amount;
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
               amount == other.amount &&
               Objects.equals(transactionId, other.transactionId) &&
               Objects.equals(id, other.id) &&
               Objects.equals(userId, other.userId) &&
               Objects.equals(type, other.type) &&
               Objects.equals(subscription, other.subscription);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        final long amountBits = Double.doubleToLongBits(amount);
        hash = 31 * hash + (int)(time ^ (time >>> 32));
        hash = 31 * hash + (int)(amountBits ^ (amountBits >>> 32));
        hash = 31 * hash + Objects.hashCode(transactionId);
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(userId);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(subscription);
        return hash;
    }
}
