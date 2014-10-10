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

import com.codenvy.dto.shared.DTO;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironment {
    /** Gets unique identifier of runner environment. */
    @Nonnull
    EnvironmentId getId();

    /** Sets unique identifier of runner environment. */
    void setId(@Nonnull EnvironmentId id);

    RunnerEnvironment withId(@Nonnull EnvironmentId id);

    /**
     * Gets runtime options of this runner environment. If {@code Map} contains mapping to empty string for some option it means that
     * environment doesn't provide any default value for this option.
     */
    @Nonnull
    Map<String, String> getOptions();

    /**
     * Sets runtime options of this runner environment.
     *
     * @see #getOptions()
     */
    void setOptions(Map<String, String> options);

    RunnerEnvironment withOptions(Map<String, String> options);

    /** Gets environment variables (runner type and(or) receipt specific). */
    /**
     * Gets environment variables of this runner environment. If {@code Map} contains mapping to empty string for some variable it means
     * that environment doesn't provide any default value for this variable.
     */
    @Nonnull
    Map<String, String> getVariables();

    /**
     * Sets environment variables of this runner environment.
     *
     * @see #getVariables()
     */
    void setVariables(Map<String, String> variables);

    RunnerEnvironment withVariables(Map<String, String> variables);
}
