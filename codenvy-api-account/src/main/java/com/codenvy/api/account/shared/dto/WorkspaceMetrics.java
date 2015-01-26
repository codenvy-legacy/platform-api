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

/**
 * @author Sergii Leschenko
 */
@DTO
public interface WorkspaceMetrics {
    @ApiModelProperty(value = "Workspace ID")
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    WorkspaceMetrics withWorkspaceId(String workspaceId);

    @ApiModelProperty(value = "Number of consumed Mb/min during current billing period")
    Long getUsedMemoryInCurrentBillingPeriod();

    void setUsedMemoryInCurrentBillingPeriod(Long usedMemoryInCurrentBillingPeriod);

    WorkspaceMetrics withUsedMemoryInCurrentBillingPeriod(Long usedMemoryInCurrentBillingPeriod);

    @ApiModelProperty(value = "Memory megabytes which can be used in workspace at one time")
    Integer getWorkspaceMemoryLimit();

    void setWorkspaceMemoryLimit(Integer workspaceMemoryLimit);

    WorkspaceMetrics withWorkspaceMemoryLimit(Integer workspaceMemoryLimit);
}
