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

import com.codenvy.api.project.server.ValueProviderFactory;

import java.util.*;

/**
 * @author gazarenkov
 */
public abstract class ProjectType2 {


    private final String id;
    private final String displayName;
    private final Map<String, Attribute2> attributes;
    private final List<ProjectType2> parents;
    protected final List<String> runnerCategories;
    protected final List<String> builderCategories;

    protected ProjectType2(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.attributes = new HashMap<>();
        this.parents = new ArrayList<ProjectType2>();
        this.runnerCategories = new ArrayList<String>();
        this.builderCategories = new ArrayList<String>();

    }


    public String getId() {
        return id;
    }


    public String getDisplayName() {
        return displayName;
    }

    public List<Attribute2> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    public List<ProjectType2> getParents() {
        return parents;
    }

    public boolean isTypeOf(String typeId) {
        if(this.id.equals(typeId))
            return true;
        for(ProjectType2 type : getParents()) {
            if (type.getId().equals(typeId))
                return true;
        }
        return false;
    }

    public Attribute2 getAttribute(String name) {
        return attributes.get(name);
    }


    public List<String> getRunnerCategories() {
        return runnerCategories;
    }


    protected void addConstantDefinition(String name, String description, AttributeValue value) {
        attributes.put(name, new Constant(id, name, description, value));
    }

    protected void addConstantDefinition(String name, String description, String value) {
        attributes.put(name, new Constant(id, name, description, value));
    }

    protected void addVariableDefinition(String name, String description, boolean required) {
        attributes.put(name, new Variable(id, name, description, required));
    }

    protected void addVariableDefinition(String name, String description, boolean required, AttributeValue value) {
        attributes.put(name, new Variable(id, name, description, required, value));
    }

    protected void addVariableDefinition(String name, String description, boolean required, ValueProviderFactory factory) {
        attributes.put(name, new Variable(id, name, description, required, factory));
    }

    protected void addAttributeDefinition(Attribute2 attr) {
        attributes.put(attr.getName(), attr);
    }

    protected void addParent(ProjectType2 parent) {
        parents.add(parent);
    }

}

