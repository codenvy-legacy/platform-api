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

import java.util.Map;

/**
 * Describes of {@link com.codenvy.api.builder.internal.Builder}.
 *
 * @author andrew00x
 * @see com.codenvy.api.builder.internal.Builder
 * @see com.codenvy.api.builder.internal.Builder#getName()
 * @see com.codenvy.api.builder.internal.Builder#getDescription()
 * @see com.codenvy.api.builder.internal.SlaveBuilderService#availableBuilders()
 */
@DTO
public interface BuilderDescriptor {

    /**
     * Get Builder name.
     *
     * @return builder name
     */
    String getName();

    /**
     * Set Builder name.
     *
     * @param name
     *         builder name
     */
    void setName(String name);

    BuilderDescriptor withName(String name);

    /**
     * Get optional description of Builder.
     *
     * @return builder description
     */
    String getDescription();

    /**
     * Set optional description of Builder.
     *
     * @param description
     *         builder description
     */
    void setDescription(String description);

    BuilderDescriptor withDescription(String description);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    BuilderDescriptor withProperties(Map<String, String> properties);
}
