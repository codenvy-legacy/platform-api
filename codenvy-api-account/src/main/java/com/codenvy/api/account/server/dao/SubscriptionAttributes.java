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
 * Describes attributes of saved subscription
 *
 * @author Alexander Garagatyi
 */
public class SubscriptionAttributes {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
     */
    private String              description;
    private String              startDate;
    private String              endDate;
    private Integer             trialDuration;
    private Billing             billing;
    private Map<String, String> custom;

    public SubscriptionAttributes() {
    }

    public SubscriptionAttributes(SubscriptionAttributes other) {
        this.description = other.description;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.trialDuration = other.trialDuration;
        this.billing = other.billing;
        this.custom = other.custom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubscriptionAttributes withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public SubscriptionAttributes withStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public SubscriptionAttributes withEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public Integer getTrialDuration() {
        return trialDuration;
    }

    public void setTrialDuration(Integer trialDuration) {
        this.trialDuration = trialDuration;
    }

    public SubscriptionAttributes withTrialDuration(Integer trialDuration) {
        this.trialDuration = trialDuration;
        return this;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public SubscriptionAttributes withBilling(Billing billing) {
        this.billing = billing;
        return this;
    }

    public Map<String, String> getCustom() {
        if (custom == null) {
            custom = new HashMap<>();
        }
        return custom;
    }

    public void setCustom(Map<String, String> custom) {
        this.custom = custom;
    }

    public SubscriptionAttributes withCustom(Map<String, String> custom) {
        this.custom = custom;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionAttributes)) {
            return false;
        }

        SubscriptionAttributes that = (SubscriptionAttributes)o;

        return Objects.equals(billing,that.billing) &&
               Objects.equals(getCustom(), that.getCustom()) &&
               Objects.equals(description, that.description) &&
               Objects.equals(endDate, that.endDate) &&
               Objects.equals(startDate, that.startDate) &&
               Objects.equals(trialDuration, that.trialDuration);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(description);
        hash = 31 * hash + Objects.hashCode(startDate);
        hash = 31 * hash + Objects.hashCode(endDate);
        hash = 31 * hash + Objects.hashCode(trialDuration);
        hash = 31 * hash + Objects.hashCode(billing);
        hash = 31 * hash + Objects.hashCode(getCustom());
        return hash;
    }
}
