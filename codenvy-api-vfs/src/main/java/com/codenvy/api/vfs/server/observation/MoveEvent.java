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
package com.codenvy.api.vfs.server.observation;

import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystemUser;

/**
 * @author andrew00x
 */
public class MoveEvent extends VirtualFileEvent {
    private final String oldPath;

    public MoveEvent(String oldPath, VirtualFile virtualFile, VirtualFileSystemUser user) {
        super(virtualFile, ChangeType.MOVED, user);
        this.oldPath = oldPath;
    }

    public String getOldPath() {
        return oldPath;
    }
}
