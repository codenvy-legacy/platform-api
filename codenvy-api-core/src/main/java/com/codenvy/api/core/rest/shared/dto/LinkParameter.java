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
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.api.core.rest.shared.ParameterType;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * Describes one query parameter of the request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.annotations.Description
 * @see com.codenvy.api.core.rest.annotations.Required
 * @see com.codenvy.api.core.rest.annotations.Valid
 */
@DTO
public interface LinkParameter {
    /**
     * Get name of parameter.
     *
     * @return name of parameter
     */
    String getName();

    /**
     * Set name of parameter.
     *
     * @param name
     *         name of parameter
     */
    void setName(String name);

    /**
     * Get defaultValue of parameter.
     *
     * @return defaultValue of parameter
     */
    String getDefaultValue();

    /**
     * Set defaultValue of parameter.
     *
     * @param defaultValue
     *         defaultValue of parameter
     */
    void setDefaultValue(String defaultValue);

    /**
     * Get optional description of parameter.
     *
     * @return optional description of parameter
     * @see com.codenvy.api.core.rest.annotations.Description
     */
    String getDescription();

    /**
     * Set optional description of parameter.
     *
     * @param description
     *         optional description of parameter
     * @see com.codenvy.api.core.rest.annotations.Description
     */
    void setDescription(String description);

    /**
     * Get type of parameter.
     *
     * @return type of parameter
     * @see com.codenvy.api.core.rest.shared.ParameterType
     */
    ParameterType getType();

    /**
     * Set type of parameter.
     *
     * @param type
     *         type of parameter
     * @see ParameterType
     */
    void setType(ParameterType type);

    /**
     * Reports whether the parameter is mandatory.
     *
     * @return {@code true} if parameter is required and {@code false} otherwise
     * @see com.codenvy.api.core.rest.annotations.Required
     */
    boolean isRequired();

    /**
     * @param required
     *         {@code true} if parameter is required and {@code false} otherwise
     * @see com.codenvy.api.core.rest.annotations.Required
     */
    void setRequired(boolean required);

    /**
     * Get optional list of constraint strings.
     *
     * @return optional list of constraint strings
     * @see com.codenvy.api.core.rest.annotations.Valid
     */
    List<String> getValid();

    /**
     * Set optional list of constraint strings.
     *
     * @param valid
     *         optional list of constraint strings
     * @see com.codenvy.api.core.rest.annotations.Valid
     */
    void setValid(List<String> valid);
}
