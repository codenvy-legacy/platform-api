/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
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
package com.codenvy.api.project.shared;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class AttributeTest {
    @Test
    public void getChild() {
        Attribute attrA = new Attribute("a", "value1");
        Attribute attrB = new Attribute("b", "value2");
        Attribute attrC = new Attribute("c", "value3");
        Attribute attrB1 = new Attribute("b1", "value21");
        Attribute attrC1 = new Attribute("c1", "value31");
        attrB.addChild(attrC);
        attrB1.addChild(attrC1);
        attrA.addChild(attrB1);
        attrA.addChild(attrB);
        Attribute attrParent = new Attribute("x", "y");
        attrParent.addChild(attrA);
        Assert.assertTrue(attrParent.hasChild("a.b.c"));
        Attribute child = attrParent.getChild("a.b.c");
        Assert.assertNotNull(child);
        Assert.assertEquals(child.getValue(), "value3");
    }

    @Test
    public void hasChildren() {
        Attribute attrA = new Attribute("a", "value1");
        Attribute attrB = new Attribute("b", "value2");
        attrA.addChild(attrB);
        Assert.assertTrue(attrA.hasChildren());
        Assert.assertFalse(attrB.hasChildren());
    }

    @Test
    public void addChild() {
        Attribute attrA = new Attribute("a", "value1");
        Attribute attrB = new Attribute("b", "value2");
        Attribute attrC = new Attribute("c", "value3");
        attrB.addChild(attrC);
        attrA.addChild(attrB);
        Assert.assertTrue(attrA.hasChildren());
        Assert.assertTrue(attrB.hasChildren());
        Assert.assertNotNull(attrA.getChild("b"));
        Assert.assertNotNull(attrA.getChild("b.c"));
        Assert.assertEquals(attrB.getFullName(), "a.b");
        Assert.assertEquals(attrC.getFullName(), "a.b.c");
    }

    @Test(expectedExceptions={IllegalArgumentException.class})
    public void addChildNull() {
        Attribute attrA = new Attribute("a", "value1");
        attrA.addChild(null);
    }

    @Test(expectedExceptions={IllegalArgumentException.class})
    public void addChildNameConflict() {
        Attribute attrA = new Attribute("a", "value1");
        Attribute attrB = new Attribute("b", "value2");
        attrA.addChild(attrB);
        Attribute attrBConflict = new Attribute("b", "any");
        attrA.addChild(attrBConflict);
    }

    @Test
    public void removeChild() {
        Attribute attrA = new Attribute("a", "value1");
        Attribute attrB = new Attribute("b", "value2");
        Attribute attrC = new Attribute("c", "value3");
        attrB.addChild(attrC);
        attrA.addChild(attrB);
        Assert.assertNotNull(attrA.removeChild("b"));
        Assert.assertEquals(attrB.getFullName(), "b");
        Assert.assertEquals(attrC.getFullName(), "b.c");
        Assert.assertFalse(attrA.hasChildren());
        Assert.assertTrue(attrB.hasChildren());
    }

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
}
