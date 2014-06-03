/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

/**
 * Filter for virtual files.
 *
 * @author andrew00x
 */
public interface VirtualFileFilter {
    /** Tests whether specified file should be included in result. */
    boolean accept(VirtualFile file) throws VirtualFileSystemException;

    VirtualFileFilter ALL = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return true;
        }
    };
}
