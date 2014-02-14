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
public class SourceImporterRegistry {

    private final Map<String, SourceImporter> importers;

    @Inject
    public SourceImporterRegistry(Set<SourceImporter> importers) {
        this.importers = new ConcurrentHashMap<>();
        for (SourceImporter importer : importers) {
            register(importer);
        }
    }

    public void register(SourceImporter importer) {
        importers.put(importer.getType(), importer);
    }

    public SourceImporter getImporter(String type) {
        final SourceImporter importer = importers.get(type);
        if (importer == null) {
            throw new IllegalArgumentException(String.format("%s source importer not registered in the system", type));
        }
        return importer;
    }

    public List<String> getImporterTypes() {
        return new ArrayList<>(importers.keySet());
    }
}
