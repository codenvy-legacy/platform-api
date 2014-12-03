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
package com.codenvy.api.project.newproj.server;

import com.codenvy.api.project.newproj.Attribute2;
import com.codenvy.api.project.newproj.ProjectType2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */
public abstract class AbstractProjectType implements ProjectType2 {


    protected final String id;
    protected final String displayName;
    protected final List<Attribute2> attributes;
    protected final List<ProjectType2> parents;
    protected final List<String> runnerCategories;
    protected final List<String> builderCategories;

    protected AbstractProjectType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.attributes = new ArrayList<Attribute2>();
        this.parents = new ArrayList<ProjectType2>();
        this.runnerCategories = new ArrayList<String>();
        this.builderCategories = new ArrayList<String>();

    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public List<Attribute2> getAttributes() {
        return attributes;
    }

    @Override
    public List<ProjectType2> getParents() {
        return parents;
    }

    @Override
    public boolean isTypeOf(String typeId) {
        if(this.id.equals(typeId))
            return true;
        for(ProjectType2 type : getParents()) {
            if (type.getId().equals(typeId))
                return true;
        }
        return false;
    }

    @Override
    public Attribute2 getAttribute(String name) {
        for(Attribute2 attr : attributes) {
            if(attr.getName().equals(name))
                return attr;
        }

        return null;
    }

    @Override
    public List<String> getRunnerCategories() {
        return runnerCategories;
    }
}

