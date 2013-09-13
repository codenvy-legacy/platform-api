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

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class DefaultBuildLogger implements BuildLogger {
    private final java.io.File file;
    private final String       contentType;
    private final Writer       writer;
    private final boolean      autoFlush;

    public DefaultBuildLogger(java.io.File file, String contentType) throws IOException {
        this.file = file;
        this.contentType = contentType;
        autoFlush = true;
        writer = new BufferedWriter(new FileWriter(file));
    }

    @Override
    public Reader getReader() throws IOException {
        return new FileReader(file);
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public java.io.File getFile() {
        return file;
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (line != null) {
            writer.write(line);
        }
        writer.write('\n');
        if (autoFlush) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
