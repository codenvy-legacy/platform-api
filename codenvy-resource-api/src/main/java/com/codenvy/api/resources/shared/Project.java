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

import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class Project extends Folder {
    public Project(VirtualFileSystemConnector connector, Folder parent, String id, String name) {
        super(connector, parent, "PROJECT", id, name);
    }

    @Override
    public boolean isProject() {
        checkValid();
        return true;
    }

    public String getWorkspace() {
        checkValid();
        return connector.getWorkspaceName();
    }

    public String getDescription() {
        checkValid();
        return ""; // TODO
    }

    public String getProjectType() {
        checkValid();
        return (String)getAttributes().getAttribute("vfs:projectType").getValue();
    }

    public Project createProject(String name, List<Attribute<?>> attributes) {
        checkValid();
        return connector.createProject(this, name, attributes);
    }
}
