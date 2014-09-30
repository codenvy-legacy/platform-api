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
package com.codenvy.api.factory.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
/**
 * Describe restrictions of the factory
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface Policies {
    /**
     * Restrict access if referer header doesn't match this field
     */
    // Do not change referer to referrer
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "refererHostname")
    String getRefererHostname();

    void setRefererHostname(String refererHostname);

    Policies withRefererHostname(String refererHostname);

    /**
     * Restrict access for factories used earlier then author supposes
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validSince")
    long getValidSince();

    void setValidSince(long validSince);

    Policies withValidSince(long validSince);

    /**
     * Restrict access for factories used later then author supposes
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "validUntil")
    long getValidUntil();

    void setValidUntil(long validUntil);

    Policies withValidUntil(long validUntil);
}
