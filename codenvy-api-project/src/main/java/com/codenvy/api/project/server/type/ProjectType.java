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

import java.util.*;


/**
 * @author gazarenkov
 */
public abstract class ProjectType {


    private final   String                  id;
    private final   String                  displayName;
    private final   Map<String, Attribute> attributes;
    private final   List<ProjectType>      parents;
    protected final List<String>            runnerCategories;
    protected final List<String>            builderCategories;
    private String defaultBuilder = null;
    private String defaultRunner  = null;
    private final boolean mixable;
    private final boolean primaryable;
//    private final Set<ValueProviderFactory> providedFactories;

    /**
     *
     * @param id
     * @param displayName
     * @param primaryable - whether the ProjectType can be used as Primary
     * @param mixable - whether the projectType can be used as Mixin
     */
    protected ProjectType(String id, String displayName, boolean primaryable, boolean mixable) {
        this.id = id;
        this.displayName = displayName;
        this.attributes = new HashMap<>();
        this.parents = new ArrayList<ProjectType>();
        this.runnerCategories = new ArrayList<String>();
        this.builderCategories = new ArrayList<String>();
        this.mixable = mixable;
        this.primaryable = primaryable;
//        providedFactories = new HashSet<>();
    }

//    /**
//     *
//     * @param id
//     * @param displayName
//     * @deprecated
//     */
//    protected ProjectType(String id, String displayName) {
//        this(id, displayName, true, true);
//    }


    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Attribute> getAttributes() {
        return new ArrayList<>(attributes.values());
    }

    public List<ProjectType> getParents() {
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

    public Attribute getAttribute(String name) {
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

//    public Set<ValueProviderFactory> getProvidedFactories() {
//        return providedFactories;
//    }

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
//        this.providedFactories.add(factory);
    }

    protected void addAttributeDefinition(Attribute attr) {
        attributes.put(attr.getName(), attr);
    }

    protected void addParent(ProjectType parent) {
        parents.add(parent);
    }

    protected void setDefaultBuilder(String builder) {
        this.defaultBuilder = builder;
    }

    protected void setDefaultRunner(String runner) {
        this.defaultRunner = runner;
    }

    protected void addRunnerCategories(List<String> categories) {
        this.runnerCategories.addAll(categories);
    }

    private boolean recurseParents(ProjectType child, String parent) {

        for (ProjectType p : child.getParents()) {
            if(p.getId().equals(parent)) {
                return true;
            }
            if(recurseParents(p, parent))
                return true;
        }

        return false;

    }

}

