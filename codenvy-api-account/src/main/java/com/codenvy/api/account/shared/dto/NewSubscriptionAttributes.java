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
 * Describes attributes of the subscription
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface NewSubscriptionAttributes {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
     */

    String getDescription();

    void setDescription(String description);

    NewSubscriptionAttributes withDescription(String description);

    String getStartDate();

    void setStartDate(String startDate);

    NewSubscriptionAttributes withStartDate(String startDate);

    String getEndDate();

    void setEndDate(String endDate);

    NewSubscriptionAttributes withEndDate(String endDate);

    Integer getTrialDuration();

    void setTrialDuration(Integer trialDuration);

    NewSubscriptionAttributes withTrialDuration(Integer trialDuration);

    NewBilling getBilling();

    void setBilling(NewBilling billing);

    NewSubscriptionAttributes withBilling(NewBilling billing);

    Map<String, String> getCustom();

    void setCustom(Map<String, String> other);

    NewSubscriptionAttributes withCustom(Map<String, String> other);
}
