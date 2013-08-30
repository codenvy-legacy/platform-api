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
public class Folder extends Resource {
    public Folder(VirtualFileSystemConnector workspace, Folder parent, String id, String name) {
        this(workspace, parent, "FOLDER", id, name);
    }

    protected Folder(VirtualFileSystemConnector workspace, Folder parent, String type, String id, String name) {
        super(workspace, parent, type, id, name);
    }

    @Override
    public boolean isFolder() {
        checkValid();
        return true;
    }

    @Override
    public boolean isProject() {
        checkValid();
        return false;
    }

    @Override
    public boolean isFile() {
        checkValid();
        return false;
    }

    public Resource getResource(String name) {
        checkValid();
        return connector.getResource(this, name);
    }

    public List<Resource> getChildren() {
        checkValid();
        return connector.getChildResources(this);
    }

    public File createFile(String name) {
        checkValid();
        return connector.createFile(this, name);
    }

    public Folder createFolder(String name) {
        checkValid();
        return connector.createFolder(this, name);
    }

    public Project createProject(String name) {
        checkValid();
        return connector.createProject(name);
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public String createPath(String path) {
        if (isRoot()) {
            return '/' + path;
        }
        return getPath() + '/' + path;
    }
}
