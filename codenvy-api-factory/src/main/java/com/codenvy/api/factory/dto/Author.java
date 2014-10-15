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

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describes author of the factory
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Author {
    /**
     * Name of the author
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "name")
    String getName();

    void setName(String name);

    Author withName(String name);

    /**
     * Email of the author
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "email")
    String getEmail();

    void setEmail(String email);

    Author withEmail(String email);

    /**
     * Identifier for the tracked factory features.
     * Replaces orgid.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "accountId")
    String getAccountId();

    void setAccountId(String accountId);

    Author withAccountId(String accountId);

    /**
     * Id of user that create factory, set by the server
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "userId", setByServer = true, format = ENCODED)
    String getUserId();

    void setUserId(String userId);

    Author withUserId(String userId);

    /**
     * @return Creation time of factory, set by the server (in milliseconds, from Unix epoch, no timezone)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "created", setByServer = true, format = ENCODED)
    Long getCreated();

    void setCreated(Long created);

    Author withCreated(Long created);
}
