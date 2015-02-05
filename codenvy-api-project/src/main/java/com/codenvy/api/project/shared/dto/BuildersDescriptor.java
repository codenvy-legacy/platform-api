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
package com.codenvy.api.project.shared.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
/**
 * @author andrew00x
 */
@DTO
public interface BuildersDescriptor {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "default")
    /** Gets default builder identifier, e.g. "maven". */
    String getDefault();

    /** Sets default builder identifier. e.g. "maven". */
    void setDefault(String _default);

    BuildersDescriptor withDefault(String _default);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configs")
    /** Gets all available runner configurations. */
    Map<String, BuilderConfiguration> getConfigs();

    /** Sets new runner configurations. */
    void setConfigs(Map<String, BuilderConfiguration> configs);

    BuildersDescriptor withConfigs(Map<String, BuilderConfiguration> configs);
}
