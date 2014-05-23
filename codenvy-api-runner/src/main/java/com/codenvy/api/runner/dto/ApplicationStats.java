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
package com.codenvy.api.runner.dto;

import com.codenvy.api.builder.dto.BuildTaskStats;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface ApplicationStats {
    /** Get the time when application was created. */
    long getCreationTime();

    ApplicationStats withCreationTime(long created);

    void setCreationTime(long created);


    /** Get the time in milliseconds that this application is waiting for start. */
    long getWaitingTime();

    ApplicationStats withWaitingTime(long waitingTime);

    void setWaitingTime(long waitingTime);

    /** Get the limit time for application to start. If this application isn't stated before this time, it will be removed from the queue. */
    long getWaitingTimeLimit();

    ApplicationStats withWaitingTimeLimit(long timeLimit);

    void setWaitingTimeLimit(long timeLimit);


    /** Get the uptime of this task in milliseconds. */
    long getUptime();

    ApplicationStats withUptime(long uptime);

    void setUptime(long uptime);

    /** Get the termination time of the application. Zero or negative return implies the application is never stopped by anyone but user. */
    long getTerminationTime();

    ApplicationStats withTerminationTime(long terminationTime);

    void setTerminationTime(long terminationTime);


    BuildTaskStats getBuildTaskStats();

    ApplicationStats withBuildTaskStats(BuildTaskStats buildTaskStats);

    void setBuildTaskStats(BuildTaskStats buildTaskStats);


    /** Get stats of application's environment. */
    List<ApplicationEnvironmentStatsItem> getEnvironmentStats();

    ApplicationStats withEnvironmentStats(List<ApplicationEnvironmentStatsItem> envStats);

    void setEnvironmentStats(List<ApplicationEnvironmentStatsItem> envStats);
}
