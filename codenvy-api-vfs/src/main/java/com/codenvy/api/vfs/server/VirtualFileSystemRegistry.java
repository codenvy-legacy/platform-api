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

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry for virtual file system providers.
 *
 * @author andrew00x
 * @see VirtualFileSystemFactory
 */
@Singleton
public class VirtualFileSystemRegistry {
    private final ConcurrentMap<String, VirtualFileSystemProvider> providers = new ConcurrentHashMap<>();

    public void registerProvider(String vfsId, VirtualFileSystemProvider provider) throws VirtualFileSystemException {
        if (providers.putIfAbsent(id(vfsId), provider) != null) {
            throw new VirtualFileSystemException(String.format("Virtual file system %s already registered. ", vfsId));
        }
    }

    public void unregisterProvider(String vfsId) throws VirtualFileSystemException {
        final VirtualFileSystemProvider removed = providers.remove(id(vfsId));
        if (removed != null) {
            removed.close();
        }
    }

    public VirtualFileSystemProvider getProvider(String vfsId) throws VirtualFileSystemException {
        String myId = id(vfsId);
        VirtualFileSystemProvider provider = providers.get(myId);
        if (provider == null) {
            VirtualFileSystemProvider newProvider = loadProvider(myId);
            if (newProvider != null) {
                provider = providers.putIfAbsent(myId, newProvider);
                if (provider == null) {
                    provider = newProvider;
                }
            } else {
                throw new VirtualFileSystemException(String.format("Virtual file system %s does not exist. ", vfsId));
            }
        }
        return provider;
    }

    protected VirtualFileSystemProvider loadProvider(String vfsId) throws VirtualFileSystemException {
        return null;
    }

    public Collection<VirtualFileSystemProvider> getRegisteredProviders() throws VirtualFileSystemException {
        return Collections.unmodifiableCollection(providers.values());
    }

    private String id(String vfsId) {
        return vfsId == null ? "default" : vfsId;
    }
}
