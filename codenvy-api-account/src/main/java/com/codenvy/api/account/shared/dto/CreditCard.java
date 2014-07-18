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
 * Payment information about card, subscription to pay for.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface CreditCard {
    String getCardNumber();

    void setCardNumber(String cardNumber);

    CreditCard withCardNumber(String cardNumber);

    String getCvv();

    void setCvv(String cvv);

    CreditCard withCvv(String cvv);

    String getExpirationMonth();

    void setExpirationMonth(String expirationMonth);

    CreditCard withExpirationMonth(String expirationMonth);

    String getExpirationYear();

    void setExpirationYear(String expirationYear);

    CreditCard withExpirationYear(String expirationYear);

    String getCardholderName();

    void setCardholderName(String cardholderName);

    CreditCard withCardholderName(String cardholderName);
}
