/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface BuildTaskStats {
    /** Get the time when build task was created. */
    long getCreationTime();

    BuildTaskStats withCreationTime(long created);

    void setCreationTime(long created);

    /** Get the time that this task is standing in queue. */
    long getWaitingTime();

    void setWaitingTime(long waitingTime);

    BuildTaskStats withWaitingTime(long waitingTime);

    /** Get tes execution time of this task. */
    long getExecutionTime();

    BuildTaskStats withExecutionTime(long endTime);

    void setExecutionTime(long endTime);

    /** Get the time before this task must be completed. If this task isn't completed before this time, if will be terminated forcibly. */
    long getExecutionTimeLimit();

    BuildTaskStats withExecutionTimeLimit(long timeLimit);

    void setExecutionTimeLimit(long timeLimit);

    /**
     * Get the time before build process must be started. If this task isn't stated before this time, it will be removed from the build
     * queue.
     */
    long getWaitingTimeLimit();

    BuildTaskStats withWaitingTimeLimit(long timeLimit);

    void setWaitingTimeLimit(long timeLimit);
}
