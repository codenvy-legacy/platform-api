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
import java.util.regex.Pattern;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTypeRegistry.class);

    public static final ProjectType2 BASE_TYPE = new BaseProjectType();

    private final static Pattern NAME_PATTERN = Pattern.compile("[^a-zA-Z0-9-_]");

    private final Map<String, ProjectType2> projectTypes = new HashMap<>();

    private Set <String> allIds = new HashSet<>();

    @Inject
    public ProjectTypeRegistry(Set<ProjectType2> projTypes) {

        if(!projTypes.contains(BASE_TYPE)) {
            allIds.add(BASE_TYPE.getId());
            projectTypes.put(BASE_TYPE.getId(), BASE_TYPE);
        }

        for(ProjectType2 pt : projTypes) {
            if(pt.getId() != null && !pt.getId().isEmpty()) {
                allIds.add(pt.getId());

                // add Base Type as a parent if not pointed
                if (pt.getParents() != null && pt.getParents().isEmpty() && !pt.getId().equals(BASE_TYPE.getId())) {
                    LOG.debug("BASE added as parent of: " + pt.getId());
                    pt.getParents().add(BASE_TYPE);
                }
            }
        }

        for(ProjectType2 pt : projTypes) {
            if(pt.getId() != null && !pt.getId().isEmpty()) {
                try {
                    init(pt);
                    this.projectTypes.put(pt.getId(), pt);
                } catch (ProjectTypeConstraintException e) {
                    LOG.error(e.getMessage());
                }
            }
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

        init(projectType);
        this.projectTypes.put(projectType.getId(), projectType);

    }


    private void init(ProjectType2 pt) throws ProjectTypeConstraintException {

        if(pt.getId() == null || pt.getId().isEmpty()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with null or empty ID: " + pt.getClass().getName());
        }

        // ID spelling (no spaces, only alphanumeric)
        if(NAME_PATTERN.matcher(pt.getId()).find()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with invalid ID (only Alphanumeric, dash and underscore allowed): " +pt.getClass().getName()+ " ID: '"+pt.getId()+"'");
        }

        if(pt.getDisplayName() == null || pt.getDisplayName().isEmpty()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with null or empty display name: " +pt.getId());
        }

        for (Attribute2 attr : pt.getAttributes()) {

            // ID spelling (no spaces, only alphanumeric)
            if(NAME_PATTERN.matcher(attr.getName()).find()) {
                throw new ProjectTypeConstraintException("Could not register Project Type with invalid attribute Name (only Alphanumeric, dash and underscore allowed): " +attr.getClass().getName()+ " ID: '"+attr.getId()+"'");
            }
        }

        // look for parents
        for(ProjectType2 parent : pt.getParents()) {
            if(!allIds.contains(parent.getId())) {
                throw new ProjectTypeConstraintException("Could not register Project Type: "+pt.getId()+" : Unregistered parent Type: "+parent.getId());
            }
        }

        initAttributesRecursively(pt, pt);

        // Builders and Runners

    }


    private void initAttributesRecursively(ProjectType2 myType, ProjectType2 type)
            throws ProjectTypeConstraintException {

        for(ProjectType2 supertype : type.getParents()) {

            for(Attribute2 attr : supertype.getAttributes()) {

                // check attribute names
                for(Attribute2 attr2 : myType.getAttributes()) {
                    if(attr.getName().equals(attr2.getName()) && !attr.getProjectType().equals(attr2.getProjectType())) {
                        throw new ProjectTypeConstraintException("Attribute name conflict. Project type "+
                                myType.getId() + " could not be registered as attribute declaration "+ attr.getName()+
                                " is duplicated in its ancestor(s).");
                    }
                }
                ((AbstractProjectType)myType).addAttributeDefinition(attr);
            }
            initAttributesRecursively(myType, supertype);
        }

    }


}
