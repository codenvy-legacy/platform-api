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
 * Describes subscription - a link between {@link com.codenvy.api.account.server.SubscriptionService} and {@link
 * com.codenvy.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@DTO
public interface NewSubscription {
    String getAccountId();

    void setAccountId(String orgId);

    NewSubscription withAccountId(String orgId);

    String getPlanId();

    void setPlanId(String id);

    NewSubscription withPlanId(String id);

    Map<String, String> getBillingProperties();

    void setBillingProperties(Map<String, String> properties);

    NewSubscription withBillingProperties(Map<String, String> properties);
}