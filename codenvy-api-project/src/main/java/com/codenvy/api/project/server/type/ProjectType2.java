/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author gazarenkov
 */
public abstract class ProjectType2 {


    private final   String                  id;
    private final   String                  displayName;
    private final   Map<String, Attribute2> attributes;
    private final   List<ProjectType2>      parents;
    protected final List<String>            runnerCategories;
    protected final List<String>            builderCategories;
    private String defaultBuilder = null;
    private String defaultRunner  = null;
    private final boolean mixable;
    private final boolean primaryable;

    /**
     *
     * @param id
     * @param displayName
     * @param primaryable
     * @param mixable
     */
    protected ProjectType2(String id, String displayName, boolean primaryable, boolean mixable) {
        this.id = id;
        this.displayName = displayName;
        this.attributes = new HashMap<>();
        this.parents = new ArrayList<ProjectType2>();
        this.runnerCategories = new ArrayList<String>();
        this.builderCategories = new ArrayList<String>();
        this.mixable = mixable;
        this.primaryable = primaryable;
    }

    /**
     * @deprecated
     * @param id
     * @param displayName
     */
    protected ProjectType2(String id, String displayName) {
        this(id, displayName, true, true);
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
        if (this.id.equals(typeId))
            return true;

        return recurseParents(this, typeId);

    }

    public String getDefaultBuilder() {

        return defaultBuilder;
    }

    public String getDefaultRunner() {

        return defaultRunner;
    }

    public Attribute2 getAttribute(String name) {
        return attributes.get(name);
    }


    public List<String> getRunnerCategories() {
        return runnerCategories;
    }

    public boolean canBeMixin() {
        return mixable;
    }

    public boolean canBePrimary() {
        return primaryable;
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

    protected void setDefaultBuilder(String builder) {
        this.defaultBuilder = builder;
    }

    protected void setDefaultRunner(String runner) {
        this.defaultRunner = runner;
    }

    private boolean recurseParents(ProjectType2 child, String parent) {

        for (ProjectType2 p : child.getParents()) {
            if(p.getId().equals(parent)) {
                return true;
            }
            if(recurseParents(p, parent))
                return true;
        }

        return false;

    }

}

