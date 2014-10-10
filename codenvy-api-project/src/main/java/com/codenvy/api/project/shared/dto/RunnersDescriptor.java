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

package com.codenvy.api.project.shared.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
/**
 * @author andrew00x
 */
@DTO
public interface RunnersDescriptor {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "default")
    /** Gets default runner identifier. */
    String getDefault();

    /** Sets default runner identifier. */
    void setDefault(String _default);

    RunnersDescriptor withDefault(String _default);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configs")
    /** Gets all available runner configurations. */
    Map<String, RunnerConfiguration> getConfigs();

    /** Sets new runner configurations. */
    void setConfigs(Map<String, RunnerConfiguration> configs);

    RunnersDescriptor withConfigs(Map<String, RunnerConfiguration> configs);
}
