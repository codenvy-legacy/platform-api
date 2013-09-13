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

import com.codenvy.api.core.util.LineConsumer;

import java.io.IOException;
import java.io.Reader;

/**
 * Collects build logs. A BuildLogger is open after creation, and may consumes build logs with method {@link #writeLine(String)}. Once a
 * BuildLogger is closed, any attempt to write new logs upon it will cause a {@link java.io.IOException} to be thrown, but closing should
 * not prevent to get logs with {@link #getReader()}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface BuildLogger extends LineConsumer {
    /**
     * Get Reader of build log.
     *
     * @return reader
     * @throws java.io.IOException
     *         if any i/o errors occur
     */
    Reader getReader() throws IOException;

    /**
     * Get content type of build logs.
     *
     * @return content type
     */
    String getContentType();

    /**
     * Get {@code File} is case if logs stored in file.
     *
     * @return {@code File} or {@code null} if BuildLogger does not use {@code File} as backend
     */
    java.io.File getFile();

    /** Dummy {@code BuildLogger} implementation. */
    BuildLogger DUMMY = new BuildLogger() {
        @Override
        public Reader getReader() {
            return new Reader() {
                @Override
                public int read(char[] buf, int off, int len) throws IOException {
                    return -1;
                }

                @Override
                public void close() throws IOException {
                }
            };
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public java.io.File getFile() {
            return null;
        }

        @Override
        public void writeLine(String line) {
        }

        @Override
        public void close() {
        }
    };
}
