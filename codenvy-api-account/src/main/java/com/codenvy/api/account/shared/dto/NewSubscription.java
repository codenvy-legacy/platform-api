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
 * Describes subscription - a link between {@link com.codenvy.api.account.server.subscription.SubscriptionService} and {@link
 * com.codenvy.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@DTO
public interface NewSubscription extends NewSubscriptionTemplate {
    Boolean getUsePaymentSystem();

    void setUsePaymentSystem(Boolean usePaymentSystem);

    NewSubscription withUsePaymentSystem(Boolean usePaymentSystem);

    String getPaymentToken();

    void setPaymentToken(String paymentToken);

    NewSubscription withPaymentToken(String paymentToken);

    NewSubscription withTrialDuration(Integer trialDuration);

    NewSubscription withPlanId(String id);

    NewSubscription withAccountId(String orgId);
}