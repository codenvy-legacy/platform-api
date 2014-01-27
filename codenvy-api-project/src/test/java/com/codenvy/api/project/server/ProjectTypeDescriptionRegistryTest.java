/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ProjectTypeDescription;
import com.codenvy.api.project.shared.ProjectTypeExtension;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author andrew00x
 */
public class ProjectTypeDescriptionRegistryTest {

    ProjectTypeRegistry            typeRegistry;
    ProjectTypeDescriptionRegistry descriptionRegistry;

    @BeforeMethod
    public void setUp() {
        typeRegistry = new ProjectTypeRegistry();
        descriptionRegistry = new ProjectTypeDescriptionRegistry(typeRegistry);
    }

    @Test
    public void testRegisterProjectType() {
        final ProjectType type = new ProjectType("my_type", "my_type");
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
        });
        ProjectType myType = typeRegistry.getProjectType("my_type");
        Assert.assertNotNull(myType);
        Assert.assertEquals(myType.getName(), "my_type");
        Assert.assertEquals(myType.getId(), "my_type");
        ProjectTypeDescription myTypeDescription = descriptionRegistry.getDescription(myType);
        Assert.assertNotNull(myTypeDescription);
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
