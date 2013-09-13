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
package com.codenvy.api.core.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one query parameter of the request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.annotations.Description
 * @see com.codenvy.api.core.rest.annotations.Required
 * @see com.codenvy.api.core.rest.annotations.Valid
 */
@DtoType(DtoTypes.PARAMETER_DESCRIPTOR_TYPE)
public class ParameterDescriptor {
    /** Name of parameter. */
    private String        name;
    /** Optional description of parameter. See {@link com.codenvy.api.core.rest.annotations.Description}. */
    private String        description;
    /** Type of parameter. See {@link ParameterType}. */
    private ParameterType type;
    /** Reports whether the parameter is mandatory. See {@link com.codenvy.api.core.rest.annotations.Required}. */
    private boolean       required;
    /**
     * List of constraint strings. This parameter is optional and may be {@code null}. See {@link
     * com.codenvy.api.core.rest.annotations.Valid}.
     */
    private List<String>  valid;

    public ParameterDescriptor(String name, String description, ParameterType type, boolean required, List<String> valid) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        if (valid != null) {
            this.valid = new ArrayList<>(valid);
        }
    }

    ParameterDescriptor(ParameterDescriptor other) {
        // Full copy of other instance. Used from Link.
        this(other.name, other.description, other.type, other.required, other.valid);
    }

    public ParameterDescriptor() {
    }

    /**
     * Get name of parameter.
     *
     * @return name of parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Set name of parameter.
     *
     * @param name
     *         name of parameter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get optional description of parameter.
     *
     * @return optional description of parameter
     * @see com.codenvy.api.core.rest.annotations.Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set optional description of parameter.
     *
     * @param description
     *         optional description of parameter
     * @see com.codenvy.api.core.rest.annotations.Description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get type of parameter.
     *
     * @return type of parameter
     * @see ParameterType
     */
    public ParameterType getType() {
        return type;
    }

    /**
     * Set type of parameter.
     *
     * @param type
     *         type of parameter
     * @see ParameterType
     */
    public void setType(ParameterType type) {
        this.type = type;
    }

    /**
     * Reports whether the parameter is mandatory.
     *
     * @return {@code true} if parameter is required and {@code false} otherwise
     * @see com.codenvy.api.core.rest.annotations.Required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required
     *         {@code true} if parameter is required and {@code false} otherwise
     * @see com.codenvy.api.core.rest.annotations.Required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Get optional list of constraint strings.
     *
     * @return optional list of constraint strings
     * @see com.codenvy.api.core.rest.annotations.Valid
     */
    public List<String> getValid() {
        if (valid == null) {
            valid = new ArrayList<>();
        }
        return valid;
    }

    /**
     * Set optional list of constraint strings.
     *
     * @param valid
     *         optional list of constraint strings
     * @see com.codenvy.api.core.rest.annotations.Valid
     */
    public void setValid(List<String> valid) {
        if (valid == null) {
            this.valid = null;
        } else {
            this.valid = new ArrayList<>(valid);
        }
    }

    @Override
    public String toString() {
        return "ParameterDescriptor{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", type=" + type +
               ", required=" + required +
               ", valid=" + valid +
               '}';
    }
}
