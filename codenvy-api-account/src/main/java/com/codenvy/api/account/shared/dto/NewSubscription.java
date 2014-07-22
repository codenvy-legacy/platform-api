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
 */
@DTO
public interface NewSubscription {
    String getAccountId();

    void setAccountId(String orgId);

    NewSubscription withAccountId(String orgId);

    String getServiceId();

    void setServiceId(String id);

    NewSubscription withServiceId(String id);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    NewSubscription withProperties(Map<String, String> properties);
}