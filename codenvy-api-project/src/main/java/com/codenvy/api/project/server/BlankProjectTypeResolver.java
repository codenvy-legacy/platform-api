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

import com.codenvy.api.project.shared.Constants;
import com.codenvy.api.project.shared.dto.ProjectUpdate;

/**
 * This is standard project type resolver, it can resolve any kind of project and set project type to
 * {@link com.codenvy.api.project.shared.Constants#BLANK_ID}
 * @author Evgen Vidolob
 */
public class BlankProjectTypeResolver implements ProjectTypeResolver {

    @Override
    public boolean resolve(Project project, ProjectUpdate description) {
        description.setProjectTypeId(Constants.BLANK_ID);
        return true;
    }
}
