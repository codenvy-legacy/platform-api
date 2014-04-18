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
package com.codenvy.api.runner;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Wraps RemoteRunnerProcess.
 *
 * @author andrew00x
 */
public final class RunQueueTask {
    private final Long                        id;
    private final RunRequest                  request;
    private final Future<RemoteRunnerProcess> future;
    private final long                        created;

    /* NOTE: don't use directly! Always use getter that makes copy of this UriBuilder. */
    private final UriBuilder uriBuilder;

    private UriBuilder getUriBuilder() {
        return uriBuilder.clone();
    }
    /* ~~~~ */

    private RemoteRunnerProcess myRemoteProcess;

    RunQueueTask(Long id, RunRequest request, Future<RemoteRunnerProcess> future, UriBuilder uriBuilder) {
        this.id = id;
        this.future = future;
        this.request = request;
        this.uriBuilder = uriBuilder;
        created = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public RunRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    public long getCreationTime() {
        return created;
    }

    /** Get date when request for start application was sent to remote runner. Returns {@code -1} if process is still in the queue. */
    public long getSendToRemoteRunnerTime() {
        try {
            final RemoteRunnerProcess remoteProcess = getRemoteProcess();
            if (remoteProcess != null) {
                return remoteProcess.getCreationTime();
            }
        } catch (Exception ignored) {
            // If get exception then process is not started.
        }
        return -1;
    }

    public ApplicationProcessDescriptor getDescriptor() throws ApiException {
        if (future.isCancelled()) {
            return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                             .withProcessId(id)
                             .withStatus(ApplicationStatus.CANCELLED);
        }
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            final List<Link> links = new ArrayList<>(2);
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_GET_STATUS)
                                .withHref(getUriBuilder().path(RunnerService.class, "getStatus")
                                                         .build(request.getWorkspace(), id).toString()).withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_STOP)
                                .withHref(getUriBuilder().path(RunnerService.class, "stop")
                                                         .build(request.getWorkspace(), id).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
            return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                             .withProcessId(id)
                             .withStatus(ApplicationStatus.NEW)
                             .withLinks(links);
        }
        final ApplicationProcessDescriptor remoteStatus = remoteProcess.getApplicationProcessDescriptor();
        // re-write some parameters, we are working as revers-proxy
        return DtoFactory.getInstance().clone(remoteStatus)
                         .withProcessId(id)
                         .withLinks(rewriteKnownLinks(remoteStatus.getLinks()));
    }

    private List<Link> rewriteKnownLinks(List<Link> links) {
        final List<Link> rewritten = new ArrayList<>();
        for (Link link : links) {
            if (Constants.LINK_REL_GET_STATUS.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getStatus").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_STOP.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "stop").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_LOG.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getLogs").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else {
                rewritten.add(DtoFactory.getInstance().clone(link));
            }
        }
        return rewritten;
    }

    public void cancel() throws Exception {
        stop();
    }

    public boolean isCancelled() throws ApiException {
        return future.isCancelled();
    }

    public boolean isWaiting() {
        return !future.isDone();
    }

    public void stop() throws ApiException {
        if (future.isCancelled()) {
            return;
        }
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess != null) {
            remoteProcess.stop();
        } else {
            future.cancel(true);
        }
    }

    public void readLogs(OutputProvider output) throws IOException, ApiException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new ApiException("Application isn't started yet, logs aren't available");
        }
        remoteProcess.readLogs(output);
    }

    private RemoteRunnerProcess getRemoteProcess() throws ApiException {
        if (!future.isDone()) {
            return null;
        }
        if (myRemoteProcess == null) {
            try {
                myRemoteProcess = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause; // lets caller to get Error as is
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                } else if (cause instanceof ApiException) {
                    throw (ApiException)cause;
                } else {
                    throw new ApiException(cause.getMessage(), cause);
                }
            }
        }
        return myRemoteProcess;
    }

    @Override
    public String toString() {
        return "RunQueueTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }
}
