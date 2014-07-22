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
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * References payment and subscription.
 * Theoretically Amount may be not the same as actual payment, here it is interpreted as the sum for this Subscription.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionPaymentDescriptor {

    String getTransactionId();

    void setTransactionId(String transactionId);

    SubscriptionPaymentDescriptor withTransactionId(String transactionId);

    double getAmount();

    void setAmount(double amount);

    SubscriptionPaymentDescriptor withAmount(double amount);
}
