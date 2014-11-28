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
package com.codenvy.api.core.util;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author andrew00x
 */
public class SizeTest {
    @Test
    public void testToHumanSize() {
        Assert.assertEquals(Size.toHumanSize(1024), "1 kB");
        Assert.assertEquals(Size.toHumanSize(1000), "1000 B");
        Assert.assertEquals(Size.toHumanSize(1024 * 1024), "1 MB");
        Assert.assertEquals(Size.toHumanSize(5 * 1024 * 1024), "5 MB");
        Assert.assertEquals(Size.toHumanSize(5l * 1024 * 1024 * 1024), "5 GB");
        Assert.assertEquals(Size.toHumanSize(7539480), "7.2 MB");
        Assert.assertEquals(Size.toHumanSize(10226124), "9.8 MB");
    }

    @Test
    public void testParse() {
        Assert.assertEquals(Size.parseSize("1 kB"), 1024);
        Assert.assertEquals(Size.parseSize("1000B"), 1000);
        Assert.assertEquals(Size.parseSize("1000"), 1000);
        Assert.assertEquals(Size.parseSize("1 MB"), 1024 * 1024);
        Assert.assertEquals(Size.parseSize("5 mb"), 5 * 1024 * 1024);
        Assert.assertEquals(Size.parseSize("9.8M"), Float.valueOf(9.8f * 1024 * 1024).longValue());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseErrorInvalidSuffix() {
        Size.parseSize("1 xx");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseErrorInvalidNumber() {
        Size.parseSize("1.x kB");
    }
}
