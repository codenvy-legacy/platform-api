/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Location of {@code SlaveBuilderService} resource.
 *
 * @author andrew00x
 * @see com.codenvy.api.builder.internal.SlaveBuilderService
 */
@DTO
public interface BuilderServerLocation {
    /**
     * Get URL of this SlaveBuilderService. This URL may be used for direct access to the {@code SlaveBuilderService} functionality.
     *
     * @return resource URL
     */
    String getUrl();

    /**
     * Set URL of this SlaveBuilderService. This URL may be used for direct access to the {@code SlaveBuilderService} functionality.
     *
     * @param url
     *         resource URL
     */
    void setUrl(String url);

    BuilderServerLocation withUrl(String url);
}
