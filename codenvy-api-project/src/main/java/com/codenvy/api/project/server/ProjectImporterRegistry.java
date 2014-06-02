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
 * @author Vitaly Parfonov
 */
@Singleton
public class ProjectImporterRegistry {
    private final Map<String, ProjectImporter> importers;

    @Inject
    public ProjectImporterRegistry(Set<ProjectImporter> importers) {
        this.importers = new ConcurrentHashMap<>();
        for (ProjectImporter importer : importers) {
            register(importer);
        }
    }

    public void register(ProjectImporter importer) {
        importers.put(importer.getId(), importer);
    }

    public ProjectImporter unregister(String type) {
        if (type == null) {
            return null;
        }
        return importers.remove(type);
    }

    public ProjectImporter getImporter(String type) {
        if (type == null) {
            return null;
        }
        return importers.get(type);
    }

    public List<ProjectImporter> getImporters() {
        return new ArrayList<>(importers.values());
    }
}
