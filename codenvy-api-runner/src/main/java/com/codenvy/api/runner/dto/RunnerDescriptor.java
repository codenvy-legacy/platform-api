/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
