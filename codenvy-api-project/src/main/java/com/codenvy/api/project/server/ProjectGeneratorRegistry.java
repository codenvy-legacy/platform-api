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
 */
@Singleton
public class ProjectGeneratorRegistry {
    private final Map<String, ProjectGenerator> generators;

    @Inject
    public ProjectGeneratorRegistry(Set<ProjectGenerator> generators) {
        this.generators = new ConcurrentHashMap<>();
        for (ProjectGenerator generator : generators) {
            this.generators.put(generator.getProjectTypeId(), generator);
            //register(generator);
        }
    }

    // for tests only?
    public void register(ProjectGenerator generator) {

        this.generators.put(generator.getProjectTypeId(), generator);

//        List<ProjectGenerator> projectGenerators = generators.get(generator.getProjectTypeId());
//        if (projectGenerators == null) {
//            projectGenerators = new LinkedList<>();
//        }
//        projectGenerators.add(generator);
//        generators.put(generator.getProjectTypeId(), projectGenerators);
    }
//
//    public ProjectGenerator unregister(String projectTypeId) {
//        if (projectTypeId == null) {
//            return null;
//        }
//        List<ProjectGenerator> projectGenerators = generators.get(projectTypeId);
//        if (projectGenerators != null) {
//            for (ProjectGenerator generator : projectGenerators) {
//                if (id.equals(generator.getId())) {
//                    projectGenerators.remove(generator);
//                    return generator;
//                }
//            }
//        }
//
//        return null;
//    }

    public ProjectGenerator getGenerator(String projectTypeId) {
//        if (id == null || projectTypeId == null) {
//            return null;
//        }
//        List<ProjectGenerator> projectGenerators = generators.get(projectTypeId);
//        if (projectGenerators != null) {
//            for (ProjectGenerator generator : projectGenerators) {
//                if (id.equals(generator.getId())) {
//                    return generator;
//                }
//            }
//        }
        return generators.get(projectTypeId);
    }

//    public List<ProjectGenerator> getGenerators() {
//        List<ProjectGenerator> list = new ArrayList<>();
//        for (List<ProjectGenerator> projectGenerators : generators.values()) {
//            list.addAll(projectGenerators);
//        }
//        return list;
//    }
}
