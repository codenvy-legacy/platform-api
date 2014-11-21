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

import java.util.List;

/**
 * Describe IDE look and feel on application closed event.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface OnAppClosed {

    /**
     * @return actions for current event.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "actions")
    List<Actions1> getActions();

    void setActions(List<Actions1> actions);

    OnAppClosed withActions(List<Actions1> actions);


    /**
     * @return parts for current event.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "parts")
    List<Part> getParts();

    void setParts(List<Part> actions);

    OnAppClosed withParts(List<Part> actions);
}
