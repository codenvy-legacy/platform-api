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

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface SubscriptionReference {
    String getServiceId();

    void setServiceId(String serviceId);

    SubscriptionReference withServiceId(String serviceId);

    String getSubscriptionId();

    void setSubscriptionId(String subscriptionId);

    SubscriptionReference withSubscriptionId(String subscriptionId);

    String getPlanId();

    void setPlanId(String planId);

    SubscriptionReference withPlanId(String planId);

    @ApiModelProperty(value = "Description of the subscription")
    String getDescription();

    void setDescription(String description);

    SubscriptionReference withDescription(String description);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    SubscriptionReference withLinks(List<Link> links);
}
