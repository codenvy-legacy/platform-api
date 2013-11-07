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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.util.LineConsumer;

import java.io.IOException;

/**
 * Collects application logs. A ApplicationLogger is open after creation, and may consumes applications logs with method {@link
 * #writeLine(String)}. Once a ApplicationLogger is closed, any attempt to write new lines upon it will cause a {@link java.io.IOException}
 * to be thrown, but closing should not prevent to get logs with {@link #getLogs(Appendable)}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface ApplicationLogger extends LineConsumer {
    /**
     * Get application logs.
     *
     * @param output
     *         output for logs
     * @throws java.io.IOException
     *         if an i/o errors occur
     */
    void getLogs(Appendable output) throws IOException;

    /**
     * Get content type of application logs.
     *
     * @return content type
     */
    String getContentType();

    /** Dummy {@code ApplicationLogger} implementation. */
    ApplicationLogger DUMMY = new ApplicationLogger() {
        @Override
        public void getLogs(Appendable output) {
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public void writeLine(String line) {
        }

        @Override
        public void close() {
        }
    };
}
