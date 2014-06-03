/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
package com.codenvy.api.account.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.math.BigDecimal;

/**
 * References payment and Subscription.
 * Theoretically Amount may be not the same as actual payment, here it is interpreted as the sum for this Subscription.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionPayment {
    String getSubscriptionId();

    void setSubscriptionId(String subscriptionId);

    SubscriptionPayment withSubscriptionId(String subscriptionId);

    String getTransactionId();

    void setTransactionId(String transactionId);

    SubscriptionPayment withTransactionId(String transactionId);

    BigDecimal getAmount();

    void setAmount(BigDecimal amount);

    SubscriptionPayment withAmount(BigDecimal amount);
}
