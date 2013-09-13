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
package com.codenvy.api.builder.internal.dto;

import com.codenvy.api.builder.internal.dto.BuilderDtoTypes;
import com.codenvy.api.core.rest.dto.DtoType;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DtoType(BuilderDtoTypes.INSTANCE_STATE_TYPE)
public class InstanceState {
    private int  cpuPercentUsage;
    private long totalMemory;
    private long freeMemory;

    public InstanceState(int cpuPercentUsage, long totalMemory, long freeMemory) {
        this.cpuPercentUsage = cpuPercentUsage;
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
    }

    public InstanceState() {
    }

    public int getCpuPercentUsage() {
        return cpuPercentUsage;
    }

    public void setCpuPercentUsage(int cpuPercentUsage) {
        this.cpuPercentUsage = cpuPercentUsage;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    @Override
    public String toString() {
        return "InstanceState{" +
               "cpuPercentUsage=" + cpuPercentUsage +
               "%, totalMemory=" + totalMemory +
               "B, freeMemory=" + freeMemory +
               "B}";
    }
}
