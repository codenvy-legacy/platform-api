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
package com.codenvy.api.runner;

import com.codenvy.api.builder.BuilderException;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.OutputProvider;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Cancellable;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerMetric;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Wraps RemoteRunnerProcess.
 *
 * @author andrew00x
 */
public final class RunQueueTask implements Cancellable {
    private static final String           RFC1123_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final SimpleDateFormat RFC1123_DATE_FORMAT  = new SimpleDateFormat(RFC1123_DATE_PATTERN, Locale.US);

    private final Long                             id;
    private final RunRequest                       request;
    private final Future<RemoteRunnerProcess>      future;
    private final ValueHolder<BuildTaskDescriptor> buildTaskHolder;
    private final long                             created;
    private final long                             waitingTimeout;

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
                 Future<RemoteRunnerProcess> future,
                 ValueHolder<BuildTaskDescriptor> buildTaskHolder,
                 UriBuilder uriBuilder) {
        this.id = id;
        this.future = future;
        this.request = request;
        this.waitingTimeout = waitingTimeout;
        this.buildTaskHolder = buildTaskHolder;
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
                final List<RunnerMetric> runStats = new ArrayList<>(1);
                final SimpleDateFormat format = (SimpleDateFormat)RFC1123_DATE_FORMAT.clone();
                runStats.add(dtoFactory.createDto(RunnerMetric.class)
                                       .withName("waitingTimeLimit")
                                       .withValue(format.format(created + waitingTimeout))
                                       .withDescription("Waiting for start limit"));
                descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class)
                                       .withProcessId(id)
                                       .withStatus(ApplicationStatus.NEW)
                                       .withRunStats(runStats)
                                       .withLinks(links)
                                       .withWorkspace(request.getWorkspace())
                                       .withProject(request.getProject())
                                       .withUserName(request.getUserName());

            } else {
                final ApplicationProcessDescriptor remoteDescriptor = remoteProcess.getApplicationProcessDescriptor();
                // re-write some parameters, we are working as revers-proxy
                descriptor = dtoFactory.clone(remoteDescriptor).withProcessId(id).withLinks(rewriteKnownLinks(remoteDescriptor.getLinks()));
                final long started = descriptor.getStartTime();
                long waitingTimeMillis = started > 0 ? started - created : System.currentTimeMillis() - created;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(waitingTimeMillis);
                waitingTimeMillis -= TimeUnit.MINUTES.toMillis(minutes);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(waitingTimeMillis);
                waitingTimeMillis -= TimeUnit.SECONDS.toMillis(seconds);
                final List<RunnerMetric> runStats = descriptor.getRunStats();
                runStats.add(dtoFactory.createDto(RunnerMetric.class)
                                       .withName("waitingTime")
                                       .withValue(String.format("%02dm:%02ds:%03dms", minutes, seconds, waitingTimeMillis))
                                       .withDescription("Waiting for start duration"));
            }
            if (buildTaskHolder != null) {
                final BuildTaskDescriptor buildTaskDescriptor = buildTaskHolder.get();
                if (buildTaskDescriptor != null) {
                    descriptor.setBuildStats(buildTaskDescriptor.getBuildStats());
                }
            }
        }
        return descriptor;
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
            } else if (Constants.LINK_REL_RUNNER_RECIPE.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getRecipeFile").build(request.getWorkspace(), id).toString());
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

    public void readRecipeFile(OutputProvider output) throws RunnerException, IOException, NotFoundException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new RunnerException("Application isn't started yet, recipe file isn't available");
        }
        remoteProcess.readRecipeFile(output);
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
