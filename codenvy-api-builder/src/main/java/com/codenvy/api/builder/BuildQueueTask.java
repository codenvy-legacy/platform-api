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
package com.codenvy.api.builder;

import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.dto.BaseBuilderRequest;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author andrew00x
 */
public final class BuildQueueTask implements Cancellable {
    private final Long               id;
    private final long               created;
    private final BaseBuilderRequest request;
    private final Future<RemoteTask> future;

    /* NOTE: don't use directly! Always use getter that makes copy of this UriBuilder. */
    private final UriBuilder uriBuilder;

    private UriBuilder getUriBuilder() {
        return uriBuilder.clone();
    }
    /* ~~~~ */

    private RemoteTask remoteTask;

    BuildQueueTask(Long id, BaseBuilderRequest request, Future<RemoteTask> future, UriBuilder uriBuilder) {
        this.id = id;
        this.uriBuilder = uriBuilder;
        this.future = future;
        this.request = request;
        created = System.currentTimeMillis();
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    public Long getId() {
        return id;
    }

    public BaseBuilderRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    /**
     * Reports that the task was interrupted.
     *
     * @return {@code true} if task was interrupted and {@code false} otherwise
     */
    public boolean isCancelled() throws IOException, BuilderException {
        return future.isCancelled();
    }

    /**
     * Reports that the task is waiting in the BuildQueue.
     *
     * @return {@code true} if task is waiting and {@code false} if the build process already started on slave-builder
     */
    public boolean isWaiting() {
        return !future.isDone();
    }

    /** Get date when this task was created. */
    public long getCreationTime() {
        return created;
    }

    /**
     * Cancel this task.
     *
     * @throws Exception
     *         if other error occurs
     */
    @Override
    public void cancel() throws Exception {
        if (future.isCancelled()) {
            return;
        }
        final RemoteTask task = getRemoteTask();
        if (task != null) {
            task.cancel();
        } else {
            future.cancel(true);
        }
    }

    /**
     * Get status of this task.
     *
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTaskDescriptor getDescriptor() throws BuilderException, NotFoundException {
        if (isWaiting()) {
            final List<Link> links = new ArrayList<>(2);
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_GET_STATUS)
                                .withHref(getUriBuilder().path(BuilderService.class, "getStatus").build(request.getWorkspace(), id)
                                                         .toString())
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_CANCEL)
                                .withHref(getUriBuilder().path(BuilderService.class, "cancel").build(request.getWorkspace(), id).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
            return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                             .withTaskId(id)
                             .withStatus(BuildStatus.IN_QUEUE)
                             .withLinks(links)
                             .withEndTime(-1);
        } else if (future.isCancelled()) {
            return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                             .withTaskId(id)
                             .withStatus(BuildStatus.CANCELLED)
                             .withStartTime(-1)
                             .withEndTime(-1);
        }
        final BuildTaskDescriptor remoteStatus = getRemoteTask().getBuildTaskDescriptor();
        return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                         .withTaskId(id)
                         .withStatus(remoteStatus.getStatus())
                         .withLinks(rewriteKnownLinks(remoteStatus.getLinks()))
                         .withStartTime(remoteStatus.getStartTime())
                         .withEndTime(remoteStatus.getEndTime());
    }

    private List<Link> rewriteKnownLinks(List<Link> links) {
        final List<Link> rewritten = new LinkedList<>();
        for (Link link : links) {
            if (Constants.LINK_REL_GET_STATUS.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(BuilderService.class, "getStatus").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_CANCEL.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(BuilderService.class, "cancel").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_LOG.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(BuilderService.class, "getLogs").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_REPORT.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(BuilderService.class, "getReport").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_DOWNLOAD_RESULT.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                // Special behaviour for download links.
                // Download links may be multiple.
                // Relative path to download file is in query parameter so copy query string from original URL.
                final UriBuilder myUriBuilder = getUriBuilder();
                myUriBuilder.path(BuilderService.class, "download");
                final String originalUrl = copy.getHref();
                final int q = originalUrl.indexOf('?');
                if (q > 0) {
                    myUriBuilder.replaceQuery(originalUrl.substring(q + 1));
                }
                copy.setHref(myUriBuilder.build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            }
        }
        return rewritten;
    }

    RemoteTask getRemoteTask() throws NotFoundException, BuilderException {
        if (!future.isDone()) {
            return null;
        }
        if (remoteTask == null) {
            try {
                remoteTask = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                } else if (cause instanceof NotFoundException) {
                    throw (NotFoundException)cause;
                } else if (cause instanceof ApiException) {
                    throw new BuilderException(((ApiException)cause).getServiceError());
                } else {
                    throw new BuilderException(cause.getMessage(), cause);
                }
            }
        }
        return remoteTask;
    }

    @Override
    public String toString() {
        return "BuildQueueTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }

    public void readLogs(OutputProvider output) throws BuilderException, IOException, NotFoundException {
        if (isWaiting()) {
            // Logs aren't available until build starts
            throw new BuilderException("Logs are not available. Task is not started yet.");
        }
        getRemoteTask().readLogs(output);
    }

    public void readReport(OutputProvider output) throws BuilderException, IOException, NotFoundException {
        if (isWaiting()) {
            // Logs aren't available until build starts
            throw new BuilderException("Report is not available. Task is not started yet.");
        }
        getRemoteTask().readReport(output);
    }

    public void download(String path, OutputProvider output) throws BuilderException, IOException, NotFoundException {
        if (isWaiting()) {
            // There is nothing for download until build ends
            throw new BuilderException("Downloads are not available. Task is not started yet.");
        }
        getRemoteTask().readFile(path, output);
    }
}
