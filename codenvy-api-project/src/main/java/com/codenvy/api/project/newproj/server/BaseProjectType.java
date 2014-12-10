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

import javax.inject.Singleton;

/**
 * @author gazarenkov
 */
@Singleton
public class BaseProjectType extends AbstractProjectType {

    public static final String ID = "blank";

    public BaseProjectType() {
        super(ID, "Blank");
        addVariableDefinition("vcs", "VCS", false);
    }

}
