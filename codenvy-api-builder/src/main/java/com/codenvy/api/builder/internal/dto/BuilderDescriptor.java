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

import com.codenvy.api.core.rest.dto.DtoType;

/**
 * Describes of {@link com.codenvy.api.builder.internal.Builder}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.builder.internal.Builder
 * @see com.codenvy.api.builder.internal.Builder#getName()
 * @see com.codenvy.api.builder.internal.Builder#getDescription()
 * @see com.codenvy.api.builder.internal.SlaveBuilderService#availableBuilders()
 */
@DtoType(BuilderDtoTypes.BUILDER_DESCRIPTOR_TYPE)
public class BuilderDescriptor {
    /**
     * Builder name. Each Builder type must provide unique name.
     *
     * @see com.codenvy.api.builder.internal.Builder#getName()
     */
    private String name;
    /**
     * Optional description of Builder.
     *
     * @see com.codenvy.api.builder.internal.Builder#getDescription()
     */
    private String description;

    public BuilderDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public BuilderDescriptor() {
    }

    /**
     * Get Builder name.
     *
     * @return builder name
     * @see #name
     */
    public String getName() {
        return name;
    }

    /**
     * Set Builder name.
     *
     * @param name
     *         builder name
     * @see #name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get Builder name.
     *
     * @return builder description
     * @see #description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set Builder name.
     *
     * @param description
     *         builder description
     * @see #description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "BuilderDescriptor{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
