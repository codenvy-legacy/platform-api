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
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Resource access criteria. Basically resource may be assigned to {@code workspace}, {@code project} in some workspace or {@code
 * username}.
 *
 * @author andrew00x
 */
@DTO
public interface BuilderServerAccessCriteria {
    String getWorkspace();

    BuilderServerAccessCriteria withWorkspace(String workspace);

    void setWorkspace(String workspace);

    String getProject();

    BuilderServerAccessCriteria withProject(String project);

    void setProject(String project);
}
