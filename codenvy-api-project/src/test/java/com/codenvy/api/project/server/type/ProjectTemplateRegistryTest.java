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

import com.codenvy.api.project.server.ProjectTemplateRegistry;
import com.codenvy.api.project.shared.dto.ProjectTemplateDescriptor;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vitaly Parfonov
 */
public class ProjectTemplateRegistryTest {

    private ProjectTemplateRegistry   templateRegistry;
    private ProjectTemplateDescriptor pt1;
    private ProjectTemplateDescriptor pt11;
    private ProjectTemplateDescriptor pt2;

    @BeforeMethod
    public void setUp() {
        templateRegistry = new ProjectTemplateRegistry();
        pt1 = mock(ProjectTemplateDescriptor.class);
        when(pt1.getProjectType()).thenReturn("mytype");
        pt11 = mock(ProjectTemplateDescriptor.class);
        when(pt11.getProjectType()).thenReturn("mytype");
        pt2 = mock(ProjectTemplateDescriptor.class);
        when(pt2.getProjectType()).thenReturn("myCoolType");

        templateRegistry.register(pt1);
    }


    @Test
    public void testRegisterTemplates() {
        Assert.assertNotNull(templateRegistry.getTemplates("mytype"));
    }


    @Test
    public void testRegisterTemplates2() {
        templateRegistry.register(pt1);
        templateRegistry.register(pt2);

        Assert.assertNotNull(templateRegistry.getTemplates("mytype"));
        Assert.assertEquals(2, templateRegistry.getTemplates("mytype").size());

        Assert.assertNotNull(templateRegistry.getTemplates("myCoolType"));
        Assert.assertEquals(1, templateRegistry.getTemplates("myCoolType").size());
    }



}
