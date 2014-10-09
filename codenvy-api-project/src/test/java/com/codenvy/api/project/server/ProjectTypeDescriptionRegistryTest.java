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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
public class ProjectTypeDescriptionRegistryTest {

    ProjectTypeDescriptionRegistry descriptionRegistry;

    @BeforeMethod
    public void setUp() {
        descriptionRegistry = new ProjectTypeDescriptionRegistry("test/host");
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
            public Builders getBuilders() {
                return null;
            }

            @Override
            public Runners getRunners() {
                return null;
            }

            @Override
            public List<ProjectTemplateDescription> getTemplates() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, String> getIconRegistry() {
                return Collections.emptyMap();
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
        List<AttributeDescription> _attributeDescriptions = descriptionRegistry.getAttributeDescriptions(myType);
        Assert.assertNotNull(_attributeDescriptions);
        Assert.assertEquals(_attributeDescriptions.size(), 2);//2 because we add one more attributes for all project type @see ProjectTypeDescriptionRegistry.registerDescription
        AttributeDescription _attributeDescription = _attributeDescriptions.get(0);
        Assert.assertEquals(_attributeDescription.getName(), "name3");
        Assert.assertEquals(_attributeDescription.getDescription(), "description3");
        List<Attribute> predefinedAttributes = descriptionRegistry.getPredefinedAttributes(myType);
        Assert.assertEquals(predefinedAttributes.size(), 2);
        Attribute a = findAttribute("name1", predefinedAttributes);
        Assert.assertNotNull(a);


        try {
            Assert.assertEquals(a.getValue(), "value1");
            a = findAttribute("name2", predefinedAttributes);
            Assert.assertNotNull(a);
            Assert.assertEquals(a.getValue(), "value2");
        } catch (ValueStorageException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRegisterProjectTypeWithIconRegistry() {
        final ProjectType type = new ProjectType("my_type", "my_type", "my_category");
        final List<Attribute> attributes = Arrays.asList(new Attribute("name1", "value1"), new Attribute("name2", "value2"));
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
            public Builders getBuilders() {
                return null;
            }

            @Override
            public Runners getRunners() {
                return null;
            }

            @Override
            public List<ProjectTemplateDescription> getTemplates() {
                return Collections.emptyList();
            }

            @Override
            public Map<String, String> getIconRegistry() {
                Map<String, String> icons = new HashMap<String, String>();
                icons.put("BIG", "/aaa/bbb/ccc.xml");
                return icons;
            }
        });
        ProjectType myType = descriptionRegistry.getProjectType("my_type");
        Assert.assertNotNull(myType);
        Map<String, String> iconRegistry = descriptionRegistry.getIconRegistry(myType);
        Assert.assertNotNull(iconRegistry);
        Assert.assertTrue(iconRegistry.containsKey("BIG"));
        Assert.assertEquals(iconRegistry.get("BIG"), "test/host/aaa/bbb/ccc.xml");

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
