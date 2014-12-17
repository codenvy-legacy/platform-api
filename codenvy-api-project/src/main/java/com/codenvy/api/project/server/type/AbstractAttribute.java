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
package com.codenvy.api.project.server.type;

/**
 * @author gazarenkov
 */
public abstract class AbstractAttribute implements Attribute2 {

    protected String projectType;
    protected String name;
    protected String description;
    protected boolean required;
    protected boolean variable;

    protected AbstractAttribute(String projectType, String name, String description, boolean required, boolean variable) {
        this.projectType = projectType;
        this.name = name;
        this.description = description;
        this.required = required;
        this.variable = variable;
    }

    @Override
    public String getId() {
        return projectType+":"+name;
    }

    @Override
    public String getProjectType() {
        return projectType;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean isVariable() {
        return variable;
    }

    @Override
    public String getName() {
        return name;
    }
}
