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

import java.util.List;
import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author andrew00x
 */
@DTO
public interface BuilderConfiguration {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "options")
    /** Gets runtime options (runner type and(or) receipt specific). */
    Map<String, String> getOptions();

    /**
     * Sets runtime options (runner type and(or) receipt specific).
     *
     * @see #getOptions()
     */
    void setOptions(Map<String, String> options);

    BuilderConfiguration withOptions(Map<String, String> options);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "targets")
    /** Gets environment variables (runner type and(or) receipt specific). */
    List<String> getTargets();

    /**
     * Sets environment variables (runner type and(or) receipt specific).
     *
     * @see #getTargets()
     */
    void setTargets(List<String> targets);

    BuilderConfiguration withTargets(List<String> targets);
}
