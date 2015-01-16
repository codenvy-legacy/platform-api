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
package com.codenvy.api.builder.internal;

/** @author Andrey Parfonov */
public enum BuilderTaskType {
    DEFAULT("default", "Default build task"),
    COPY_DEPS("copy dependencies", "Copy all project's dependencies");

    private final String name;
    private final String description;

    private BuilderTaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "BuilderTaskType{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
