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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains statistic about usage of all runners.
 *
 * @author andrew00x
 */
@Singleton
public class RunnerStats {
    private final AtomicInteger runningAppsCounter;

    @Inject
    RunnerStats(EventService eventService) {
        this.runningAppsCounter = new AtomicInteger(0);
        eventService.subscribe(new EventSubscriber<RunnerEvent>() {
            @Override
            public void onEvent(RunnerEvent event) {
                switch (event.getType()) {
                    case STARTED:
                        runningAppsCounter.incrementAndGet();
                        break;
                    case STOPPED:
                        runningAppsCounter.decrementAndGet();
                        break;
                }
            }
        });
    }

    public int getRunningAppsNum() {
        return runningAppsCounter.get();
    }
}
