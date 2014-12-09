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
package com.codenvy.api.project.server.event;

import com.codenvy.api.project.newproj.server.event.GetItemEvent;
import com.codenvy.api.project.newproj.server.event.GetItemHandler;
import com.codenvy.api.project.server.type.ProjectTypeTest;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author gazarenkov
 */
@Singleton
public class MyGetFileSubscriber extends GetItemHandler {

    @Inject
    public MyGetFileSubscriber(ProjectTypeTest.MyProjectType type) {
        super(type);
    }

    @Override
    protected void execute(GetItemEvent event) {
        System.out.println("EVENT >>>>>" + event.getProjectType().getId() + " --> " + event.getFile()/*.getVirtualFile().getProperties()*/);
    }
}
