/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.api.resources.shared;

import java.util.ArrayList;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class Workspace {
    private final VirtualFileSystemConnector connector;

    public Workspace(VirtualFileSystemConnector connector) {
        this.connector = connector;
    }

    public String getName() {
        return connector.getWorkspaceName();
    }

    public List<Project> getProjects() {
        final List<Project> projects = new ArrayList<>(4);
        for (Resource resource : connector.getRoot().getChildren()) {
            if (resource.isProject()) {
                projects.add((Project)resource);
            }
        }
        return projects;
    }

    public Project createProject(String name, List<Attribute<?>> attributes) {
        return connector.getRoot().createProject(name, attributes);
    }
}
