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

import com.codenvy.api.builder.internal.BuildStatus;
import com.codenvy.api.builder.internal.BuilderException;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.core.rest.RemoteAccessException;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class WaitingBuildTask {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long                    id;
    private final long                    since;
    private final BaseBuilderRequest      request;
    private final Future<RemoteBuildTask> future;

    private RemoteBuildTask remote;

    WaitingBuildTask(BaseBuilderRequest request, Future<RemoteBuildTask> future) {
        id = sequence.getAndIncrement();
        since = System.currentTimeMillis();
        this.future = future;
        this.request = request;
    }

    public Long getId() {
        return id;
    }

    public boolean isCancelled() throws IOException, RemoteException {
        if (future.isCancelled()) {
            return true;
        }
        final RemoteBuildTask remoteTask = getRemoteTask();
        if (remoteTask == null) {
            // is not started yet, so is not cancelled
            return false;
        }
        return remoteTask.getStatus().getStatus() == BuildStatus.CANCELLED;
    }

    public boolean isWaiting() {
        return !future.isDone();
    }

    public long getCreationDate() {
        return since;
    }

    public void cancel() throws IOException, RemoteException, BuilderException {
        if (future.isCancelled()) {
            return;
        }
        if (isWaiting()) {
            future.cancel(true);
        } else {
            getRemoteTask().cancel();
        }
    }

    public BuildTaskDescriptor getStatus(ServiceContext restfulRequestContext) throws RemoteException, IOException {
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        if (isWaiting()) {
            final List<Link> links = new ArrayList<>(2);

            final Link statusLink = DtoFactory.getInstance().createDto(Link.class);
            statusLink.setRel(Constants.LINK_REL_GET_STATUS);
            statusLink.setHref(
                    servicePathBuilder.clone().path(BuilderService.class, "getStatus").build(request.getWorkspace(), id).toString());
            statusLink.setMethod("GET");
            statusLink.setProduces(MediaType.APPLICATION_JSON);
            links.add(statusLink);

            final Link cancelLink = DtoFactory.getInstance().createDto(Link.class);
            cancelLink.setRel(Constants.LINK_REL_CANCEL);
            cancelLink
                    .setHref(servicePathBuilder.clone().path(BuilderService.class, "cancel").build(request.getWorkspace(), id).toString());
            cancelLink.setMethod("POST");
            cancelLink.setProduces(MediaType.APPLICATION_JSON);
            links.add(cancelLink);

            final BuildTaskDescriptor descriptor = DtoFactory.getInstance().createDto(BuildTaskDescriptor.class);
            descriptor.setTaskId(id);
            descriptor.setStatus(BuildStatus.IN_QUEUE);
            descriptor.setLinks(links);
            descriptor.setStartTime(-1);
            return descriptor;
        } else if (isCancelled()) {
            final BuildTaskDescriptor descriptor = DtoFactory.getInstance().createDto(BuildTaskDescriptor.class);
            descriptor.setTaskId(id);
            descriptor.setStatus(BuildStatus.CANCELLED);
            descriptor.setStartTime(-1);
            return descriptor;
        } else {
            final BuildTaskDescriptor remoteStatus = getRemoteTask().getStatus();
            final BuildTaskDescriptor descriptor = DtoFactory.getInstance().createDto(BuildTaskDescriptor.class);
            descriptor.setTaskId(id);
            descriptor.setStatus(remoteStatus.getStatus());
            descriptor.setLinks(rewriteKnownLinks(remoteStatus.getLinks(), servicePathBuilder));
            descriptor.setStartTime(remoteStatus.getStartTime());
            return descriptor;
        }
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

    private RemoteBuildTask getRemoteTask() throws IOException, RemoteException {
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
                } else if (cause instanceof RemoteAccessException) {
                    throw (RemoteAccessException)cause;
                } else if (cause instanceof RemoteException) {
                    throw (RemoteException)cause;
                } else if (cause instanceof IOException) {
                    throw (IOException)cause;
                } else {
                    throw new RemoteAccessException(cause.getMessage(), cause);
                }
            }
        }
        return remote;
    }

    @Override
    public String toString() {
        return "WaitingBuildTask{" +
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
