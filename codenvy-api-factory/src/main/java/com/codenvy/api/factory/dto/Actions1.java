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

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Describe ide action.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface Actions1 {
    /**
     * Action Id
     *
     * @return id of action.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "id")
    String getId();

    void setId(String id);

    Actions1 withId(String id);

    /***
     *
     * @return Action properties
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "properties")
    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Actions1 withProperties(Map<String, String> properties);
}
