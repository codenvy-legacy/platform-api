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
package com.codenvy.api.project.newproj;

import java.util.List;

/**
 * @author gazarenkov
 */
public class ProjectDescriptor {

    //private String name;
    private String description;
    private String typeId;
    private List<Attribute> attributes;

    public ProjectDescriptor(String description, String typeId, List<Attribute> attributes) {

        //this.name = name;
        this.description = description;
        this.typeId = typeId;
        this.attributes = attributes;
    }

//    public String getName() {
//        return name;
//    }

    public String getDescription() {
        return description;
    }

    public String getTypeId() {
        return typeId;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }


    //    ProjectRunnerConfig getRunnerConfig();
}
