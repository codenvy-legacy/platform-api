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

/**
 * Provides info for registration new {@code SlaveRunnerService}.
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.RunnerAdminService#registerRunnerServer(RunnerServerRegistration)
 */
@DTO
public interface RunnerServerRegistration {
    RunnerServerLocation getRunnerServerLocation();

    RunnerServerRegistration withRunnerServerLocation(RunnerServerLocation runnerServiceLocation);

    void setRunnerServerLocation(RunnerServerLocation runnerServiceLocation);

    RunnerServerAccessCriteria getRunnerServerAccessCriteria();

    RunnerServerRegistration withRunnerServerAccessCriteria(RunnerServerAccessCriteria runnerServerAccessCriteria);

    void setRunnerServerAccessCriteria(RunnerServerAccessCriteria runnerServerAccessCriteria);
}
