/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
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
