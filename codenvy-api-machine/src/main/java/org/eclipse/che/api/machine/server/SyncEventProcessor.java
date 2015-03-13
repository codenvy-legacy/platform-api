/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.commons.lang.NamedThreadFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
* @author Alexander Garagatyi
*/
class SyncEventProcessor implements LineConsumer {
    private static final Logger LOG = getLogger(SyncEventProcessor.class);
    private final String                        workspace;
    private final String                        project;
    private final String                        token;
    private final ConcurrentLinkedQueue<String> events;
    private final ScheduledExecutorService      executor;

    @Inject
    public SyncEventProcessor(String workspace, String project, String token, long delayInMilis) {
        this.workspace = workspace;
        this.project = project;
        this.token = token;
        this.events = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MachineProjectSourceSync-", true));
        this.executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                processLines();
            }
        }, delayInMilis, delayInMilis, TimeUnit.MILLISECONDS);
        // TODO improve with wait() notify() : do nothing when files weren't changed
    }

    @Override
    public void writeLine(String line) throws IOException {
        // process events in separate thread to prevent native process stdout/stderr overflowing
        events.add(line);
        LOG.info("Add line {}", line);
    }

    private void processLines() {
        LOG.info("Sync processor starts processing");
        ArrayList<String> deleted = new ArrayList<>();
        // moved -> <from, to>
        LinkedHashMap<String, String> moved = new LinkedHashMap<>();
        ArrayList<String> modified = new ArrayList<>();
        // created -> <path, isFolder>
        LinkedHashMap<String, Boolean> created = new LinkedHashMap<>();
        for (String event; (event = events.poll()) != null; ) {
            LOG.info("Process event {}", event);
            if (Thread.currentThread().isInterrupted()) {
                LOG.info("Thread is interrupted");
                return;
            }
            if (event.startsWith("CREATE")) {
//                LOG.info("sync: create file {}", file);
                if (event.charAt(6) == ' ') {
                    // file was created
                    created.put(event.substring(7), false);
                } else {
                    // dir was created
                    created.put(event.substring(13), true);
                }
            } else if (event.startsWith("MODIFY")) {
//                LOG.info("sync: modify file {}", file);
                // file was modified
                modified.add(event.substring(7));
            } else if (event.startsWith("DELETE")) {
                // dir or file was deleted
                deleted.add(event.substring(event.charAt(6) == ' ' ? 7 : 13));
//                LOG.info("sync: delete dir {}", file);
            } else if (event.startsWith("MOVED_FROM")) {//,ISDIR
                // file or folder was moved from project
                // if followed by MOVED_TO is considered as moving/renaming operation inside a project
                // if it's not followed by MOVED_TO is considered as removing operation

                boolean isFolder = false;
                String movedItem;
                if (event.charAt(10) == ' ') {
                    // file, event format "MOVED_FROM path"
                    movedItem = event.substring(11);
                } else {
                    // folder, event format "MOVED_FROM,ISDIR path"
                    isFolder = true;
                    movedItem = event.substring(17);
                }

                // do not remove from queue. will be removed only if events are considered as move
                String nextEvent = events.peek();
                if (nextEvent == null || !nextEvent.startsWith("MOVED_TO")) {
                    // moved out of the project considered as deleted
                    deleted.add(movedItem);
                } else {
                    // TODO check that MOVE_FROM argument and MOVE_TO are the same
                    // example:
                    //     mv project/src /tmp/
                    //     mv /var/log project/src
                    //     MOVE_FROM,ISDIR project/src
                    //     MOVE_TO,ISDIR project/src

                    // weak heuristic. compare types of files (file or folder) for events
                    // if types differ process events separately
                    if ((isFolder && nextEvent.charAt(8) != ' ') || (!isFolder && nextEvent.charAt(8) != ' ')) {
                        moved.put(movedItem, nextEvent.substring(isFolder ? 14: 8));
                        events.poll();
                    } else {
                        deleted.add(movedItem);
                    }
                }
//                LOG.info("sync: move dir from {}", file);
            } else if (event.startsWith("MOVED_TO")) {
//                LOG.info("sync: move file to {}", event.substring(8));
                // file or folder was moved to project, considered as files creation
                if (event.charAt(8) == ' ') {
                    created.put(event.substring(9), false);
                } else {
                    created.put(event.substring(15), true);
                }
            }
        }
        LOG.info("Create:");
        for (Map.Entry<String, Boolean> createdItem : created.entrySet()) {
            LOG.info((createdItem.getValue() ? "Folder:{}" : "File:{}"), createdItem.getKey());
        }
        // TODO add heuristics to remove, e.g. skip removed children if parent folder was removed
        LOG.info("Removed:");
        for (String removedItem : deleted) {
            LOG.info(removedItem);
        }
        LOG.info("Modified:");
        for (String modifiedItem : modified) {
            LOG.info(modifiedItem);
        }
        LOG.info("Moved:");
        for (Map.Entry<String, String> movedItem : moved.entrySet()) {
            LOG.info("{} -> {}", movedItem.getKey(), movedItem.getValue());
        }
        LOG.info("Sync processor finishes processing");
    }

    @Override
    public void close() throws IOException {
        LOG.info("Sync processor is shutting down");
        executor.shutdownNow();
    }
}
