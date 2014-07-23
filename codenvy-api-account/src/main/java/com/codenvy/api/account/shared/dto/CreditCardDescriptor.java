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
public interface CreditCardDescriptor {
    String getCardNumber();

    void setCardNumber(String cardNumber);

    CreditCardDescriptor withCardNumber(String cardNumber);

    String getExpirationMonth();

    void setExpirationMonth(String expirationMonth);

    CreditCardDescriptor withExpirationMonth(String expirationMonth);

    String getExpirationYear();

    void setExpirationYear(String expirationYear);

    CreditCardDescriptor withExpirationYear(String expirationYear);

    String getCardholderName();

    void setCardholderName(String cardholderName);

    CreditCardDescriptor withCardholderName(String cardholderName);
}
