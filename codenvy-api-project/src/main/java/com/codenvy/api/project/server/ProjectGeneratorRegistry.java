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
package com.codenvy.api.project.server;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry for project generators.
 *
 * @author andrew00x
 * @author Artem Zatsarynnyy
 *
 * @deprecated use ProjectHandlerRegistry instead
 */
@Singleton
public class ProjectGeneratorRegistry {
    private final Map<String, ProjectGenerator> generators;

    @Inject
    public ProjectGeneratorRegistry(Set<ProjectGenerator> generators) {
        this.generators = new ConcurrentHashMap<>();
        for (ProjectGenerator generator : generators) {
            this.generators.put(generator.getProjectTypeId(), generator);
        }
    }

    // for tests only?
    public void register(ProjectGenerator generator) {

        this.generators.put(generator.getProjectTypeId(), generator);

    }

    public ProjectGenerator getGenerator(String projectTypeId) {

        return generators.get(projectTypeId);
    }

}
