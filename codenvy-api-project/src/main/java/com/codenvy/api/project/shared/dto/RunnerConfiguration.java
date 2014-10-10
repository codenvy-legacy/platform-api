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
public interface RunnerConfiguration {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "ram")
    /** Gets amount of RAM for this configuration in megabytes. */
    int getRam();

    /** Sets amount of RAM for this configuration in megabytes. */
    void setRam(int ram);

    RunnerConfiguration withRam(int ram);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "options")
    /** Gets runtime options (runner type and(or) receipt specific). */
    Map<String, String> getOptions();

    /**
     * Sets runtime options (runner type and(or) receipt specific).
     *
     * @see #getOptions()
     */
    void setOptions(Map<String, String> options);

    RunnerConfiguration withOptions(Map<String, String> options);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "variables")
    /** Gets environment variables (runner type and(or) receipt specific). */
    Map<String, String> getVariables();

    /**
     * Sets environment variables (runner type and(or) receipt specific).
     *
     * @see #getVariables()
     */
    void setVariables(Map<String, String> variables);

    RunnerConfiguration withVariables(Map<String, String> variables);
}
