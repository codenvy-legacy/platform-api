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
package com.codenvy.api.project.shared;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/** @author andrew00x */
public class AttributeTest {
    @Test
    public void getValues() {
        Attribute attrA = new Attribute("a", "value1");
        Assert.assertEquals(attrA.getValue(), "value1");
        Assert.assertEquals(attrA.getValues(), Arrays.asList("value1"));
    }

    @Test
    public void setValue() {
        Attribute attrA = new Attribute("a", "value1");
        attrA.setValue("updated");
        Assert.assertEquals(attrA.getValue(), "updated");
        Assert.assertEquals(attrA.getValues(), Arrays.asList("updated"));
    }

    @Test
    public void setValues() {
        Attribute attrA = new Attribute("a", "value1");
        attrA.setValues(Arrays.asList("updated"));
        Assert.assertEquals(attrA.getValue(), "updated");
        Assert.assertEquals(attrA.getValues(), Arrays.asList("updated"));
    }

    @Test
    public void updateValue() {
        Attribute attrA = new Attribute("a", Arrays.asList("value1", "value2"));
        attrA.getValues().clear();
        Assert.assertEquals(attrA.getValues(), Arrays.asList("value1", "value2"));
    }
}
