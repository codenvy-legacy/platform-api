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

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describe receipt
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface Receipt {
    String getAccountId();

    void setAccountId(String accountId);

    Receipt withAccountId(String accountId);


    Long getId();

    void setId(Long id);

    Receipt withId(Long id);


    Double getTotal();

    void setTotal(Double total);

    Receipt withTotal(Double total);


    String getBillingPeriod();

    void setBillingPeriod(String billingPeriod);

    Receipt withBillingPeriod(String billingPeriod);


    String getCreditCardId();

    void setCreditCardId(String creditCardId);

    Receipt withCreditCardId(String creditCardId);


    Long getPaymentDate();

    void setPaymentDate(Long paymentDate);

    Receipt withPaymentDate(Long paymentDate);


    Long getMailingDate();

    void setMailingDate(Long mailingDate);

    Receipt withMailingDate(Long mailingDate);


    List<Charge> getCharges();

    void setCharges(List<Charge> charges);

    Receipt withCharges(List<Charge> charges);

    List<MemoryChargeDetails> getMemoryChargeDetails();

    void setMemoryChargeDetails(List<MemoryChargeDetails> memoryChargeDetails);

    Receipt withMemoryChargeDetails(List<MemoryChargeDetails> memoryChargeDetails);


}
