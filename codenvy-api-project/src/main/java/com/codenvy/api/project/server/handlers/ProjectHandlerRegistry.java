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
package com.codenvy.api.project.server.handlers;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectHandlerRegistry {

    private Map<String, CreateProjectHandler> createProjectHandlers = new HashMap<>();
    private Map<String, GetItemHandler> getItemHandlers = new HashMap<>();

    @Inject
    public ProjectHandlerRegistry(Set<ProjectHandler> projectHandlers) {

        for(ProjectHandler handler : projectHandlers) {
            register(handler);
        }

    }

    public void register(ProjectHandler handler) {
        if(handler instanceof CreateProjectHandler)
            this.createProjectHandlers.put(handler.getProjectType(), (CreateProjectHandler)handler);
        else if(handler instanceof GetItemHandler)
            this.getItemHandlers.put(handler.getProjectType(), (GetItemHandler)handler);
    }


    public CreateProjectHandler getCreateProjectHandler(String projectType) {
        return createProjectHandlers.get(projectType);
    }

    public GetItemHandler getGetItemHandler(String projectType) {
        return getItemHandlers.get(projectType);
    }


}
