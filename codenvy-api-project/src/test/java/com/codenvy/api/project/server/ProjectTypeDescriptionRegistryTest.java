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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.AttributeDescription;
import com.codenvy.api.project.shared.ProjectTemplateDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectTypeDescriptionRegistryTest {

    ProjectTypeDescriptionRegistry descriptionRegistry;

    @BeforeMethod
    public void setUp() {
        descriptionRegistry = new ProjectTypeDescriptionRegistry();
    }

    @Test
    public void testRegisterProjectType() {
        final ProjectType type = new ProjectType("my_type", "my_type", "my_category");
        final List<Attribute> attributes = Arrays.asList(new Attribute("name1", "value1"), new Attribute("name2", "value2"));
        final List<AttributeDescription> attributeDescriptions = Arrays.asList(new AttributeDescription("name3", "description3"));
        descriptionRegistry.registerProjectType(new ProjectTypeExtension() {
            @Override
            public ProjectType getProjectType() {
                return type;
            }

            @Override
            public List<Attribute> getPredefinedAttributes() {
                return attributes;
            }

            @Override
            public List<ProjectTemplateDescription> getTemplates() {
                return Collections.emptyList();
            }
        });
        descriptionRegistry.registerDescription(new ProjectTypeDescriptionExtension() {
            @Override
            public List<ProjectType> getProjectTypes() {
                return Arrays.asList(type);
            }

            @Override
            public List<AttributeDescription> getAttributeDescriptions() {
                return attributeDescriptions;
            }
        });
        ProjectType myType = descriptionRegistry.getProjectType("my_type");
        Assert.assertNotNull(myType);
        Assert.assertEquals(myType.getName(), "my_type");
        Assert.assertEquals(myType.getId(), "my_type");
        Assert.assertEquals(myType.getCategory(), "my_category");
        ProjectTypeDescription myTypeDescription = descriptionRegistry.getDescription(myType);
        Assert.assertNotNull(myTypeDescription);
        AttributeDescription ad = myTypeDescription.getAttributeDescription("name3");
        Assert.assertNotNull(ad);
        List<Attribute> predefinedAttributes = descriptionRegistry.getPredefinedAttributes(myType);
        Assert.assertEquals(predefinedAttributes.size(), 2);
        Attribute a = findAttribute("name1", predefinedAttributes);
        Assert.assertNotNull(a);
        Assert.assertEquals(a.getValue(), "value1");
        a = findAttribute("name2", predefinedAttributes);
        Assert.assertNotNull(a);
        Assert.assertEquals(a.getValue(), "value2");
    }

    private Attribute findAttribute(String name, List<Attribute> list) {
        for (Attribute attribute : list) {
            if (name.equals(attribute.getName())) {
                return attribute;
            }
        }
        return null;
    }
}
