/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.account.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Hyperlinks;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/29/15.
 *
 */
@DTO
public interface CreditCardDescriptor extends Hyperlinks {
    String getNumber();

    void setNumber(String number);

    CreditCardDescriptor withNumber(String number);

    String getExpiration();

    void setExpiration(String expiration);

    CreditCardDescriptor withExpiration(String expiration);

    String getAccountId();

    void setAccountId(String accountId);

    CreditCardDescriptor withAccountId(String accountId);

    String getCardholder();

    void setCardholder(String cardholder);

    CreditCardDescriptor withCardholder(String cardholder);

    String getType();

    void setType(String type);

    CreditCardDescriptor withType(String type);

    CreditCardDescriptor withLinks(List<Link> links);
}
