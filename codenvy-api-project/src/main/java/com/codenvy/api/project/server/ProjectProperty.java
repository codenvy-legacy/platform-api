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

import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectProperty {
    private String       name;
    private List<String> value;

    public ProjectProperty() {
    }

    public ProjectProperty(String name, List<String> value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectProperty withName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public ProjectProperty withValue(List<String> value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "ProjectProperty{" +
               "name='" + name + '\'' +
               ", value=" + value +
               '}';
    }
}
