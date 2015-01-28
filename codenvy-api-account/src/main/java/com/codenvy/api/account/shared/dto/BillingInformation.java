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

/**
 * @author Sergii Leschenko
 */
@DTO
public interface BillingInformation {
    String getServiceId();

    void setServiceId(String serviceId);

    BillingInformation withServiceId(String serviceId);

    String getSubscriptionId();

    void setSubscriptionId(String subscriptionId);

    BillingInformation withSubscriptionId(String subscriptionId);

    ProvidedResources getProvidedResources();

    void setProvidedResources(ProvidedResources providedResources);

    BillingInformation withProvidedResources(ProvidedResources providedResources);

    UsedResources getUsedResources();

    void setUsedResources(UsedResources usedResources);

    BillingInformation withUsedResources(UsedResources usedResources);

}
