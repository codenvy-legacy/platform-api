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

import java.io.IOException;

/**
 * Implementation of the {@code ApplicationLogger} which delegates log messages to underlying {@code ApplicationLogger}.
 *
 * @author andrew00x
 */
public abstract class DelegateApplicationLogger implements ApplicationLogger {
    protected final ApplicationLogger delegate;

    public DelegateApplicationLogger(ApplicationLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void getLogs(Appendable output) throws IOException {
        delegate.getLogs(output);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public void writeLine(String line) throws IOException {
        delegate.writeLine(line);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
