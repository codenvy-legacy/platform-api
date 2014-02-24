/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
        generators.put(generator.getName(), generator);
    }

    public ProjectGenerator unregister(String name) {
        if (name == null) {
            return null;
        }
        return generators.remove(name);
    }

    public ProjectGenerator getGenerator(String name) {
        if (name == null) {
            return null;
        }
        return generators.get(name);
    }

    public List<String> getGeneratorNames() {
        return new ArrayList<>(generators.keySet());
    }
}
