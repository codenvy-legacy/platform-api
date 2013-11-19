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
package com.codenvy.api.runner.internal.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes state of computer. Provided as part of state of slave builder.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface RunnerState {
    String getName();

    RunnerState withName(String name);

    void setName(String name);

    int getRunningAppsNum();

    RunnerState withRunningAppsNum(int num);

    void setRunningAppsNum(int num);

    int getTotalAppsNum();

    RunnerState withTotalAppsNum(int num);

    void setTotalAppsNum(int num);

    int getCpuPercentUsage();

    RunnerState withCpuPercentUsage(int cpuPercentUsage);

    void setCpuPercentUsage(int cpuPercentUsage);

    long getTotalMemory();

    RunnerState withTotalMemory(long totalMemory);

    void setTotalMemory(long totalMemory);

    long getFreeMemory();

    RunnerState withFreeMemory(long freeMemory);

    void setFreeMemory(long freeMemory);
}
