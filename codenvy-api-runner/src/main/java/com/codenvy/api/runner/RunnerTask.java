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

import com.codenvy.api.core.rest.ProxyResponse;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.runner.internal.dto.RunRequest;
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
 * Wraps RemoteRunnerProcess.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class RunnerTask implements Cancellable {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long                        id;
    private final RunRequest                  request;
    private final Future<RemoteRunnerProcess> future;
    private final long                        created;

    private RemoteRunnerProcess myRemoteProcess;

    RunnerTask(RunRequest request, Future<RemoteRunnerProcess> future) {
        this.id = sequence.getAndIncrement();
        this.future = future;
        this.request = request;
        created = System.currentTimeMillis();
    }

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    public Long getId() {
        return id;
    }

    public RunRequest getRequest() {
        return request;
    }

    /** Get date when this task was created. */
    public long getCreationTime() {
        return created;
    }

    /**
     * Get date when this application process was started. Returns {@code -1} if process is not started yet or if can't determine start
     * time.
     */
    public long getRunnerProcessStartTime() {
        try {
            final RemoteRunnerProcess remoteProcess = getRemoteProcess();
            if (remoteProcess != null) {
                return remoteProcess.getStartTime();
            }
        } catch (Exception ignored) {
            // If get exception then process is not started.
        }
        return -1;
    }

    /**
     * Get status of this process.
     *
     * @throws RemoteException
     *         if an error that we understand occurs when ask remote slave-runner about status
     * @throws IOException
     *         if an i/o error occurs when ask remote slave-runner about status
     * @throws RunnerException
     *         if any other errors
     */
    public ApplicationProcessDescriptor getDescriptor(ServiceContext restfulRequestContext)
            throws IOException, RemoteException, RunnerException {
        if (future.isCancelled()) {
            return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                             .withProcessId(id)
                             .withStatus(ApplicationStatus.CANCELLED);
        }
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
            final List<Link> links = new ArrayList<>(2);
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_GET_STATUS)
                                .withHref(servicePathBuilder.clone().path(RunnerService.class, "getStatus")
                                                            .build(request.getWorkspace(), id).toString()).withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_STOP)
                                .withHref(servicePathBuilder.clone().path(RunnerService.class, "stop")
                                                            .build(request.getWorkspace(), id).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
            return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                             .withProcessId(id)
                             .withStatus(ApplicationStatus.STOPPED)
                             .withLinks(links);
        }
        final ApplicationProcessDescriptor remoteStatus = remoteProcess.getApplicationProcessDescriptor();
        // re-write some parameters, we are working as revers-proxy
        return DtoFactory.getInstance().clone(remoteStatus)
                         .withProcessId(id)
                         .withLinks(rewriteKnownLinks(remoteStatus.getLinks(), restfulRequestContext.getServiceUriBuilder()));
    }

    private List<Link> rewriteKnownLinks(List<Link> links, UriBuilder serviceUriBuilder) {
        final List<Link> rewritten = new ArrayList<>(3);
        for (Link link : links) {
            if (Constants.LINK_REL_GET_STATUS.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(RunnerService.class, "getStatus").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_STOP.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(RunnerService.class, "stop").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_LOG.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(
                        serviceUriBuilder.clone().path(RunnerService.class, "getLogs").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            }
        }
        return rewritten;
    }

    /**
     * Stop process.
     *
     * @see #stop()
     */
    @Override
    public void cancel() throws Exception {
        stop();
    }

    /**
     * Reports that the process was interrupted.
     *
     * @return {@code true} if process was interrupted and {@code false} otherwise
     */
    public boolean isCancelled() throws IOException, RemoteException, RunnerException {
        if (future.isCancelled()) {
            return true;
        }
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        return remoteProcess != null && remoteProcess.getApplicationProcessDescriptor().getStatus() == ApplicationStatus.CANCELLED;
    }

    /** Stop process. */
    public void stop() throws RemoteException, IOException, RunnerException {
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

    public void readLogs(ProxyResponse proxyResponse) throws RemoteException, IOException, RunnerException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new RunnerException("Application isn't started yet, logs aren't available");
        }
        remoteProcess.readLogs(proxyResponse);
    }

    private RemoteRunnerProcess getRemoteProcess() throws RemoteException, IOException, RunnerException {
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
                } else if (cause instanceof RemoteException) {
                    throw (RemoteException)cause;
                } else if (cause instanceof IOException) {
                    throw (IOException)cause;
                } else {
                    throw new RunnerException(cause.getMessage(), cause);
                }
            }
        }
        return myRemoteProcess;
    }

    @Override
    public String toString() {
        return "RunnerTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }
}
