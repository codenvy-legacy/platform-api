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
 * @author Alexander Garagatyi
 */
@DTO
public interface ButtonAttributes {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "color")
    String getColor();

    void setColor(String color);

    ButtonAttributes withColor(String color);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "counter")
    boolean getCounter();

    void setCounter(boolean counter);

    ButtonAttributes withCounter(boolean counter);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "logo")
    String getLogo();

    void setLogo(String logo);

    ButtonAttributes withLogo(String logo);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "style")
    String getStyle();

    void setStyle(String style);

    ButtonAttributes withStyle(String style);
}
