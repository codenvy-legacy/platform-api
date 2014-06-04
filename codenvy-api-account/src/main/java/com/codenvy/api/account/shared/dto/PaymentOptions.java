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

/**
 * Payment information about card, subscription to pay for.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface PaymentOptions {
    String getCardNumber();

    void setCardNumber(String cardNumber);

    PaymentOptions withCardNumber(String cardNumber);

    String getCvv();

    void setCvv(String cvv);

    PaymentOptions withCvv(String cvv);

    String getExpirationMonth();

    void setExpirationMonth(String expirationMonth);

    PaymentOptions withExpirationMonth(String expirationMonth);

    String getExpirationYear();

    void setExpirationYear(String expirationYear);

    PaymentOptions withExpirationYear(String expirationYear);

    String getSubscriptionId();

    void setSubscriptionId(String subscriptionId);

    PaymentOptions withSubscriptionId(String subscriptionId);
}
