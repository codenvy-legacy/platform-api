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
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Template for NerSubscription
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface NewSubscriptionTemplate {

    @ApiModelProperty(value = "Account ID")
    String getAccountId();

    void setAccountId(String orgId);

    NewSubscriptionTemplate withAccountId(String orgId);

    @ApiModelProperty(value = "Plan ID")
    String getPlanId();

    void setPlanId(String id);

    NewSubscriptionTemplate withPlanId(String id);
}