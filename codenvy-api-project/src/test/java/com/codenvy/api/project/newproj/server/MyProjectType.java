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

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author gazarenkov
 */
@Singleton
public class MyProjectType extends AbstractProjectType {

    @Inject
    public MyProjectType(MyVPFactory myVPFactory) {

        super("my", "my type");

        attributes.add(new Constant("my", "const", "Constant", "const_value"));
        attributes.add(new Variable("my", "var", "Variable", true/*, myVPFactory.newInstance(null)*/));

    }

}
