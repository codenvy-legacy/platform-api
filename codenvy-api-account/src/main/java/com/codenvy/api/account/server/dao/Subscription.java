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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes subscription - link between {@link com.codenvy.api.account.server.SubscriptionService} and
 * {@link com.codenvy.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 */
public class Subscription {

    public enum State {
        WAIT_FOR_PAYMENT,
        ACTIVE
    }

    private long                startDate;
    private long                endDate;
    private String              id;
    private String              accountId;
    private String              serviceId;
    private State               state;
    private Map<String, String> properties;

    public Subscription() {
    }

    public Subscription(Subscription other) {
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.id = other.id;
        this.accountId = other.accountId;
        this.serviceId = other.serviceId;
        this.state = other.state;
        this.properties = new HashMap<>(other.getProperties());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Subscription withId(String id) {
        this.id = id;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Subscription withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Subscription withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public Subscription withStartDate(long startDate) {
        this.startDate = startDate;
        return this;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public Subscription withEndDate(long endDate) {
        this.endDate = endDate;
        return this;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Subscription withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Subscription withState(State state) {
        this.state = state;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Subscription)) {
            return false;
        }
        final Subscription other = (Subscription)obj;
        return startDate == other.startDate &&
               endDate == other.endDate &&
               state == other.state &&
               Objects.equals(id, other.id) &&
               Objects.equals(accountId, other.accountId) &&
               Objects.equals(serviceId, other.serviceId) &&
               Objects.equals(getProperties(), other.getProperties());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int)(startDate ^ (startDate >>> 32));
        hash = 31 * hash + (int)(endDate ^ (endDate >>> 32));
        hash = 31 * hash + Objects.hashCode(state);
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(accountId);
        hash = 31 * hash + Objects.hashCode(serviceId);
        hash = 31 * hash + Objects.hashCode(getProperties());
        return hash;
    }
}