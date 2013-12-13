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
}
