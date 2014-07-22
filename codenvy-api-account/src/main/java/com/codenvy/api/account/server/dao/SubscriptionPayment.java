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
public class SubscriptionPayment {

    private double amount;
    private String transactionId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public SubscriptionPayment withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public SubscriptionPayment withAmount(double amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubscriptionPayment)) {
            return false;
        }
        final SubscriptionPayment other = (SubscriptionPayment)obj;
        return amount == amount && Objects.equals(transactionId, other.getTransactionId());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        final long amountBits = Double.doubleToLongBits(amount);
        hash = 31 * hash + (int)(amountBits ^ (amountBits >>> 32));
        hash = 31 * hash + Objects.hashCode(transactionId);
        return hash;
    }
}
