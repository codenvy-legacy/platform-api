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

import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** @author andrew00x */
@DynaModule
public class VirtualFileSystemModule extends AbstractModule {
    @Override
    protected void configure() {
        // Initialize empty set of VirtualFileSystemProvider.
        Multibinder.newSetBinder(binder(), VirtualFileSystemProvider.class);
        bind(VirtualFileSystemRegistryPlugin.class);
    }
}
