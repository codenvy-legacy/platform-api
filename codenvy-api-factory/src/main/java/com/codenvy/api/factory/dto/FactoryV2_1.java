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

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.BOTH;
import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.dto.shared.DTO;

/**
 * Factory of version 2.0
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV2_1 extends FactoryV2_0 {
    /**
     * Describes ide look and feel.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "ide", format = BOTH)
    Ide getIde();

    void setIde(Ide button);

    FactoryV2_1 withIde(Ide button);

}
