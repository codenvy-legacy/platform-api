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
 * Provides info for registration new SlaveBuilderService.
 *
 * @author andrew00[
 * @see com.codenvy.api.builder.BuilderAdminService#register(BuilderServerRegistration)
 */
@DTO
public interface BuilderServerRegistration {
    BuilderServerLocation getBuilderServerLocation();

    BuilderServerRegistration withBuilderServerLocation(BuilderServerLocation builderServerLocation);

    void setBuilderServerLocation(BuilderServerLocation builderServerLocation);

    BuilderServerAccessCriteria getBuilderServerAccessCriteria();

    BuilderServerRegistration withBuilderServerAccessCriteria(BuilderServerAccessCriteria builderServerAccessCriteria);

    void setBuilderServerAccessCriteria(BuilderServerAccessCriteria builderServerAccessCriteria);
}
