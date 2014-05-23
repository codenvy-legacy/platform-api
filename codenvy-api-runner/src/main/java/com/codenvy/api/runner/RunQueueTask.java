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

import com.codenvy.api.builder.dto.BuildTaskStats;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.ApplicationStats;
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
public final class RunQueueTask implements Cancellable {
    private final Long                        id;
    private final RunRequest                  request;
    private final Future<RemoteRunnerProcess> future;
    private final ValueHolder<BuildTaskStats> buildStatsHolder;
    private final long                        created;
    private final long                        waitingTimeLimit;
    private final long                        terminationTime;

    /* NOTE: don't use directly! Always use getter that makes copy of this UriBuilder. */
    private final UriBuilder uriBuilder;

    private UriBuilder getUriBuilder() {
        return uriBuilder.clone();
    }
    /* ~~~~ */

    private RemoteRunnerProcess myRemoteProcess;

    RunQueueTask(Long id,
                 RunRequest request,
                 long waitingTimeout,
                 long applicationLifetime,
                 Future<RemoteRunnerProcess> future,
                 ValueHolder<BuildTaskStats> buildStatsHolder,
                 UriBuilder uriBuilder) {
        this.id = id;
        this.future = future;
        this.request = request;
        this.buildStatsHolder = buildStatsHolder;
        this.uriBuilder = uriBuilder;
        created = System.currentTimeMillis();
        waitingTimeLimit = created + waitingTimeout;
        terminationTime = created + applicationLifetime;
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

    public ApplicationProcessDescriptor getDescriptor() throws RunnerException, NotFoundException {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        ApplicationProcessDescriptor descriptor;
        if (future.isCancelled()) {
            descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class).withProcessId(id).withStatus(ApplicationStatus.CANCELLED);
        } else {
            final RemoteRunnerProcess remoteProcess = getRemoteProcess();
            if (remoteProcess == null) {
                final List<Link> links = new ArrayList<>(2);
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_GET_STATUS)
                                    .withHref(getUriBuilder().path(RunnerService.class, "getStatus")
                                                             .build(request.getWorkspace(), id).toString()).withMethod("GET")
                                    .withProduces(MediaType.APPLICATION_JSON));
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_STOP)
                                    .withHref(getUriBuilder().path(RunnerService.class, "stop")
                                                             .build(request.getWorkspace(), id).toString())
                                    .withMethod("POST")
                                    .withProduces(MediaType.APPLICATION_JSON));
                descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class)
                                       .withProcessId(id)
                                       .withStatus(ApplicationStatus.NEW)
                                       .withLinks(links);
            } else {
                final ApplicationProcessDescriptor remoteStatus = remoteProcess.getApplicationProcessDescriptor();
                // re-write some parameters, we are working as revers-proxy
                descriptor = dtoFactory.clone(remoteStatus).withProcessId(id).withLinks(rewriteKnownLinks(remoteStatus.getLinks()));
            }
        }
        final ApplicationStats stats = calculateStats(descriptor);
        return descriptor.withStats(stats);
    }

    private ApplicationStats calculateStats(ApplicationProcessDescriptor descriptor) {
        final ApplicationStats stats = DtoFactory.getInstance().createDto(ApplicationStats.class)
                                                 .withCreationTime(created)
                                                 .withWaitingTimeLimit(waitingTimeLimit)
                                                 .withTerminationTime(terminationTime);
        final long started = descriptor.getStartTime();
        if (started > 0) {
            stats.setWaitingTime(started - created);
            final long stopped = descriptor.getStopTime();
            stats.setUptime(stopped > 0 ? (stopped - started) : (System.currentTimeMillis() - started));
        } else {
            stats.setWaitingTime(System.currentTimeMillis() - created);
        }
        if (buildStatsHolder != null) {
            stats.setBuildTaskStats(buildStatsHolder.get());
        }
        final ApplicationStats remoteStats = descriptor.getStats();
        if (remoteStats != null) {
            stats.setEnvironmentStats(remoteStats.getEnvironmentStats());
        }
        return stats;
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

    @Override
    public void cancel() throws Exception {
        if (future.isCancelled()) {
            return;
        }
        doStop(getRemoteProcess());
    }

    public boolean isCancelled() throws RunnerException {
        return future.isCancelled();
    }

    public boolean isWaiting() {
        return !future.isDone();
    }

    private void doStop(RemoteRunnerProcess remoteProcess) throws RunnerException, NotFoundException {
        if (remoteProcess != null) {
            remoteProcess.stop();
        } else {
            future.cancel(true);
        }
    }

    public void readLogs(OutputProvider output) throws IOException, RunnerException, NotFoundException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new RunnerException("Application isn't started yet, logs aren't available");
        }
        remoteProcess.readLogs(output);
    }

    RemoteRunnerProcess getRemoteProcess() throws RunnerException, NotFoundException {
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
                } else if (cause instanceof RunnerException) {
                    throw (RunnerException)cause;
                } else if (cause instanceof NotFoundException) {
                    throw (NotFoundException)cause;
                } else if (cause instanceof ApiException) {
                    throw new RunnerException(((ApiException)cause).getServiceError());
                } else {
                    throw new RunnerException(cause.getMessage(), cause);
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
