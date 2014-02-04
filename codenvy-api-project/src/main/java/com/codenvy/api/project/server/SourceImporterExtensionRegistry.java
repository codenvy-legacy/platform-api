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

import com.codenvy.api.project.server.exceptions.SourceImporterNotFoundException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class SourceImporterExtensionRegistry {

    private final Map<String, SourceImporterExtension> importers;

    @Inject
    public SourceImporterExtensionRegistry() {
        this.importers = new ConcurrentHashMap<>();
    }

    public void register(SourceImporterExtension extension) {
        importers.put(extension.getType(), extension);
    }

    public SourceImporterExtension getImporter(String type) throws SourceImporterNotFoundException {
        if (importers.containsKey(type))
            return importers.get(type);
        else throw new SourceImporterNotFoundException(type + " source importer not registered in the system");
    }

    public List<String> getImporterTypes() {
        return new ArrayList<>(importers.keySet());
    }
}
