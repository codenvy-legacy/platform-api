/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server.observation;

import com.codenvy.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("vfs")
public class MoveEvent extends VirtualFileEvent {
    private String oldPath;

    public MoveEvent(String workspaceId, String path, String oldPath, boolean folder) {
        super(workspaceId, path, ChangeType.MOVED, folder);
        this.oldPath = oldPath;
    }

    public MoveEvent() {
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }
}
