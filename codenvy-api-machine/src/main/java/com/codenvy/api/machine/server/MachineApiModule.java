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
package com.codenvy.api.machine.server;

import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * @author andrew00x
 */
@DynaModule
public class MachineApiModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), MachineFactory.class);
        bind(MachineBuilderFactoryPlugin.class).asEagerSingleton();
    }
}
