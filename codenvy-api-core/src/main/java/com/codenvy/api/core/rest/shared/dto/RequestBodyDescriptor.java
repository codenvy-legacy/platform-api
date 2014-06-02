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
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes body of the request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface RequestBodyDescriptor {
    /**
     * Get optional description of request body.
     *
     * @return optional description of request body
     */
    String getDescription();

    RequestBodyDescriptor withDescription(String description);

    /**
     * Set optional description of request body.
     *
     * @param description
     *         optional description of request body
     */
    void setDescription(String description);
}
