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
 * Provides info for registration new SlaveRunnerService.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.runner.RunnerAdminService#register(com.codenvy.api.runner.dto.RunnerServiceRegistration)
 */
@DTO
public interface RunnerServiceRegistration {
    RunnerServiceLocation getRunnerServiceLocation();

    RunnerServiceRegistration withRunnerServiceLocation(RunnerServiceLocation runnerServiceLocation);

    void setRunnerServiceLocation(RunnerServiceLocation runnerServiceLocation);

    RunnerServiceAccessCriteria getRunnerServiceAccessCriteria();

    RunnerServiceRegistration withRunnerServiceAccessCriteria(RunnerServiceAccessCriteria runnerServiceAccessCriteria);

    void setRunnerServiceAccessCriteria(RunnerServiceAccessCriteria runnerServiceAccessCriteria);
}
