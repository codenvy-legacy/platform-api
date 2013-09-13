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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public enum BuilderTaskType {
    DEFAULT("default", "Default build task"),
    LIST_DEPS("list dependencies", "List all project's dependencies"),
    COPY_DEPS("copy dependencies", "Copy all project's dependencies");

    private final String name;
    private final String description;

    private BuilderTaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "BuilderTaskType{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
