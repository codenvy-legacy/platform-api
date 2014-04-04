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
 * Location of {@code SlaveRunnerService} resource.
 *
 * @author andrew00x
 * @see com.codenvy.api.runner.internal.SlaveRunnerService
 */
@DTO
public interface RunnerServerLocation {
    /**
     * Get URL of this SlaveRunnerService. This URL may be used for direct access to the {@code SlaveRunnerService} functionality.
     *
     * @return resource URL
     */
    String getUrl();

    /**
     * Set URL of this SlaveRunnerService. This URL may be used for direct access to the {@code SlaveRunnerService} functionality.
     *
     * @param url
     *         resource URL
     */
    void setUrl(String url);

    RunnerServerLocation withUrl(String url);
}
