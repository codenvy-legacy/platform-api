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

import java.util.Map;

/**
 * Represents tariff plan
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Plan {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
    */
    String getId();

    void setId(String id);

    Plan withId(String id);

    String getServiceId();

    void setServiceId(String serviceId);

    Plan withServiceId(String serviceId);

    Boolean isPaid();

    void setPaid(Boolean paid);

    Plan withPaid(Boolean paid);

    Boolean getSalesOnly();

    void setSalesOnly(Boolean salesOnly);

    Plan withSalesOnly(Boolean salesOnly);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Plan withProperties(Map<String, String> properties);

    Integer getTrialDuration();

    void setTrialDuration(Integer trialDuration);

    Plan withTrialDuration(Integer trialDuration);

    String getDescription();

    void setDescription(String description);

    Plan withDescription(String description);

    Integer getBillingCycle();

    void setBillingCycle(Integer cycle);

    Plan withBillingCycle(Integer cycle);

    Integer getBillingCycleType();

    void setBillingCycleType(Integer billingCycleType);

    Plan withBillingCycleType(Integer billingCycleType);

    Integer getBillingContractTerm();

    void setBillingContractTerm(Integer contractTerm);

    Plan withBillingContractTerm(Integer contractTerm);
}
