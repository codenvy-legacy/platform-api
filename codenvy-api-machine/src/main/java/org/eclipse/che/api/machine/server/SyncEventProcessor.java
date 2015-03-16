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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.commons.lang.NamedThreadFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
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
    private final int                           projectFilesPathsStartsFrom;
    private final String                        workspace;
    private final String                        project;
    private final String                        token;
    private final String                        apiEndPoint;
    private final ConcurrentLinkedQueue<String> events;
    private final ScheduledExecutorService      executor;

    @Inject
    public SyncEventProcessor(String watchPath,
                              String workspace,
                              String project,
                              String token,
                              @Named("machine.sync.delay") long delayInMilis,
                              @Named("api.endpoint") String apiEndPoint) {
        this.projectFilesPathsStartsFrom = watchPath.charAt(watchPath.length()) == '/' ? watchPath.length() : watchPath.length() + 1;
        this.workspace = workspace;
        this.project = project;
        this.token = token;
        this.apiEndPoint = apiEndPoint;
        this.events = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MachineProjectSourceSync-", true));
        this.executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    processLines();
                } catch (Exception e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
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

    @Override
    public void close() throws IOException {
        LOG.info("Sync processor is shutting down");
        executor.shutdownNow();
    }

    private void processLines() {
//        LOG.info("Sync processor starts processing");
        // todo use tree to remove parent automatically
        ArrayList<String> deleted = new ArrayList<>();
        // moved -> <from, to>
        // todo can we update multiple files by 1 request
        LinkedHashMap<String, String> moved = new LinkedHashMap<>();
        ArrayList<String> modified = new ArrayList<>();
        // created -> <path, isFolder>
        // fixme use tree to create parent folder at first
        LinkedHashMap<String, Boolean> created = new LinkedHashMap<>();
        for (String event; (event = events.poll()) != null; ) {
            LOG.info("Process event {}", event);
            if (Thread.currentThread().isInterrupted()) {
                LOG.info("Thread is interrupted");
                return;
            }
            // remove quotes
            event = event.substring(1, event.length() - 1);
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
            } else if (event.startsWith("MOVED_FROM")) {
                // file or folder was moved from project
                // if followed by MOVED_TO is considered as moving/renaming operation inside a project
                // if followed by MOVED_SELF is considered as removing operation

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

                // do not remove from queue. will be removed only if this event is considered as second part of one operation
                String nextEvent = events.peek();
                if (nextEvent == null || (!nextEvent.startsWith("MOVED_TO") && !nextEvent.startsWith("MOVED_SELF"))) {
                    LOG.error("Unexpected events chain: \n{}\n{}", event, nextEvent);
                    continue;
                } else if (nextEvent.startsWith("MOVED_SELF")) {
                    // moving folder out of the project considered as deletion
                    deleted.add(movedItem);
                } else {
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
            } else {
                // unexpected output
                LOG.error("unexpected output: {}", event);
            }
        }
        if (!created.isEmpty()) {
            // fixme create parent object before children
            LOG.info("Create:");
            for (Map.Entry<String, Boolean> createdItem : created.entrySet()) {
                LOG.info((createdItem.getValue() ? "Folder:{}" : "File:{}"), pretifyPath(createdItem.getKey()));
            }
        }
        if (!deleted.isEmpty()) {
            // TODO add heuristics to remove, e.g. skip removed children if parent folder was removed
            LOG.info("Removed:");
            for (String removedItem : deleted) {
                LOG.info(pretifyPath(removedItem));
            }
        }
        if (!modified.isEmpty()) {
            LOG.info("Modified:");
            for (String modifiedItem : modified) {
                LOG.info(pretifyPath(modifiedItem));
            }
        }
        if (!moved.isEmpty()) {
            LOG.info("Moved:");
            for (Map.Entry<String, String> movedItem : moved.entrySet()) {
                LOG.info("{} -> {}", pretifyPath(movedItem.getKey()), pretifyPath(movedItem.getValue()));
            }
        }
//        LOG.info("Sync processor finishes processing");
    }

    private void delete(String path) {
        try {
            HttpJsonHelper.delete(null, UriBuilder.fromUri(apiEndPoint)
                                                  .path("vfs")
                                                  .path(workspace)
                                                  .path("v2")
                                                  .path("delete")
                                                  .path(getVfsItemIdByPath(path))
                                                  .queryParam("token", token)
                                                  .build()
                                                  .toString());
        } catch (IOException | ApiException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private void createFolder(String path) {
//        try {
//            HttpJsonHelper.post(null,
//                                UriBuilder.fromUri(apiEndPoint)
//                                          .path("vfs")
//                                          .path(workspace)
//                                          .path("v2")
//                                          .path("folder")
//                                          .queryParam("name", path)
//                                          .build()
//                                          .toString(),
//                                null);
//        } catch (IOException | ApiException e) {
//            LOG.error(e.getLocalizedMessage(), e);
//        }
    }

    private void createFile(String name) {
        // fixme read file content
//        try {
//            HttpJsonHelper.post(null,
//                                UriBuilder.fromUri(apiEndPoint)
//                                          .path("vfs")
//                                          .path(workspace)
//                                          .path("v2")
//                                          .path("")
//                                          .build()
//                                          .toString(),
//                                null);
//        } catch (IOException | ApiException e) {
//            LOG.error(e.getLocalizedMessage(), e);
//        }
    }

    private String getVfsItemIdByPath(String path) throws IOException, ApiException {
        return HttpJsonHelper.get(Item.class, UriBuilder.fromUri(apiEndPoint)
                                                        .path("vfs")
                                                        .path(workspace)
                                                        .path("v2")
                                                        .path("itembypath")
                                                        .path(path)
                                                        .queryParam("token", token)
                                                        .build()
                                                        .toString())
                             .getId();
        //.queryParam("propertyFilter", "id") TODO
    }

    private String pretifyPath(String path) {
        return path.substring(projectFilesPathsStartsFrom);
    }
}
