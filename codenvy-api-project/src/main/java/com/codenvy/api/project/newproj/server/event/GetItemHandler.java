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
package com.codenvy.api.project.newproj.server.event;

import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.project.newproj.ProjectType2;

/**
 * @author gazarenkov
 */
public abstract class GetItemHandler implements EventSubscriber<GetItemEvent> {

    protected  final ProjectType2 type;

    protected GetItemHandler(ProjectType2 type) {
        this.type = type;
    }

    @Override
    public final void onEvent(GetItemEvent event) {

        if(event.getProjectType().isTypeOf(type.getId()))
            execute(event);

    }

    protected abstract void execute(GetItemEvent event);

}
