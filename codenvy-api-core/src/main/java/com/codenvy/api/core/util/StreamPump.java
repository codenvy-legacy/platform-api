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
package com.codenvy.api.core.util;

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
