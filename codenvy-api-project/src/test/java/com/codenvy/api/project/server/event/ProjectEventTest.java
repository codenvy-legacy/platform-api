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

import com.codenvy.api.project.server.type.ProjectType2;
import com.codenvy.api.project.server.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author gazarenkov
 */
public class ProjectEventTest {


    private ProjectManager      pm;

    @BeforeMethod
    public void setUp() throws Exception {


    }

    @Test
    public void test1() throws Exception {

    }

    public class MyProjectType extends ProjectType2 {

        public MyProjectType() {

            super("my", "my type");

            addConstantDefinition("const", "Constant", "const_value");

        }

    }
}
