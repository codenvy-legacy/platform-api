/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.project.server;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author andrew00x
 */
@Singleton
public class ProjectGeneratorRegistry {
    private final Map<String, ProjectGenerator> generators;

    @Inject
    public ProjectGeneratorRegistry(Set<ProjectGenerator> generators) {
        this.generators = new ConcurrentHashMap<>();
        for (ProjectGenerator generator : generators) {
            register(generator);
        }
    }

    public void register(ProjectGenerator generator) {
        generators.put(generator.getId(), generator);
    }

    public ProjectGenerator unregister(String id) {
        if (id == null) {
            return null;
        }
        return generators.remove(id);
    }

    public ProjectGenerator getGenerator(String id) {
        if (id == null) {
            return null;
        }
        return generators.get(id);
    }

    public List<ProjectGenerator> getGenerators() {
        return new ArrayList<>(generators.values());
    }
}
