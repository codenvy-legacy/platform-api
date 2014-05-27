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

import java.util.List;

/**
 * Describes current state of {@link com.codenvy.api.runner.internal.Runner}.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerState {
    String getName();

    RunnerState withName(String name);

    void setName(String name);

    List<RunnerMetric> getDetails();

    RunnerState withDetails(List<RunnerMetric> details);

    void setDetails(List<RunnerMetric> details);

    ServerState getServerState();

    RunnerState withServerState(ServerState serverState);

    void setServerState(ServerState serverState);
}
