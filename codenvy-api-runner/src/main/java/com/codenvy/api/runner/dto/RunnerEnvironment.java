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
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironment {
    /**
     * Get unique id of RunnerEnvironment.
     *
     * @return unique id of RunnerEnvironment
     */
    String getId();

    void setId(String id);

    RunnerEnvironment withId(String id);

    /**
     * Get description of RunnerEnvironment.
     *
     * @return description of RunnerEnvironment
     */
    String getDescription();

    void setDescription(String description);

    RunnerEnvironment withDescription(String description);

    boolean getIsDefault();

    void setIsDefault(boolean isDefault);

    RunnerEnvironment withIsDefault(boolean isDefault);
}
