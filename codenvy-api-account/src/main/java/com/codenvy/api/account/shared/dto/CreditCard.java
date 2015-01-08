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
 * Describe credit card
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface CreditCard {
    String getToken();

    void setToken(String token);

    CreditCard withToken(String token);

    String getNumber();

    void setNumber(String number);

    CreditCard withNumber(String number);

    String getExpiration();

    void setExpiration(String expiration);

    CreditCard withExpiration(String expiration);

    String getAccountId();

    void setAccountId(String accountId);

    CreditCard withAccountId(String accountId);

    String getCardholder();

    void setCardholder(String cardholder);

    CreditCard withCardholder(String cardholder);

    String getType();

    void setType(String type);

    CreditCard withType(String type);
}
