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
