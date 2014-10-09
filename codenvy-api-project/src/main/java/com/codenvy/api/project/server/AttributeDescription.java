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
package com.codenvy.api.project.server;

/**
 * Describes Attribute that may be defined for project type.
 *
 * @author andrew00x
 */
public class AttributeDescription {
    private final String name;
    private final String description;

    /**
     * Creates new AttributeDescription with specified {@code name}.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty
     */
    public AttributeDescription(String name) {
        this(name, null);
    }

    /**
     * Creates new AttributeDescription with specified {@code name} and {@code description}.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty
     */
    public AttributeDescription(String name, String description) {
        if (name == null) {
            throw new IllegalArgumentException("Null name is not allowed. ");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty. ");
        }
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "AttributeDescription{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
