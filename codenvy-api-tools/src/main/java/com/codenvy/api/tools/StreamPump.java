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
package com.codenvy.api.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class StreamPump implements Runnable {

    private BufferedReader bufferedReader;
    private LineConsumer   lineConsumer;

    private Exception exception;
    private boolean   done;

    public synchronized void start(Process process, LineConsumer lineConsumer) {
        this.lineConsumer = lineConsumer;
        bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final Thread t = new Thread(this, "StreamPump");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        try {
            bufferedReader.close();
        } catch (IOException ignored) {
        }
    }

    public synchronized void await() throws InterruptedException {
        while (!done) {
            wait();
        }
    }

    public synchronized boolean isDone() {
        return done;
    }

    public boolean hasError() {
        return null != exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lineConsumer.writeLine(line);
            }
        } catch (IOException e) {
            exception = e;
        } finally {
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }
    }
}
