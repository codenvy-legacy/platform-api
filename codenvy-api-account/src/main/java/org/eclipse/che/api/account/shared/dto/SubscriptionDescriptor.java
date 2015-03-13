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
package org.eclipse.che.api.account.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Describes subscription - a link between {@link org.eclipse.che.api.account.server.SubscriptionService} and {@link
 * org.eclipse.che.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionDescriptor {
    @ApiModelProperty(value = "Unique subscription ID")
    String getId();

    void setId(String id);

    SubscriptionDescriptor withId(String id);

    @ApiModelProperty(value = "Account ID")
    String getAccountId();

    void setAccountId(String orgId);

    SubscriptionDescriptor withAccountId(String orgId);

    @ApiModelProperty(value = "Service ID")
    String getServiceId();

    void setServiceId(String id);

    SubscriptionDescriptor withServiceId(String id);

    @ApiModelProperty(value = "Plan ID that includes service, duration, package and RAM amount")
    String getPlanId();

    void setPlanId(String planId);

    SubscriptionDescriptor withPlanId(String planId);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    SubscriptionDescriptor withProperties(Map<String, String> properties);

    void setLinks(List<Link> links);

    List<Link> getLinks();

    SubscriptionDescriptor withLinks(List<Link> links);
}