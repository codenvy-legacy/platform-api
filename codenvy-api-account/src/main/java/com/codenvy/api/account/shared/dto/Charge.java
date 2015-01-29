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
 * @author Sergii Kabashniuk
 */
@DTO
public interface Charge {
    String getServiceId();

    void setServiceId(String serviceId);

    Charge withServiceId(String serviceId);


    String getType();

    void setType(String type);

    Charge withType(String type);


    Double getAmount();

    void setAmount(Double amount);

    Charge withAmount(Double amount);


    Double getPrice();

    void setPrice(Double usedPrice);

    Charge withPrice(Double usedPrice);


}
