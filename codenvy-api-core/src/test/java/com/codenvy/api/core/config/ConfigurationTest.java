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
package com.codenvy.api.core.config;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class ConfigurationTest {
    @Test
    public void testString() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final String value = "second";
        cfg.set(name, value);
        Assert.assertEquals(cfg.get(name), value);
        cfg.setIfNotSet(name, "new second");
        Assert.assertEquals(cfg.get(name), value);
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
    }

    @Test
    public void testBoolean() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final boolean value = true;
        cfg.setBoolean(name, value);
        Assert.assertEquals(cfg.getBoolean(name, false), value);
        Assert.assertEquals(cfg.get(name), "true");
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
    }

    @Test
    public void testInt() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final int value = 123;
        cfg.setInt(name, value);
        Assert.assertEquals(cfg.getInt(name, 456), value);
        Assert.assertEquals(cfg.get(name), "123");
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
    }

    @Test
    public void testStrings() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final List<String> value = new ArrayList<>(Arrays.asList("a", "b", "c"));
        cfg.setStrings(name, value);
        Assert.assertEquals(cfg.get(name), "a,b,c");
        Assert.assertEquals(cfg.getStrings(name), new ArrayList<>(Arrays.asList("a", "b", "c")));
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
        Assert.assertNotNull(cfg.getStrings(name));
        Assert.assertTrue(cfg.getStrings(name).isEmpty());
    }

    @Test
    public void testStringsTrimming() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final String value = "  a, \t b,  c  ";
        cfg.set(name, value);
        Assert.assertEquals(cfg.get(name), value);
        Assert.assertEquals(cfg.getStrings(name), new ArrayList<>(Arrays.asList("a", "b", "c")));  // all whitespaces are removed
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
    }

    @Test
    public void testFile() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final String value = "file:/home/andrew/some_file.txt";
        cfg.set(name, value);
        Assert.assertEquals(cfg.get(name), value);
        Assert.assertEquals(cfg.getFile(name, null), new java.io.File("/home/andrew/some_file.txt"));
        cfg.set(name, null);
        Assert.assertNull(cfg.get(name));
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testInvalidFile() {
        final Configuration cfg = new Configuration();
        final String name = "name";
        final String value = "/home/andrew/some_file.txt";
        cfg.set(name, value);
        cfg.getFile(name, null);
    }

    @Test
    public void testMergeRetain() throws Exception {
        Configuration cfg1 = new Configuration();
        cfg1.set("a", "A1");
        cfg1.set("b", "B1");
        Configuration cfg2 = new Configuration();
        cfg2.set("a", "A2");
        cfg2.set("b", "B2");
        cfg2.set("c", "C2");
        cfg1.merge(cfg2, Configuration.MergePolicy.RETAIN);
        Assert.assertEquals(cfg1.get("a"), "A1");
        Assert.assertEquals(cfg1.get("b"), "B1");
        Assert.assertEquals(cfg1.get("c"), "C2");
    }

    @Test
    public void testMergeOverride() throws Exception {
        Configuration cfg1 = new Configuration();
        cfg1.set("a", "A1");
        cfg1.set("b", "B1");
        Configuration cfg2 = new Configuration();
        cfg2.set("a", "A2");
        cfg2.set("b", "B2");
        cfg2.set("c", "C2");
        cfg1.merge(cfg2, Configuration.MergePolicy.OVERRIDE);
        Assert.assertEquals(cfg1.get("a"), "A2");
        Assert.assertEquals(cfg1.get("b"), "B2");
        Assert.assertEquals(cfg1.get("c"), "C2");
    }
}
