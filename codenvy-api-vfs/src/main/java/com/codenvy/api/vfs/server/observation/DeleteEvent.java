/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.vfs.server.observation;

import com.codenvy.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("vfs")
public class DeleteEvent extends VirtualFileEvent {
    public DeleteEvent(String workspaceId, String path) {
        super(workspaceId, path, ChangeType.DELETED);
    }

    public DeleteEvent() {
    }
}
