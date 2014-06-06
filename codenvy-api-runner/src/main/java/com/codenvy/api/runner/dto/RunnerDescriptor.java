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
package com.codenvy.api.runner.dto;

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Describes capabilities of {@link com.codenvy.api.runner.internal.Runner}.
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.internal.Runner
 * @see com.codenvy.api.runner.internal.Runner#getName()
 * @see com.codenvy.api.runner.internal.SlaveRunnerService#availableRunners()
 */
@DTO
public interface RunnerDescriptor {
    /**
     * Get Runner name.
     *
     * @return runner name
     */
    String getName();

    /**
     * Set Runner name.
     *
     * @param name
     *         runner name
     */
    void setName(String name);

    RunnerDescriptor withName(String name);

    /**
     * Get optional description of Runner.
     *
     * @return runner description
     */
    String getDescription();

    /**
     * Set optional description of Runner.
     *
     * @param description
     *         runner description
     */
    void setDescription(String description);

    RunnerDescriptor withDescription(String description);

    Map<String, RunnerEnvironment> getEnvironments();

    void setEnvironments(Map<String, RunnerEnvironment> environments);

    RunnerDescriptor withEnvironments(Map<String, RunnerEnvironment> environments);
}
