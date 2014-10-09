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
package com.codenvy.api.project.server.type;

import com.codenvy.api.project.server.ProjectTypeDescriptionExtension;
import com.codenvy.api.project.server.ProjectTypeExtension;
import com.codenvy.api.project.server.ProjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */
public abstract class SingleProjectTypeExtension implements ProjectTypeExtension, ProjectTypeDescriptionExtension {
    @Override
    public final List<ProjectType> getProjectTypes() {
        final List<ProjectType> list = new ArrayList<>(1);
        list.add(this.getProjectType());
        return list;
    }
}
