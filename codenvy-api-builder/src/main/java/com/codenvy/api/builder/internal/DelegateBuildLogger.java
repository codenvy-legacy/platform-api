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
package com.codenvy.api.builder.internal;

import java.io.IOException;
import java.io.Reader;

/**
 * Implementation of the {@code BuildLogger} which delegates log messages to underlying {@code BuildLogger}.
 *
 * @author andrew00x
 */
public abstract class DelegateBuildLogger implements BuildLogger {
    protected final BuildLogger delegate;

    public DelegateBuildLogger(BuildLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public Reader getReader() throws IOException {
        return delegate.getReader();
    }

    @Override
    public void writeLine(String line) throws IOException {
        delegate.writeLine(line);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public java.io.File getFile() {
        return delegate.getFile();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
