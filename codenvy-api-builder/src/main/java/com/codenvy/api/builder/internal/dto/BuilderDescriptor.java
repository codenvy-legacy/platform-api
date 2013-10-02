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
package com.codenvy.api.builder.internal.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes of {@link com.codenvy.api.builder.internal.Builder}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
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
}
