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
package com.codenvy.api.project.newproj.server;

import com.codenvy.api.project.newproj.server.event.GetItemEvent;
import com.codenvy.api.project.newproj.server.event.GetItemSubcriber;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author gazarenkov
 */
@Singleton
public class MyGetFileSubscriber extends GetItemSubcriber {

    @Inject
    public MyGetFileSubscriber(MyProjectType type) {
        super(type);
    }

    @Override
    protected void execute(GetItemEvent event) {
        System.out.println("EVENT >>>>>" + event.getProjectType().getId() + " --> " + event.getFile()/*.getVirtualFile().getProperties()*/);
    }
}
