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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.notification.EventService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publishes builder's outputs to the EventService.
 *
 * @author andrew00x
 */
class BuildLogsPublisher extends DelegateBuildLogger {
    private final AtomicInteger lineCounter;
    private final EventService eventService;
    private final long         taskId;
    private final String       workspace;
    private final String       project;

    BuildLogsPublisher(BuildLogger delegate, EventService eventService, long taskId, String workspace, String project) {
        super(delegate);
        this.eventService = eventService;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
        lineCounter = new AtomicInteger(1);
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (line != null) {
            eventService.publish(BuilderEvent.messageLoggedEvent(taskId, workspace, project,
                                                                 new BuilderEvent.LoggedMessage(line, lineCounter.getAndIncrement())));
        }
        super.writeLine(line);
    }
}
