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
package com.codenvy.api.core.concurrent;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ThreadLocalPropagateContextTest {
    private static ThreadLocal<String> tl1 = new ThreadLocal<>();

    private ExecutorService exec;
    private final String tlValue = "my value";

    @BeforeTest
    public void setUp() {
        tl1.set(tlValue);
        ThreadLocalPropagateContext.addThreadLocal(tl1);
        Assert.assertEquals(ThreadLocalPropagateContext.getThreadLocals().length, 1);
        exec = Executors.newSingleThreadExecutor();
    }

    @AfterTest
    public void tearDown() {
        if (exec != null) {
            exec.shutdownNow();
        }
        ThreadLocalPropagateContext.removeThreadLocal(tl1);
        Assert.assertEquals(ThreadLocalPropagateContext.getThreadLocals().length, 0);
        tl1.remove();
    }

    @Test
    public void testRunnableWithoutThreadLocalPropagateContext() throws Exception {
        final String[] holder = new String[1];
        exec.submit(new Runnable() {
            @Override
            public void run() {
                holder[0] = tl1.get();
            }
        }).get();
        Assert.assertNull(holder[0]);
    }

    @Test
    public void testRunnableWithThreadLocalPropagateContext() throws Exception {
        final String[] holder = new String[1];
        exec.submit(ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                holder[0] = tl1.get();
            }
        })).get();
        Assert.assertEquals(holder[0], tlValue);
    }

    @Test
    public void testCallableWithoutThreadLocalPropagateContext() throws Exception {
        final String v = exec.submit(new Callable<String>() {
            @Override
            public String call() {
                return tl1.get();
            }
        }).get();
        Assert.assertNull(v);
    }

    @Test
    public void testCallableWithThreadLocalPropagateContext() throws Exception {
        final String v = exec.submit(ThreadLocalPropagateContext.wrap(new Callable<String>() {
            @Override
            public String call() {
                return tl1.get();
            }
        })).get();
        Assert.assertEquals(v, tlValue);
    }
}
