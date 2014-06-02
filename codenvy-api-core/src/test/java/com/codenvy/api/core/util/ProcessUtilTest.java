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
package com.codenvy.api.core.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** @author andrew00x */
public class ProcessUtilTest {

    @Test
    public void testKill() throws Exception {
        final Process p = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "sleep 10; echo wake\\ up"});
        final List<String> stdout = new ArrayList<>();
        final List<String> stderr = new ArrayList<>();
        final IOException[] processError = new IOException[1];
        final CountDownLatch latch = new CountDownLatch(1);
        final long start = System.currentTimeMillis();
        new Thread() {
            public void run() {
                try {
                    ProcessUtil.process(p,
                                        new LineConsumer() {
                                            @Override
                                            public void writeLine(String line) throws IOException {
                                                stdout.add(line);
                                            }

                                            @Override
                                            public void close() throws IOException {
                                            }
                                        },
                                        new LineConsumer() {
                                            @Override
                                            public void writeLine(String line) throws IOException {
                                                stderr.add(line);
                                            }

                                            @Override
                                            public void close() throws IOException {
                                            }
                                        }
                                       );
                } catch (IOException e) {
                    processError[0] = e; // throw when kill process
                } finally {
                    latch.countDown();
                }
            }
        }.start();

        Thread.sleep(1000); // give time to start process
        Assert.assertTrue(ProcessUtil.isAlive(p), "Process is not started.");

        ProcessUtil.kill(p); // kill process

        latch.await(15, TimeUnit.SECONDS); // should not stop here if process killed
        final long end = System.currentTimeMillis();

        // System process sleeps 10 seconds. It is safety to check we done in less then 3 sec.
        Assert.assertTrue((end - start) < 3000, "Fail kill process");

        System.out.println(processError[0]);
        //processError[0].printStackTrace();
        System.out.println(stdout);
        System.out.println(stderr);
    }
}
