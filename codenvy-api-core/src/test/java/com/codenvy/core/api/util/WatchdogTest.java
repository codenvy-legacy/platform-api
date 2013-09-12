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
package com.codenvy.core.api.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class WatchdogTest {
    @Test
    public void testWatchDog() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] cancel = new boolean[]{false};
        final Cancellable myCancellable = new Cancellable() {
            @Override
            public void cancel() throws Exception {
                cancel[0] = true;
                latch.countDown();
            }
        };

        final Watchdog watchdog = new Watchdog(1, TimeUnit.SECONDS); // wait 1 sec then cancel myCancellable
        watchdog.start(myCancellable);
        latch.await(2, TimeUnit.SECONDS); // wait 2 sec
        Assert.assertTrue(cancel[0], "cancellation failed"); // should be cancelled
    }
}
