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
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Helps register {@link VirtualFileSystem}s on startup.
 *
 * @author andrew00x
 */
@Singleton
public final class VirtualFileSystemRegistryPlugin {
    @Inject
    public VirtualFileSystemRegistryPlugin(VirtualFileSystemRegistry registry, Set<VirtualFileSystemProvider> providers)
            throws VirtualFileSystemException {
        for (VirtualFileSystemProvider provider : providers) {
            registry.registerProvider(provider.getWorkspaceId(), provider);
        }
    }
}
