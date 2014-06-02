/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
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
