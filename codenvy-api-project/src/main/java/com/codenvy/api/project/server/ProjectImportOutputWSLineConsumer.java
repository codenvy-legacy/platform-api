/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.project.server;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.commons.lang.NamedThreadFactory;

/**
 * Send project import output to WS by skipping output messages written below the delay specified.
 */
public class ProjectImportOutputWSLineConsumer implements LineConsumer {

    private static final Logger        LOG             = LoggerFactory.getLogger(ProjectImportOutputWSLineConsumer.class);

    protected final AtomicInteger      lineCounter     = new AtomicInteger(1);

    protected String                   fPath;
    protected String                   fWorkspace;

    protected BlockingQueue<String>    lineToSendQueue = new ArrayBlockingQueue<String>(1024);

    // not using Executors helper: we want a queue capacity of 1. The scheduled runnable will treat all the pending items.
    protected ScheduledExecutorService executor        =
                                                         Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(
                                                                                                                           ProjectImportOutputWSLineConsumer.class.getName(),
                                                                                                                           false));

    public ProjectImportOutputWSLineConsumer(String fPath, String fWorkspace, int delayBetweenMessages) {
        this.fPath = fPath;
        this.fWorkspace = fWorkspace;
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                String lineToSend = null;
                // get the last line written from the queue
                while (!lineToSendQueue.isEmpty()) {
                    lineToSend = lineToSendQueue.poll();
                }
                if (lineToSend == null) {
                    return;
                }
                sendMessage(lineToSend);
            }
        }, 0, delayBetweenMessages, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    @Override
    public void writeLine(String line) throws IOException {
        try {
            lineToSendQueue.put(line);
        } catch (InterruptedException e1) {
            // ignore if interupted
        } catch (RejectedExecutionException e2) {
            // ignore, if 1 execution is already scheduled, no need to to add one more
        }
    }

    protected void sendMessage(String line) {
        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
        bm.setChannel("importProject:output:" + fWorkspace + ":" + fPath);
        bm.setBody(String.format("{\"num\":%d, \"line\":%s}",
                                 lineCounter.getAndIncrement(), JsonUtils.getJsonString(line)));
        sendMessageToWS(bm);
    }

    protected void sendMessageToWS(final ChannelBroadcastMessage bm) {
        try {
            WSConnectionContext.sendMessage(bm);
        } catch (Exception e) {
            LOG.error("A problem occurred while sending websocket message", e);
        }
    }
}
