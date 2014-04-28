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

import java.io.IOException;

/**
 * Publishes application's outputs to the EventService.
 *
 * @author andrew00x
 */
public class ApplicationLogsPublisher extends DelegateApplicationLogger {
    private final EventService eventService;
    private final long         processId;
    private final String       workspace;
    private final String       project;

    public ApplicationLogsPublisher(ApplicationLogger delegate, EventService eventService, long processId, String workspace,
                                    String project) {
        super(delegate);
        this.eventService = eventService;
        this.processId = processId;
        this.workspace = workspace;
        this.project = project;
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (line != null) {
            eventService.publish(RunnerEvent.messageLoggedEvent(processId, workspace, project, line));
        }
        super.writeLine(line);
    }
}
