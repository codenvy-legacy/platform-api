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
package com.codenvy.api.project.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Deploys project API components.
 *
 * @author andrew00x
 */
public class BaseProjectModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ProjectImporter.class).addBinding().to(ZipProjectImporter.class);
        Multibinder.newSetBinder(binder(), ValueProviderFactory.class); /* empty binding */
        Multibinder.newSetBinder(binder(), ProjectGenerator.class); /* empty binding */
        bind(ProjectTypeDescriptionsExtension.class);
        bind(BaseProjectTypeExtension.class);
        bind(ProjectService.class);
        bind(ProjectTypeService.class);
        bind(ProjectImportersService.class);
        bind(ProjectEventService.class).asEagerSingleton();
    }
}
