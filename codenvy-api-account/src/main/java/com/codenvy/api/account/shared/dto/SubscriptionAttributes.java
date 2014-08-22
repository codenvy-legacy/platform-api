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
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionAttributes {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
     */

    String getDescription();

    void setDescription(String description);

    SubscriptionAttributes withDescription(String description);

    String getStartDate();

    void setStartDate(String startDate);

    SubscriptionAttributes withStartDate(String startDate);

    String getEndDate();

    void setEndDate(String endDate);

    SubscriptionAttributes withEndDate(String endDate);

    Integer getTrialDuration();

    void setTrialDuration(Integer trialDuration);

    SubscriptionAttributes withTrialDuration(Integer trialDuration);

    Billing getBilling();

    void setBilling(Billing billing);

    SubscriptionAttributes withBilling(Billing billing);

    Map<String, String> getCustom();

    void setCustom(Map<String, String> other);

    SubscriptionAttributes withCustom(Map<String, String> other);
}
