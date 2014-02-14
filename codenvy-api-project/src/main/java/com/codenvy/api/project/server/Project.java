/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import java.util.Collections;
import java.util.List;

/**
 * @author andrew00x
 */
public class Project {
    private VirtualFile virtualFile;

    public Project(VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    public String getName() {
        try {
            return virtualFile.getName();
        } catch (VirtualFileSystemException e) {
            throw new FileSystemLevelException(e.getMessage(), e);
        }
    }

    public List<Project> getModules() {
        // TODO
        return Collections.emptyList();
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }
}
