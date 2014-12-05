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
import com.codenvy.api.project.server.ProjectTypeConstraintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTypeRegistry.class);

    public static final ProjectType2 BASE_TYPE = new BaseProjectType();

    private final Map<String, ProjectType2> projectTypes = new HashMap<>();

    private Set <String> allIds = new HashSet<>();

    @Inject
    public ProjectTypeRegistry(Set<ProjectType2> projTypes) {

        if(!projTypes.contains(BASE_TYPE)) {
            allIds.add(BASE_TYPE.getId());
            projectTypes.put(BASE_TYPE.getId(), BASE_TYPE);
        }

        for(ProjectType2 pt : projTypes) {
            allIds.add(pt.getId());
        }

        for(ProjectType2 pt : projTypes) {

            if(init(pt))
               this.projectTypes.put(pt.getId(), pt);
        }

    }

    public ProjectType2 getProjectType(String id) {
        return projectTypes.get(id);
    }

    public Collection <ProjectType2> getProjectTypes() {
        return projectTypes.values();
    }

    // maybe for test only?
    public void registerProjectType(ProjectType2 projectType) throws ProjectTypeConstraintException {

        if(init(projectType))
            this.projectTypes.put(projectType.getId(), projectType);
        else
            throw new ProjectTypeConstraintException("Could not register project type: "+ projectType.getId());

    }


    private boolean init(ProjectType2 pt) {

        if(pt.getId() == null || pt.getId().isEmpty()) {
            return false;
        }
            //throw new NullPointerException("Project Type ID is null or empty for "+pt.getClass().getName());

        // TODO check constraints on ID spelling (no spaces, only alphanumeric)?


        if(pt.getDisplayName() == null || pt.getDisplayName().isEmpty()) {
            return false;
        }

            //throw new NullPointerException("Project Type Display Name is null or empty for "+pt.getClass().getName());


        for (Attribute2 attr : pt.getAttributes()) {

            // TODO check attribute name spelling (no spaces, only alphanumeric)?
            // make attributes with FQ ID: projectTypeId:attributeId
            //this.attributes.add(attr);
            //init
//            if(attr.isVariable()) {
//                //System.out.println("INIT >>>>> " + attr.getId() + " = "+((Variable)attr).valueProviderFactory);
//            }

        }



        // add Base Type as a parent if not pointed
        if(pt.getParents() !=null && pt.getParents().isEmpty() && !pt.getId().equals(BASE_TYPE.getId()))
            pt.getParents().add(BASE_TYPE);





        // look for parents
        for(ProjectType2 parent : pt.getParents()) {
            if(!allIds.contains(parent.getId())) {
                return false;
            }
        }

        try {
            initAttributesRecursively(pt.getAttributes(), pt);
        } catch (ProjectTypeConstraintException e) {
            LOG.error(e.getMessage());
        }

        // Builders and Runners


        return true;

    }


    private void initAttributesRecursively(List <Attribute2> attributes, ProjectType2 type)
            throws ProjectTypeConstraintException {

        for(ProjectType2 supertype : type.getParents()) {

            for(Attribute2 attr : supertype.getAttributes()) {
                for(Attribute2 attr2 : attributes) {
                    if(attr.getName().equals(attr2.getName())) {
                        throw new ProjectTypeConstraintException("Attribute name conflict. Attribute "+
                                attr.getName()+ " can not be used in the project type "+attr.getProjectType()+
                                " as it's already defined in supertype "+attr2.getProjectType());
                    }
                }
                attributes.add(attr);
            }
            //attributes.addAll(supertype.getAttributes());
            initAttributesRecursively(attributes, supertype);
        }

    }


}
