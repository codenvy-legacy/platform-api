/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
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
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.dto.Dependency;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.dto.server.JsonArrayImpl;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects dependencies of project and writes it in JSON format.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class DependencyCollector {
    private final List<Dependency> dependencies;

    public DependencyCollector() {
        dependencies = new ArrayList<>();
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public void writeJson(java.io.File jsonFile) throws IOException {
        try (Writer writer = Files.newBufferedWriter(jsonFile.toPath(), Charset.forName("UTF-8"))) {
            writeJson(writer);
        }
    }

    public void writeJson(Writer output) throws IOException {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        output.write(dtoFactory.toJson(new JsonArrayImpl<>(dependencies)));
    }
}
