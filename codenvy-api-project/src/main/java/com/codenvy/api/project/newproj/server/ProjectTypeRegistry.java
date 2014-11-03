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
import com.codenvy.api.project.newproj.ProjectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectTypeRegistry {

    public static final ProjectType BASE_TYPE = new BaseProjectType();

    private final Map<String, ProjectType> projectTypes = new HashMap<>();

    private Set <String> allIds = new HashSet<>();

    @Inject
    public ProjectTypeRegistry(Set<ProjectType> projTypes) {

        for(ProjectType pt : projTypes) {
            allIds.add(pt.getId());
        }

        for(ProjectType pt : projTypes) {

            if(init(pt))
               this.projectTypes.put(pt.getId(), pt);
        }

    }

    public ProjectType getProjectType(String id) {
        return projectTypes.get(id);
    }


    private boolean init(ProjectType pt) {

        if(pt.getId() == null || pt.getId().isEmpty()) {
            return false;
        }
            //throw new NullPointerException("Project Type ID is null or empty for "+pt.getClass().getName());

        // TODO check constraints on ID spelling (no spaces, only alphanumeric)?


        if(pt.getDisplayName() == null || pt.getDisplayName().isEmpty()) {
            return false;
        }

            //throw new NullPointerException("Project Type Display Name is null or empty for "+pt.getClass().getName());




        for(Attribute attr : pt.getAttributes()) {

            // TODO check attribute name spelling (no spaces, only alphanumeric)?
            // make attributes with FQ ID: projectTypeId:attributeId
            //this.attributes.add(attr);
            //init

        }



        // add Base Type as a parent if not pointed
        if(pt.getParents().isEmpty() && !pt.getId().equals(BASE_TYPE.getId()))
            pt.getParents().add(BASE_TYPE);


        // look for parents
        for(ProjectType parent : pt.getParents()) {
            if(!allIds.contains(parent.getId())) {
                return false;
            }
        }

        initAttributesRecursively(pt.getAttributes(), pt);

        // Builders and Runners


        return true;

    }


    private void initAttributesRecursively(List <Attribute> attributes, ProjectType type) {

        for(ProjectType supertype : type.getParents()) {
            attributes.addAll(supertype.getAttributes());
            initAttributesRecursively(attributes, supertype);
        }

    }


}
