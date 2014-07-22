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
public interface NewCreditCard {
    String getCardNumber();

    void setCardNumber(String cardNumber);

    NewCreditCard withCardNumber(String cardNumber);

    String getCvv();

    void setCvv(String cvv);

    NewCreditCard withCvv(String cvv);

    String getExpirationMonth();

    void setExpirationMonth(String expirationMonth);

    NewCreditCard withExpirationMonth(String expirationMonth);

    String getExpirationYear();

    void setExpirationYear(String expirationYear);

    NewCreditCard withExpirationYear(String expirationYear);

    String getCardholderName();

    void setCardholderName(String cardholderName);

    NewCreditCard withCardholderName(String cardholderName);
}
