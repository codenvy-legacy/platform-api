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

import com.codenvy.dto.shared.DTO;

/**
 * Describes single metric of runner's stats.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerMetric {
    String getName();

    RunnerMetric withName(String name);

    void setName(String name);

    String getValue();

    RunnerMetric withValue(String value);

    void setValue(String value);

    String getDescription();

    RunnerMetric withDescription(String description);

    void setDescription(String description);
}
