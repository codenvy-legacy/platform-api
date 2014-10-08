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
 * Represents tariff plan
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Plan {
    String getId();

    void setId(String id);

    Plan withId(String id);

    String getServiceId();

    void setServiceId(String serviceId);

    Plan withServiceId(String serviceId);

    boolean isPaid();

    void setPaid(boolean paid);

    Plan withPaid(boolean paid);

    boolean getSalesOnly();

    void setSalesOnly(boolean salesOnly);

    Plan withSalesOnly(boolean salesOnly);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Plan withProperties(Map<String, String> properties);
}
