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

import com.codenvy.commons.user.User;

/**
 * @author andrew00x
 */
public class LocalRunnerEvent {
    private final String        state;
    private final RunnerProcess runnerProcess;
    private final User          user;
    private final Throwable     error;

    public LocalRunnerEvent(RunnerProcess runnerProcess, String state, User user) {
        this(runnerProcess, state, null, user);
    }

    public LocalRunnerEvent(RunnerProcess runnerProcess, String state, Throwable error, User user) {
        this.runnerProcess = runnerProcess;
        this.state = state;
        this.user = user;
        this.error = error;
    }

    public RunnerProcess getRunnerProcess() {
        return runnerProcess;
    }

    public String getState() {
        return state;
    }

    public User getUser() {
        return user;
    }

    public Throwable getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }
}
