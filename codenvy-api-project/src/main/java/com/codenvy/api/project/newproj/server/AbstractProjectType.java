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

import com.codenvy.api.project.newproj.Attribute;
import com.codenvy.api.project.newproj.AttributeValue;
import com.codenvy.api.project.newproj.ProjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */
public abstract class AbstractProjectType implements ProjectType {


    protected final String id;
    protected final String displayName;
    protected final List<Attribute> attributes;
    protected final List<ProjectType> parents;
    protected final List<String> runnerCategories;
    protected final List<String> builderCategories;

    protected AbstractProjectType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.attributes = new ArrayList<Attribute>();
        this.parents = new ArrayList<ProjectType>();
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
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public List<ProjectType> getParents() {
        return parents;
    }

    @Override
    public boolean isTypeOf(String typeId) {
        if(this.id.equals(typeId))
            return true;
        for(ProjectType type : getParents()) {
            if (type.getId().equals(typeId))
                return true;
        }
        return false;
    }

    @Override
    public Attribute getAttribute(String id) {
        for(Attribute attr : attributes) {
            if(attr.getId().equals(id))
                return attr;
        }

        return null;
    }
}

