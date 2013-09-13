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

import com.codenvy.commons.json.JsonHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class DependencyCollector {
    private final List<Dependency> dependencies;

    public DependencyCollector() {
        dependencies = new ArrayList<>();
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public Dependency[] getDependencies() {
        return dependencies.toArray(new Dependency[dependencies.size()]);
    }

    public void writeJson(java.io.File jsonFile) throws IOException {
        try (Writer writer = new FileWriter(jsonFile)) {
            writeJson(writer);
        }
    }

    public void writeJson(Writer output) throws IOException {
        output.write(JsonHelper.toJson(dependencies));
    }

    /**
     * Describes one dependency of project.
     *
     * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
     */
    public static class Dependency {
        /**
         * Full name of project dependency. Typically name should provide information about name of library including a version number.
         * Different build system may sub-classes of this class to provide more details about dependency.
         */
        private String fullName;

        public Dependency(String fullName) {
            this.fullName = fullName;
        }

        public Dependency() {
        }

        /**
         * get name of project dependency.
         *
         * @return name of project dependency
         * @see #fullName
         */
        public String getFullName() {
            return fullName;
        }

        /**
         * Set name of project dependency.
         *
         * @param fullName
         *         name of project dependency
         * @see #fullName
         */
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        @Override
        public String toString() {
            return "Dependency{" +
                   "fullName='" + fullName + '\'' +
                   '}';
        }
    }
}