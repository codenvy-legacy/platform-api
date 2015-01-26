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
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface AccountMetrics {
    @ApiModelProperty(value = "Is account premium")
    Boolean isPremium();

    void setPremium(Boolean isPremium);

    AccountMetrics withPremium(Boolean isPremium);

    @ApiModelProperty(value = "Maximum memory limit for workspace in megabytes")
    Integer getMaxWorkspaceMemoryLimit();

    void setMaxWorkspaceMemoryLimit(Integer workspaceMemoryLimit);

    AccountMetrics withMaxWorkspaceMemoryLimit(Integer workspaceMemoryLimit);

    @ApiModelProperty(value = "Number of consumed Mb/min during current billing period")
    Long getUsedMemoryInCurrentBillingPeriod();

    void setUsedMemoryInCurrentBillingPeriod(Long usedMemoryInCurrentBillingPeriod);

    AccountMetrics withUsedMemoryInCurrentBillingPeriod(Long usedMemoryInCurrentBillingPeriod);

    @ApiModelProperty(value = "Metrics information for account's workspaces")
    List<WorkspaceMetrics> getWorkspaceMetrics();

    void setWorkspaceMetrics(List<WorkspaceMetrics> workspaceMetrics);

    AccountMetrics withWorkspaceMetrics(List<WorkspaceMetrics> workspaceMetrics);

    @ApiModelProperty(value = "Number of free Mb/min")
    Long getFreeMemory();

    void setFreeMemory(Long freeMemory);

    AccountMetrics withFreeMemory(Long freeMemory);

    @ApiModelProperty(value = "Date reset of consumed resources")
    Long getResourcesResetTime();

    void setResourcesResetTime(Long resourcesResetTime);

    AccountMetrics withResourcesResetTime(Long resourcesResetTime);
}
