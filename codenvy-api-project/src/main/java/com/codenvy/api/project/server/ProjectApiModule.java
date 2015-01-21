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
package com.codenvy.api.project.server;

import com.codenvy.api.project.server.type.BaseProjectType;
import com.codenvy.api.project.server.type.ProjectType2;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * @author gazarenkov
 */
public class ProjectApiModule extends AbstractModule {

    @Override
    protected void configure() {

        Multibinder<ProjectType2> projectTypesMultibinder = Multibinder.newSetBinder(binder(), ProjectType2.class);
        projectTypesMultibinder.addBinding().to(BaseProjectType.class);

    }
}