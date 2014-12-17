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

import com.codenvy.api.account.server.dao.Subscription.State;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Describes subscription - a link between {@link com.codenvy.api.account.server.subscription.SubscriptionService} and {@link
 * com.codenvy.api.account.server.dao.Account}
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

    State getState();

    void setState(State state);

    SubscriptionDescriptor withState(State state);

    String getStartDate();

    void setStartDate(String startDate);

    SubscriptionDescriptor withStartDate(String startDate);

    String getEndDate();

    void setEndDate(String endDate);

    SubscriptionDescriptor withEndDate(String endDate);

    String getTrialStartDate();

    void setTrialStartDate(String trialStartDate);

    SubscriptionDescriptor withTrialStartDate(String trialStartDate);

    String getTrialEndDate();

    void setTrialEndDate(String trialEndDate);

    SubscriptionDescriptor withTrialEndDate(String trialEndDate);

    Boolean getUsePaymentSystem();

    void setUsePaymentSystem(Boolean usePaymentSystem);

    SubscriptionDescriptor withUsePaymentSystem(Boolean usePaymentSystem);

    String getBillingStartDate();

    void setBillingStartDate(String billingStartDate);

    SubscriptionDescriptor withBillingStartDate(String billingStartDate);

    String getBillingEndDate();

    void setBillingEndDate(String billingEndDate);

    SubscriptionDescriptor withBillingEndDate(String billingEndDate);

    String getNextBillingDate();

    void setNextBillingDate(String nextBillingDate);

    SubscriptionDescriptor withNextBillingDate(String nextBillingDate);

    Integer getBillingCycle();

    void setBillingCycle(Integer billingCycle);

    SubscriptionDescriptor withBillingCycle(Integer billingCycle);

    CycleTypeDescriptor getBillingCycleType();

    void setBillingCycleType(CycleTypeDescriptor billingCycleType);

    SubscriptionDescriptor withBillingCycleType(CycleTypeDescriptor billingCycleType);

    Integer getBillingContractTerm();

    void setBillingContractTerm(Integer BillingContractTerm);

    SubscriptionDescriptor withBillingContractTerm(Integer BillingContractTerm);

    String getDescription();

    void setDescription(String description);

    SubscriptionDescriptor withDescription(String description);

    void setLinks(List<Link> links);

    List<Link> getLinks();

    SubscriptionDescriptor withLinks(List<Link> links);
}