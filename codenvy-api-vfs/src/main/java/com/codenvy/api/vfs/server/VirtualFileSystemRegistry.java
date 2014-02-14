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
            VirtualFileSystemProvider newProvider = loadProvider(vfsId);
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
