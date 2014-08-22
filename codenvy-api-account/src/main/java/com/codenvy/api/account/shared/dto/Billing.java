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
 * @author Alexander Garagatyi
 */
@DTO
public interface Billing {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
     */

    String getUsePaymentSystem();

    void setUsePaymentSystem(String usePaymentSystem);

    Billing withUsePaymentSystem(String usePaymentSystem);

    String getPaymentToken();

    void setPaymentToken(String paymentToken);

    Billing withPaymentToken(String paymentToken);

    String getStartDate();

    void setStartDate(String startDate);

    Billing withStartDate(String startDate);

    String getEndDate();

    void setEndDate(String endDate);

    Billing withEndDate(String endDate);

    Integer getCycle();

    void setCycle(Integer cycle);

    Billing withCycle(Integer cycle);

    Integer getCycleType();

    void setCycleType(Integer cycleType);

    Billing withCycleType(Integer cycleType);

    Integer getContractTerm();

    void setContractTerm(Integer contractTerm);

    Billing withContractTerm(Integer contractTerm);
}
