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
 * Describes billing properties of the subscription properties of the subscription
 *
 * @author Alexander Garagatyi
 */
public class Billing {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
         * that allow better validate data that was sent
         */
    private String usePaymentSystem;
    private String startDate;
    private String endDate;
    private Integer    cycle;
    private Integer    cycleType;
    private Integer    contractTerm;

    public Billing() {
    }

    public Billing(Billing other) {
        this.usePaymentSystem = other.usePaymentSystem;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.cycle = other.cycle;
        this.cycleType = other.cycleType;
        this.contractTerm = other.contractTerm;
    }

    public String getUsePaymentSystem() {
        return usePaymentSystem;
    }

    public void setUsePaymentSystem(String usePaymentSystem) {
        this.usePaymentSystem = usePaymentSystem;
    }

    public Billing withUsePaymentSystem(String usePaymentSystem) {
        this.usePaymentSystem = usePaymentSystem;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public Billing withStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Billing withEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public Integer getCycle() {
        return cycle;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public Billing withCycle(Integer cycle) {
        this.cycle = cycle;
        return this;
    }

    public Integer getCycleType() {
        return cycleType;
    }

    public void setCycleType(Integer cycleType) {
        this.cycleType = cycleType;
    }

    public Billing withCycleType(Integer cycleType) {
        this.cycleType = cycleType;
        return this;
    }

    public Integer getContractTerm() {
        return contractTerm;
    }

    public void setContractTerm(Integer contractTerm) {
        this.contractTerm = contractTerm;
    }

    public Billing withContractTerm(Integer contractTerm) {
        this.contractTerm = contractTerm;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Billing)) {
            return false;
        }

        Billing billing = (Billing)o;

        return Objects.equals(contractTerm, billing.contractTerm) &&
               Objects.equals(cycle, billing.cycle) &&
               Objects.equals(cycleType, billing.cycleType) &&
               Objects.equals(endDate, billing.endDate) &&
               Objects.equals(startDate, billing.startDate) &&
               Objects.equals(usePaymentSystem, billing.usePaymentSystem);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(usePaymentSystem);
        hash = 31 * hash + Objects.hashCode(startDate);
        hash = 31 * hash + Objects.hashCode(endDate);
        hash = 31 * hash + Objects.hashCode(cycle);
        hash = 31 * hash + Objects.hashCode(cycleType);
        hash = 31 * hash + Objects.hashCode(contractTerm);
        return hash;
    }
}
