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
import com.codenvy.api.builder.internal.BuilderException;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.core.rest.ProxyResponse;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper for RemoteBuildTask.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class BuildQueueTask implements Cancellable {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long                    id;
    private final long                    created;
    private final BaseBuilderRequest      request;
    private final Future<RemoteBuildTask> future;

    private RemoteBuildTask remote;

    BuildQueueTask(BaseBuilderRequest request, Future<RemoteBuildTask> future) {
        id = sequence.getAndIncrement();
        created = System.currentTimeMillis();
        this.future = future;
        this.request = request;
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    public Long getId() {
        return id;
    }

    /**
     * Reports that the task was interrupted.
     *
     * @return {@code true} if task was interrupted and {@code false} otherwise
     */
    public boolean isCancelled() throws IOException, RemoteException, BuilderException {
        if (future.isCancelled()) {
            return true;
        }
        final RemoteBuildTask remoteTask = getRemoteTask();
        // if task is not started yet it is not cancelled
        return remoteTask != null && remoteTask.getBuildTaskDescriptor().getStatus() == BuildStatus.CANCELLED;
    }

    /**
     * Reports that the task is waiting in the BuildQueue.
     *
     * @return {@code true} if task is waiting and {@code false} if the build process already started on remote slave-builder
     */
    public boolean isWaiting() {
        return !future.isDone();
    }

    /** Get date when this task was created. */
    public long getCreationTime() {
        return created;
    }

    /**
     * Cancel this task. If task already started then we ask remote slave-builder to stop it otherwise just remote this task from the
     * BuildQueue.
     *
     * @throws RemoteException
     *         if an error occurs when ask remote slave-builder interrupt build process
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     */
    @Override
    public void cancel() throws IOException, RemoteException, BuilderException {
        if (future.isCancelled()) {
            return;
        }
        final RemoteBuildTask remoteTask = getRemoteTask();
        if (remoteTask != null) {
            remoteTask.cancel();
        } else {
            future.cancel(true);
        }
    }

    /**
     * Get status of this task.
     *
     * @throws RemoteException
     *         if an error that we understand occurs when ask remote slave-builder about status
     * @throws IOException
     *         if an i/o error occurs when ask remote slave-builder about status
     * @throws BuilderException
     *         if any other errors
     */
    public BuildTaskDescriptor getDescriptor(ServiceContext restfulRequestContext) throws RemoteException, IOException, BuilderException {
        if (isWaiting()) {
            final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
            final List<Link> links = new ArrayList<>(2);
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_GET_STATUS)
                                .withHref(servicePathBuilder.clone().path(BuilderService.class, "getStatus")
                                                            .build(request.getWorkspace(), id).toString()).withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_CANCEL)
                                .withHref(servicePathBuilder.clone().path(BuilderService.class, "cancel")
                                                            .build(request.getWorkspace(), id).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
            return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                             .withTaskId(id)
                             .withStatus(BuildStatus.IN_QUEUE)
                             .withLinks(links)
                             .withStartTime(-1);
        } else if (future.isCancelled()) {
            return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                             .withTaskId(id)
                             .withStatus(BuildStatus.CANCELLED)
                             .withStartTime(-1);
        }
        final BuildTaskDescriptor remoteStatus = getRemoteTask().getBuildTaskDescriptor();
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                         .withTaskId(id)
                         .withStatus(remoteStatus.getStatus())
                         .withLinks(rewriteKnownLinks(remoteStatus.getLinks(), servicePathBuilder))
                         .withStartTime(remoteStatus.getStartTime());
    }

    private List<Link> rewriteKnownLinks(List<Link> links, UriBuilder serviceUriBuilder) {
        final List<Link> rewritten = new ArrayList<>(5);
        for (Link link : links) {
            if (Constants.LINK_REL_GET_STATUS.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(BuilderService.class, "getStatus").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_CANCEL.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(BuilderService.class, "cancel").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_LOG.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(BuilderService.class, "getLogs").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_REPORT.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(BuilderService.class, "getReport").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_DOWNLOAD_RESULT.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                // Special behaviour for download links.
                // Download links may be multiple.
                // Relative path to download file is in query parameter so copy query string from original URL.
                final UriBuilder cloned = serviceUriBuilder.clone();
                cloned.path(BuilderService.class, "download");
                final String originalUrl = copy.getHref();
                final int q = originalUrl.indexOf('?');
                if (q > 0) {
                    cloned.replaceQuery(originalUrl.substring(q + 1));
                }
                copy.setHref(cloned.build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            }
        }
        return rewritten;
    }

    private RemoteBuildTask getRemoteTask() throws IOException, RemoteException, BuilderException {
        if (!future.isDone()) {
            return null;
        }
        if (remote == null) {
            try {
                remote = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause; // lets caller to get Error as is
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                } else if (cause instanceof RemoteException) {
                    throw (RemoteException)cause;
                } else if (cause instanceof IOException) {
                    throw (IOException)cause;
                } else {
                    throw new BuilderException(cause.getMessage(), cause);
                }
            }
        }
        return remote;
    }

    @Override
    public String toString() {
        return "BuildQueueTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }

    public void readLogs(ProxyResponse proxyResponse) throws BuilderException, IOException, RemoteException {
        if (isWaiting()) {
            // Logs aren't available until build starts
            throw new BuilderException("Logs are not available. Task is not started yet.");
        }
        getRemoteTask().readLogs(proxyResponse);
    }

    public void readReport(ProxyResponse proxyResponse) throws BuilderException, IOException, RemoteException {
        if (isWaiting()) {
            // Logs aren't available until build starts
            throw new BuilderException("Report is not available. Task is not started yet.");
        }
        getRemoteTask().readReport(proxyResponse);
    }

    public void download(String path, ProxyResponse proxyResponse) throws BuilderException, IOException, RemoteException {
        if (isWaiting()) {
            // There is nothing for download until build ends
            throw new BuilderException("Downloads are not available. Task is not started yet.");
        }
        getRemoteTask().download(path, proxyResponse);
    }
}
