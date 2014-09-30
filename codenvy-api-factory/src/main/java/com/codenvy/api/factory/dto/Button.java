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

import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describes factory button
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Button {
    public enum ButtonType {
        logo, nologo
    }

    /** Type of the button */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "type")
    ButtonType getType();

    void setType(ButtonType type);

    Button withType(ButtonType type);

    /** Button attributes */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "attributes")
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    Button withAttributes(Map<String, String> attributes);
}
