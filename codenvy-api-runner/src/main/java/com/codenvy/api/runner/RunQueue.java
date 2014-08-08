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

import com.codenvy.api.builder.BuildStatus;
import com.codenvy.api.builder.BuilderService;
import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.DebugMode;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.api.runner.dto.RunnerServerAccessCriteria;
import com.codenvy.api.runner.dto.RunnerServerLocation;
import com.codenvy.api.runner.dto.RunnerServerRegistration;
import com.codenvy.api.runner.dto.RunnerState;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.runner.internal.RunnerEvent;
import com.codenvy.api.workspace.server.WorkspaceService;
import com.codenvy.api.workspace.shared.dto.WorkspaceDescriptor;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.lang.Pair;
import com.codenvy.commons.lang.concurrent.ThreadLocalPropagateContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Singleton
public class RunQueue {
    private static final Logger LOG = LoggerFactory.getLogger(RunQueue.class);

    /** Pause in milliseconds for checking the result of build process. */
    private static final long CHECK_BUILD_RESULT_DELAY     = 2000;
    private static final long CHECK_AVAILABLE_RUNNER_DELAY = 2000;

    private static final int DEFAULT_MAX_MEMORY_SIZE = 512;

    private static final int APPLICATION_CHECK_URL_TIMEOUT = 2000;
    private static final int APPLICATION_CHECK_URL_COUNT   = 30;

    private static final AtomicLong sequence = new AtomicLong(1);

    private final ConcurrentMap<String, RemoteRunnerServer>       runnerServices;
    private final RunnerSelectionStrategy                         runnerSelector;
    private final ConcurrentMap<ProjectWithWorkspace, RunnerList> runnerListMapping;
    private final ConcurrentMap<Long, RunQueueTask>               tasks;
    private final int                                             defMemSize;
    private final EventService                                    eventService;
    private final String                                          baseWorkspaceApiUrl;
    private final String                                          baseProjectApiUrl;
    private final String                                          baseBuilderApiUrl;
    private final int                                             defLifetime;
    private final long                                            maxWaitingTimeMillis;
    private final AtomicBoolean                                   started;
    private final long                                            appCleanupTime;
    // Helps to reduce lock contentions when check available resources.
    private final Lock[]                                          resourceCheckerLocks;
    private final int                                             resourceCheckerMask;

    private ExecutorService          executor;
    private ScheduledExecutorService scheduler;

    /** Optional pre-configured slave runners. */
    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_SLAVE_RUNNER_URLS)
    private String[] slaves = new String[0];

    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_WS_MAX_MEMORY_SIZE)
    private int defMaxMemorySize = DEFAULT_MAX_MEMORY_SIZE;

    /**
     * @param baseWorkspaceApiUrl
     *         workspace api url. Configuration parameter that points to the Workspace API location. If such parameter isn't specified than
     *         use the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy
     *         .com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find workspace API at URL:
     *         <i>http://codenvy.com/api/workspace/my_workspace</i>
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy
     *         .com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param baseBuilderApiUrl
     *         builder api url. Configuration parameter that points to the base Builder API location. If such parameter isn't specified
     *         than use the same base URL as runner API has, e.g. suppose we have runner API at URL:
     *         <i>http://codenvy.com/api/runner/my_workspace</i>, in this case base URL is <i>http://codenvy.com/api</i> so we will try to
     *         find builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>.
     * @param defMemSize
     *         default size of memory for application in megabytes. This value used is there is nothing specified in properties of project.
     * @param maxWaitingTime
     *         max time for request to be in queue in seconds
     * @param defLifetime
     *         default application life time in seconds. After this time the application may be terminated.
     */
    @Inject
    @SuppressWarnings("unchecked")
    public RunQueue(@Nullable @Named("workspace.base_api_url") String baseWorkspaceApiUrl,
                    @Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                    @Nullable @Named("builder.base_api_url") String baseBuilderApiUrl,
                    @Named(Constants.APP_DEFAULT_MEM_SIZE) int defMemSize,
                    @Named(Constants.WAITING_TIME) int maxWaitingTime,
                    @Named(Constants.APP_LIFETIME) int defLifetime,
                    @Named(Constants.APP_CLEANUP_TIME) int appCleanupTime,
                    RunnerSelectionStrategy runnerSelector,
                    EventService eventService) {
        this.baseWorkspaceApiUrl = baseWorkspaceApiUrl;
        this.baseProjectApiUrl = baseProjectApiUrl;
        this.baseBuilderApiUrl = baseBuilderApiUrl;
        this.defMemSize = defMemSize;
        this.eventService = eventService;
        this.maxWaitingTimeMillis = TimeUnit.SECONDS.toMillis(maxWaitingTime);
        this.defLifetime = defLifetime;
        this.runnerSelector = runnerSelector;
        this.appCleanupTime = TimeUnit.SECONDS.toMillis(appCleanupTime);

        runnerServices = new ConcurrentHashMap<>();
        tasks = new ConcurrentHashMap<>();
        runnerListMapping = new ConcurrentHashMap<>();
        started = new AtomicBoolean(false);
        final int partitions = 1 << 4;
        resourceCheckerMask = partitions - 1;
        resourceCheckerLocks = new Lock[partitions];
        for (int i = 0; i < partitions; i++) {
            resourceCheckerLocks[i] = new ReentrantLock();
        }
    }

    public RunQueueTask run(String wsId, String project, ServiceContext serviceContext, RunOptions runOptions) throws RunnerException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(wsId, project, serviceContext);
        final User user = EnvironmentContext.getCurrent().getUser();
        final RunRequest request = DtoFactory.getInstance().createDto(RunRequest.class)
                                             .withWorkspace(wsId)
                                             .withProject(project)
                                             .withProjectDescriptor(descriptor)
                                             .withUserName(user == null ? "" : user.getName());
        BuildOptions buildOptions = null;
        if (runOptions != null) {
            request.setMemorySize(runOptions.getMemorySize());
            request.setOptions(runOptions.getOptions());
            request.setShellOptions(runOptions.getShellOptions());
            request.setEnvironmentId(runOptions.getEnvironmentId());
            if (runOptions.getDebugMode() != null) {
                request.setDebugMode(DtoFactory.getInstance().createDto(DebugMode.class).withMode(runOptions.getDebugMode().getMode()));
            }
            buildOptions = runOptions.getBuildOptions();
        }
        final Map<String, List<String>> projectAttributes = descriptor.getAttributes();

        String runner = request.getRunner();
        if (runner == null) {
            runner = getProjectAttributeValue(Constants.RUNNER_CUSTOM_LAUNCHER, projectAttributes);
            if (runner == null) {
                runner = getProjectAttributeValue(Constants.RUNNER_NAME, projectAttributes);
            }
            if (runner == null) {
                throw new RunnerException(
                        String.format("Name of runner is not specified, be sure property of project %s is set", Constants.RUNNER_NAME));
            }
            request.setRunner(runner);
        }
        if (!hasRunner(request)) {
            throw new RunnerException(String.format("Runner '%s' is not available. ", runner));
        }

        String runnerEnvId = request.getEnvironmentId();
        if (runnerEnvId == null) {
            runnerEnvId = getProjectAttributeValue(Constants.RUNNER_ENV_ID, projectAttributes);
            request.setEnvironmentId(runnerEnvId);
        }
        request.setRunnerScriptUrls(getRunnerScript(descriptor));
        if (request.getDebugMode() == null) {
            final String debugAttr = getProjectAttributeValue(Constants.RUNNER_DEBUG_MODE.replace("${runner}", runner), projectAttributes);
            if (debugAttr != null) {
                request.setDebugMode(DtoFactory.getInstance().createDto(DebugMode.class).withMode(debugAttr));
            }
        }

        final WorkspaceDescriptor workspace = getWorkspaceDescriptor(wsId, serviceContext);

        int mem = request.getMemorySize();
        if (mem <= 0) {
            final String memAttr = getProjectAttributeValue(Constants.RUNNER_MEMORY_SIZE.replace("${runner}", runner), projectAttributes);
            mem = memAttr != null ? Integer.parseInt(memAttr) : defMemSize;
        }
        request.setMemorySize(mem);

        checkResources(workspace, request);

        final String lifetimeAttr = workspace.getAttributes().get(Constants.RUNNER_LIFETIME);
        int lifetime = lifetimeAttr != null ? Integer.parseInt(lifetimeAttr) : defLifetime;
        if (lifetime <= 0) {
            lifetime = Integer.MAX_VALUE;
        }
        request.setLifetime(lifetime);

        final List<String> optionsAttr = projectAttributes.get(Constants.RUNNER_OPTIONS.replace("${runner}", runner));
        if (optionsAttr != null && !optionsAttr.isEmpty()) {
            final Map<String, String> options = request.getOptions();
            for (String str : optionsAttr) {
                if (str != null) {
                    final String[] pair = str.split("=");
                    if (!options.containsKey(pair[0])) {
                        options.put(pair[0], pair.length > 1 ? pair[1] : null);
                    }
                }
            }
        }

        boolean skipBuild = runOptions != null && runOptions.getSkipBuild();
        final ValueHolder<BuildTaskDescriptor> buildTaskHolder = skipBuild ? null : new ValueHolder<BuildTaskDescriptor>();
        final Callable<RemoteRunnerProcess> callable;
        if (!skipBuild
            && ((buildOptions != null && buildOptions.getBuilderName() != null)
                || getProjectAttributeValue(com.codenvy.api.builder.internal.Constants.BUILDER_NAME, descriptor.getAttributes()) != null)) {
            LOG.debug("Need build project first");
            if (buildOptions == null) {
                buildOptions = DtoFactory.getInstance().createDto(BuildOptions.class);
            }
            // We want bundle of application with all dependencies (libraries) that application needs.
            buildOptions.setIncludeDependencies(true);
            buildOptions.setSkipTest(true);
            final RemoteServiceDescriptor builderService = getBuilderServiceDescriptor(wsId, serviceContext);
            // schedule build
            final BuildTaskDescriptor buildDescriptor;
            try {
                final Link buildLink = builderService.getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_BUILD);
                if (buildLink == null) {
                    throw new RunnerException("You requested a run and your project has not been built." +
                                              " The runner was unable to get the proper build URL to initiate a build.");
                }
                buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class, buildLink, buildOptions, Pair.of("project", project));
            } catch (IOException e) {
                throw new RunnerException(e);
            } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
                throw new RunnerException(e.getServiceError());
            }
            callable = createTaskFor(buildDescriptor, request, buildTaskHolder);
        } else {
            final Link zipballLink = getLink(com.codenvy.api.project.server.Constants.LINK_REL_EXPORT_ZIP, descriptor.getLinks());
            if (zipballLink != null) {
                final String zipballLinkHref = zipballLink.getHref();
                final String token = getAuthenticationToken();
                request.setDeploymentSourcesUrl(token != null ? String.format("%s?token=%s", zipballLinkHref, token) : zipballLinkHref);
            }
            callable = createTaskFor(null, request, buildTaskHolder);
        }
        final Long id = sequence.getAndIncrement();
        final RunFutureTask future = new RunFutureTask(ThreadLocalPropagateContext.wrap(callable), id, wsId, project);
        request.setId(id); // for getting callback events from remote runner
        final RunQueueTask task = new RunQueueTask(id, request, maxWaitingTimeMillis, future, buildTaskHolder,
                                                   serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        eventService.publish(RunnerEvent.queueStartedEvent(id, wsId, project));
        executor.execute(future);
        return task;
    }

    private void checkResources(WorkspaceDescriptor workspace, RunRequest request) throws RunnerException {
        final String wsId = workspace.getId();
        final int index = wsId.hashCode() & resourceCheckerMask;
        // Lock to be sure other threads don't try to start application in the same workspace.
        resourceCheckerLocks[index].lock();
        try {
            final String availableMemAttr = workspace.getAttributes().get(Constants.RUNNER_MAX_MEMORY_SIZE);
            final int availableMem = availableMemAttr != null ? Integer.parseInt(availableMemAttr) : defMaxMemorySize;
            if (availableMem < request.getMemorySize()) {
                throw new RunnerException(
                        String.format("Not enough resources to start application. Available memory %dM but %dM required. ",
                                      availableMem < 0 ? 0 : availableMem, request.getMemorySize())
                );
            }
            checkMemory(wsId, availableMem, request.getMemorySize());
        } finally {
            resourceCheckerLocks[index].unlock();
        }
    }

    private void checkMemory(String wsId, int availableMem, int mem) throws RunnerException {
        for (RunQueueTask task : tasks.values()) {
            final RunRequest request = task.getRequest();
            if (wsId.equals(request.getWorkspace())) {
                try {
                    ApplicationStatus status;
                    if (task.isWaiting()
                        || (status = task.getRemoteProcess().getApplicationProcessDescriptor().getStatus()) == ApplicationStatus.RUNNING
                        || status == ApplicationStatus.NEW) {
                        availableMem -= request.getMemorySize();
                        if (availableMem <= 0) {
                            throw new RunnerException(
                                    String.format("Not enough resources to start application. Available memory %dM but %dM required. ",
                                                  availableMem < 0 ? 0 : availableMem, mem)
                            );
                        }
                    }
                } catch (NotFoundException ignored) {
                    // If remote process is not found, it is stopped and removed from remote server.
                }
            }
        }
    }

    protected Callable<RemoteRunnerProcess> createTaskFor(final BuildTaskDescriptor buildDescriptor,
                                                          final RunRequest request,
                                                          final ValueHolder<BuildTaskDescriptor> buildTaskHolder) {
        return new Callable<RemoteRunnerProcess>() {
            @Override
            public RemoteRunnerProcess call() throws Exception {
                if (buildDescriptor != null) {
                    final Link buildStatusLink = getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_GET_STATUS,
                                                         buildDescriptor.getLinks());
                    if (buildStatusLink == null) {
                        throw new RunnerException("Invalid response from builder service. Unable get URL for checking build status");
                    }
                    for (; ; ) {
                        if (Thread.currentThread().isInterrupted()) {
                            // Expected to get here if task is canceled. Try to cancel related build process.
                            tryCancelBuild(buildDescriptor);
                            return null;
                        }
                        synchronized (this) {
                            try {
                                wait(CHECK_BUILD_RESULT_DELAY);
                            } catch (InterruptedException e) {
                                // Expected to get here if task is canceled. Try to cancel related build process.
                                tryCancelBuild(buildDescriptor);
                                return null;
                            }
                        }
                        BuildTaskDescriptor buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class,
                                                                                     // create copy of link when pass it outside!!
                                                                                     DtoFactory.getInstance().clone(buildStatusLink));
                        if (buildTaskHolder != null) {
                            buildTaskHolder.set(buildDescriptor);
                        }
                        switch (buildDescriptor.getStatus()) {
                            case SUCCESSFUL:
                                final Link downloadLink = getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_DOWNLOAD_RESULT,
                                                                  buildDescriptor.getLinks());
                                if (downloadLink == null) {
                                    throw new RunnerException("Unable start application. Application build is successful but there " +
                                                              "is no URL for download result of build.");
                                }
                                final String downloadLinkHref = downloadLink.getHref();
                                final String token = getAuthenticationToken();
                                request.withDeploymentSourcesUrl(
                                        token != null ? String.format("%s&token=%s", downloadLinkHref, token) : downloadLinkHref);
                                return getRunner(request).run(request);
                            case CANCELLED:
                            case FAILED:
                                String msg = "Unable start application. Build of application is failed or cancelled.";
                                final Link logLink = getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_VIEW_LOG,
                                                             buildDescriptor.getLinks());
                                if (logLink != null) {
                                    msg += (" Build logs: " + logLink.getHref());
                                }
                                throw new RunnerException(msg);
                            case IN_PROGRESS:
                            case IN_QUEUE:
                                // wait
                                break;
                        }
                    }
                } else {
                    return getRunner(request).run(request);
                }
            }
        };
    }

    private List<String> getRunnerScript(ProjectDescriptor projectDescriptor) {
        final String projectUrl = projectDescriptor.getBaseUrl();
        final String projectPath = projectDescriptor.getPath();
        final String authToken = getAuthenticationToken();
        final List<String> attrs = projectDescriptor.getAttributes().get(Constants.RUNNER_SCRIPT_FILES);
        if (attrs == null) {
            return Collections.emptyList();
        }
        final List<String> scripts = new ArrayList<>(attrs.size());
        for (String attr : attrs) {
            scripts.add(projectUrl.replace(projectPath, String.format("/file%s/%s?token=%s", projectPath, attr, authToken)));
        }
        return scripts;
    }

    private RemoteServiceDescriptor getBuilderServiceDescriptor(String workspace, ServiceContext serviceContext) {
        final UriBuilder baseBuilderUriBuilder = baseBuilderApiUrl == null || baseBuilderApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseBuilderApiUrl);
        final String builderUrl = baseBuilderUriBuilder.path(BuilderService.class).build(workspace).toString();
        return new RemoteServiceDescriptor(builderUrl);
    }

    private WorkspaceDescriptor getWorkspaceDescriptor(String workspace, ServiceContext serviceContext) throws RunnerException {
        final UriBuilder baseWorkspaceUriBuilder = baseWorkspaceApiUrl == null || baseWorkspaceApiUrl.isEmpty()
                                                   ? serviceContext.getBaseUriBuilder()
                                                   : UriBuilder.fromUri(baseWorkspaceApiUrl);
        final String workspaceUrl = baseWorkspaceUriBuilder.path(WorkspaceService.class)
                                                           .path(WorkspaceService.class, "getById")
                                                           .build(workspace).toString();
        try {
            return HttpJsonHelper.get(WorkspaceDescriptor.class, workspaceUrl);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, ServiceContext serviceContext)
            throws RunnerException {
        final UriBuilder baseProjectUriBuilder = baseProjectApiUrl == null || baseProjectApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseProjectApiUrl);
        final String projectUrl = baseProjectUriBuilder.path(ProjectService.class)
                                                       .path(ProjectService.class, "getProject")
                                                       .build(workspace, project.startsWith("/") ? project.substring(1) : project)
                                                       .toString();
        try {
            return HttpJsonHelper.get(ProjectDescriptor.class, projectUrl);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    private static Link getLink(String rel, List<Link> links) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private static String getProjectAttributeValue(String name, Map<String, List<String>> attributes) {
        final List<String> list = attributes.get(name);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private boolean tryCancelBuild(BuildTaskDescriptor buildDescriptor) {
        final Link cancelLink = getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_CANCEL, buildDescriptor.getLinks());
        if (cancelLink == null) {
            LOG.error("Can't cancel build process since cancel link is not available.");
            return false;
        } else {
            try {
                final BuildTaskDescriptor result = HttpJsonHelper.request(BuildTaskDescriptor.class,
                                                                          // create copy of link when pass it outside!!
                                                                          DtoFactory.getInstance().clone(cancelLink));
                LOG.debug("Build cancellation result {}", result);
                return result != null && result.getStatus() == BuildStatus.CANCELLED;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return false;
            }
        }
    }

    public RunQueueTask getTask(Long id) throws NotFoundException {
        checkStarted();
        final RunQueueTask task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException(String.format("Not found task %d. It may be canceled by timeout.", id));
        }
        return task;
    }

    public List<? extends RunQueueTask> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                              new NamedThreadFactory("RunQueue-", true)) {
                @Override
                protected void afterExecute(Runnable runnable, Throwable error) {
                    super.afterExecute(runnable, error);
                    if (runnable instanceof RunFutureTask) {
                        final RunFutureTask runFutureTask = (RunFutureTask)runnable;
                        if (error == null) {
                            try {
                                runFutureTask.get();
                            } catch (CancellationException e) {
                                error = e;
                            } catch (ExecutionException e) {
                                error = e.getCause();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        if (error != null) {
                            LOG.error(error.getMessage(), error);
                            eventService.publish(RunnerEvent.errorEvent(runFutureTask.id, runFutureTask.workspace, runFutureTask.project,
                                                                        error.getMessage()));
                        }
                    }
                }
            };
            scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("RunQueueScheduler-", true));
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    int num = 0;
                    int waitingNum = 0;
                    for (Iterator<RunQueueTask> i = tasks.values().iterator(); i.hasNext(); ) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        final RunQueueTask task = i.next();
                        final boolean waiting = task.isWaiting();
                        final RunRequest request = task.getRequest();
                        if (waiting) {
                            if ((task.getCreationTime() + maxWaitingTimeMillis) < System.currentTimeMillis()) {
                                try {
                                    task.cancel();
                                    eventService.publish(
                                            RunnerEvent.queueTerminatedEvent(task.getId(), request.getWorkspace(), request.getProject()));
                                } catch (Exception e) {
                                    LOG.warn(e.getMessage(), e);
                                }
                                i.remove();
                                waitingNum++;
                                num++;
                            }
                        } else {
                            RemoteRunnerProcess remote = null;
                            try {
                                remote = task.getRemoteProcess();
                            } catch (Exception e) {
                                LOG.warn(e.getMessage(), e);
                            }
                            if (remote == null) {
                                i.remove();
                                num++;
                            } else if ((remote.getCreationTime() + request.getLifetime() + appCleanupTime) < System.currentTimeMillis()) {
                                try {
                                    remote.getApplicationProcessDescriptor();
                                } catch (NotFoundException e) {
                                    i.remove();
                                    num++;
                                } catch (Exception e) {
                                    LOG.warn(e.getMessage(), e);
                                    i.remove();
                                    num++;
                                }
                            }
                        }
                    }
                    if (num > 0) {
                        LOG.debug("Remove {} expired tasks, {} of them were waiting for processing", num, waitingNum);
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);

            eventService.subscribe(new EventSubscriber<RunnerEvent>() {
                @Override
                public void onEvent(RunnerEvent event) {
                    try {
                        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                        final long id = event.getProcessId();
                        switch (event.getType()) {
                            case STARTED:
                            case STOPPED:
                            case ERROR:
                                bm.setChannel(String.format("runner:status:%d", id));
                                try {
                                    final ApplicationProcessDescriptor descriptor = getTask(id).getDescriptor();
                                    bm.setBody(DtoFactory.getInstance().toJson(descriptor));
                                    if (event.getType() == RunnerEvent.EventType.STARTED) {
                                        final List<Link> links = descriptor.getLinks();
                                        final Link appLink = getLink(Constants.LINK_REL_WEB_URL, links);
                                        if (appLink != null) {
                                            executor.execute(new ApplicationUrlChecker(id,
                                                                                       new URL(appLink.getHref()),
                                                                                       APPLICATION_CHECK_URL_TIMEOUT,
                                                                                       APPLICATION_CHECK_URL_COUNT));
                                        }
                                    }
                                } catch (RunnerException re) {
                                    bm.setType(ChannelBroadcastMessage.Type.ERROR);
                                    bm.setBody(String.format("{\"message\":%s}", JsonUtils.getJsonString(re.getMessage())));
                                } catch (NotFoundException re) {
                                    // task was not create in some reason in this case post error message directly
                                    bm.setType(ChannelBroadcastMessage.Type.ERROR);
                                    bm.setBody(String.format("{\"message\":%s}", JsonUtils.getJsonString(event.getError())));
                                }
                                break;
                            case RUN_TASK_QUEUE_TIME_EXCEEDED:
                                bm.setChannel(String.format("runner:status:%d", id));
                                bm.setType(ChannelBroadcastMessage.Type.ERROR);
                                bm.setBody(String.format("{\"message\":%s}",
                                                         "Unable to start application, currently there are no resources to start your application. Max waiting time for available resources has been reached. Contact support for assistance."));
                                break;
                            case MESSAGE_LOGGED:
                                final RunnerEvent.LoggedMessage message = event.getMessage();
                                if (message != null) {
                                    bm.setChannel(String.format("runner:output:%d", id));
                                    bm.setBody(String.format("{\"num\":%d, \"line\":%s}",
                                                             message.getLineNum(), JsonUtils.getJsonString(message.getMessage())));
                                }
                                break;
                        }
                        WSConnectionContext.sendMessage(bm);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            });

            eventService.subscribe(new EventSubscriber<RunnerEvent>() { //Log events for analytics
                @Override
                public void onEvent(RunnerEvent event) {
                    try {
                        final long id = event.getProcessId();
                        final RunQueueTask task = getTask(id);
                        final RunRequest request = task.getRequest();
                        final String analyticsID = task.getCreationTime() + "-" + id;
                        final String project = event.getProject();
                        final String workspace = request.getWorkspace();
                        final String projectTypeId = request.getProjectDescriptor().getProjectTypeId();
                        final boolean debug = request.getDebugMode() != null;
                        final String user = request.getUserName();
                        switch (event.getType()) {
                            case STARTED:
                                LOG.info("EVENT#run-queue-waiting-finished# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user,
                                         project, projectTypeId, analyticsID);
                                if (debug) {
                                    LOG.info("EVENT#debug-started# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user, project,
                                             projectTypeId, analyticsID);
                                } else {
                                    LOG.info("EVENT#run-started# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user, project,
                                             projectTypeId, analyticsID);
                                }
                                break;
                            case STOPPED:
                                if (debug) {
                                    LOG.info("EVENT#debug-finished# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user, project,
                                             projectTypeId, analyticsID);
                                } else {
                                    LOG.info("EVENT#run-finished# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user, project,
                                             projectTypeId, analyticsID);
                                }
                                break;
                            case RUN_TASK_ADDED_IN_QUEUE:
                                LOG.info("EVENT#run-queue-waiting-started# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user,
                                         project, projectTypeId, analyticsID);
                                break;
                            case RUN_TASK_QUEUE_TIME_EXCEEDED:
                                LOG.info("EVENT#run-queue-terminated# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#", workspace, user,
                                         project, projectTypeId, analyticsID);
                                break;
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            });

            if (slaves.length > 0) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final LinkedList<RemoteRunnerServer> servers = new LinkedList<>();
                        for (String slave : slaves) {
                            try {
                                servers.add(new RemoteRunnerServer(slave));
                            } catch (IllegalArgumentException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                        final LinkedList<RemoteRunnerServer> offline = new LinkedList<>();
                        for (; ; ) {
                            while (!servers.isEmpty()) {
                                if (Thread.currentThread().isInterrupted()) {
                                    return;
                                }
                                final RemoteRunnerServer server = servers.pop();
                                if (server.isAvailable()) {
                                    try {
                                        doRegisterRunnerServer(server);
                                        LOG.debug("Pre-configured slave runner server {} registered. ", server.getBaseUrl());
                                    } catch (RunnerException e) {
                                        LOG.error(e.getMessage(), e);
                                        offline.add(server);
                                    }
                                } else {
                                    LOG.warn("Pre-configured slave runner server {} isn't responding. ", server.getBaseUrl());
                                    offline.add(server);
                                }
                            }
                            if (offline.isEmpty()) {
                                return;
                            } else {
                                servers.addAll(offline);
                                offline.clear();
                                synchronized (this) {
                                    try {
                                        wait(5000);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("The runner has not started yet and there is a delay.");
        }
    }

    @PreDestroy
    public void stop() {
        if (started.compareAndSet(true, false)) {
            boolean interrupted = false;
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate scheduler");
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        LOG.warn("Unable terminate main pool");
                    }
                }
            } catch (InterruptedException e) {
                interrupted |= true;
                executor.shutdownNow();
            }
            tasks.clear();
            runnerListMapping.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    protected EventService getEventService() {
        return eventService;
    }

    public List<RemoteRunnerServer> getRegisterRunnerServers() {
        return new ArrayList<>(runnerServices.values());
    }

    /**
     * Register remote SlaveRunnerService which can process run application.
     *
     * @param registration
     *         RunnerServerRegistration
     * @return {@code true} if set of available Runners changed as result of the call
     * if we access remote SlaveRunnerService successfully but get error response
     * @throws RunnerException
     *         if an error occurs
     */
    public boolean registerRunnerServer(RunnerServerRegistration registration) throws RunnerException {
        checkStarted();
        final String url = registration.getRunnerServerLocation().getUrl();
        final RemoteRunnerServer runnerServer = new RemoteRunnerServer(url);
        String workspace = null;
        String project = null;
        final RunnerServerAccessCriteria accessCriteria = registration.getRunnerServerAccessCriteria();
        if (accessCriteria != null) {
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }
        if (workspace != null) {
            runnerServer.setAssignedWorkspace(workspace);
            if (project != null) {
                runnerServer.setAssignedProject(project);
            }
        }
        return doRegisterRunnerServer(runnerServer);
    }

    private boolean doRegisterRunnerServer(RemoteRunnerServer runnerServer) throws RunnerException {
        final List<RemoteRunner> toAdd = new LinkedList<>();
        for (RunnerDescriptor runnerDescriptor : runnerServer.getAvailableRunners()) {
            toAdd.add(runnerServer.createRemoteRunner(runnerDescriptor));
        }
        runnerServices.put(runnerServer.getBaseUrl(), runnerServer);
        return registerRunners(runnerServer.getAssignedWorkspace(), runnerServer.getAssignedProject(), toAdd);
    }

    protected boolean registerRunners(String workspace, String project, List<RemoteRunner> toAdd) {
        final ProjectWithWorkspace key = new ProjectWithWorkspace(project, workspace);
        RunnerList runnerList = runnerListMapping.get(key);
        if (runnerList == null) {
            final RunnerList newRunnerList = new RunnerList();
            runnerList = runnerListMapping.putIfAbsent(key, newRunnerList);
            if (runnerList == null) {
                runnerList = newRunnerList;
            }
        }
        return runnerList.addRunners(toAdd);
    }

    /**
     * Unregister remote SlaveRunnerService.
     *
     * @param location
     *         RunnerServerLocation
     * @return {@code true} if set of available Runners changed as result of the call
     * if we access remote SlaveRunnerService successfully but get error response
     * @throws RunnerException
     *         if an error occurs
     */
    public boolean unregisterRunnerServer(RunnerServerLocation location) throws RunnerException {
        checkStarted();
        final String url = location.getUrl();
        if (url == null) {
            return false;
        }
        final RemoteRunnerServer runnerService = runnerServices.remove(url);
        if (runnerService == null) {
            return false;
        }
        final List<RemoteRunner> toRemove = new LinkedList<>();
        for (RunnerDescriptor runnerDescriptor : runnerService.getAvailableRunners()) {
            toRemove.add(runnerService.createRemoteRunner(runnerDescriptor));
        }
        return unregisterRunners(toRemove);
    }

    protected boolean unregisterRunners(List<RemoteRunner> toRemove) {
        boolean modified = false;
        for (Iterator<RunnerList> i = runnerListMapping.values().iterator(); i.hasNext(); ) {
            final RunnerList runnerList = i.next();
            if (runnerList.removeRunners(toRemove)) {
                modified |= true;
                if (runnerList.size() == 0) {
                    i.remove();
                }
            }
        }
        return modified;
    }

    boolean hasRunner(RunRequest request) {
        final RunnerList runnerList = getRunnerList(request.getWorkspace(), request.getProject());
        return runnerList != null && runnerList.hasRunner(request.getRunner());
    }

    private RunnerList getRunnerList(String workspace, String project) {
        RunnerList runnerList = runnerListMapping.get(new ProjectWithWorkspace(project, workspace));
        if (runnerList == null) {
            if (project != null || workspace != null) {
                if (workspace != null) {
                    // have dedicated runners for whole workspace (omit project) ?
                    runnerList = runnerListMapping.get(new ProjectWithWorkspace(null, workspace));
                }
                if (runnerList == null) {
                    // seems there is no dedicated runners for specified request, use shared one then
                    runnerList = runnerListMapping.get(new ProjectWithWorkspace(null, null));
                }
            }
        }
        return runnerList;
    }

    protected RemoteRunner getRunner(RunRequest request) throws RunnerException {
        RunnerList runnerList = getRunnerList(request.getWorkspace(), request.getProject());
        if (runnerList == null) {
            // Can't continue, typically should never happen. At least shared runners should be available for everyone.
            throw new RunnerException("There is no any runner to process this request. ");
        }
        final RemoteRunner runner = runnerList.getRunner(request);
        if (runner == null) {
            throw new RunnerException("There is no any runner to process this request. ");
        }
        LOG.debug("Use slave runner {} at {}", runner.getName(), runner.getBaseUrl());
        return runner;
    }

    private String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

    private static class ApplicationUrlChecker implements Runnable {
        final long taskId;
        final URL  url;
        final int  healthCheckerTimeout;
        final int  healthCheckAttempts;

        ApplicationUrlChecker(long taskId, URL url, int healthCheckerTimeout, int healthCheckAttempts) {
            this.taskId = taskId;
            this.url = url;
            this.healthCheckerTimeout = healthCheckerTimeout;
            this.healthCheckAttempts = healthCheckAttempts;
        }

        @Override
        public void run() {
            boolean ok = false;
            for (int i = 0; !ok && i < healthCheckAttempts; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    Thread.sleep(healthCheckerTimeout);
                } catch (InterruptedException e) {
                    return;
                }
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);
                    conn.getResponseCode();
                    if (200 == conn.getResponseCode()) {
                        ok = true;
                        LOG.debug("Application URL '{}' - OK", url);
                        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                        bm.setChannel(String.format("runner:app_health:%d", taskId));
                        bm.setBody(String.format("{\"url\":%s,\"status\":\"%s\"}", JsonUtils.getJsonString(url.toString()), "OK"));
                        try {
                            WSConnectionContext.sendMessage(bm);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                } catch (IOException ignored) {
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }
    }

    private static class RunFutureTask extends FutureTask<RemoteRunnerProcess> {
        final Long   id;
        final String workspace;
        final String project;

        RunFutureTask(Callable<RemoteRunnerProcess> callable, Long id, String workspace, String project) {
            super(callable);
            this.id = id;
            this.workspace = workspace;
            this.project = project;
        }
    }

    private static class ProjectWithWorkspace {
        final String project;
        final String workspace;

        ProjectWithWorkspace(String project, String workspace) {
            this.project = project;
            this.workspace = workspace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProjectWithWorkspace)) {
                return false;
            }
            ProjectWithWorkspace other = (ProjectWithWorkspace)o;
            return (workspace == null ? other.workspace == null : workspace.equals(other.workspace))
                   && (project == null ? other.project == null : project.equals(other.project));

        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + (workspace == null ? 0 : workspace.hashCode());
            hash = hash * 31 + (project == null ? 0 : project.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            return "ProjectWithWorkspace{" +
                   "workspace='" + workspace + '\'' +
                   ", project='" + project + '\'' +
                   '}';
        }
    }

    private class RunnerList {
        final Set<RemoteRunner> runners;

        RunnerList() {
            runners = new LinkedHashSet<>();
        }

        synchronized boolean hasRunner(String name) {
            for (RemoteRunner runner : runners) {
                if (name.equals(runner.getName())) {
                    return true;
                }
            }
            return false;
        }

        synchronized boolean addRunners(Collection<? extends RemoteRunner> list) {
            if (runners.addAll(list)) {
                notifyAll();
                return true;
            }
            return false;
        }

        synchronized boolean removeRunners(Collection<? extends RemoteRunner> list) {
            if (runners.removeAll(list)) {
                notifyAll();
                return true;
            }
            return false;
        }

        synchronized int size() {
            return runners.size();
        }

        synchronized RemoteRunner getRunner(RunRequest request) {
            final List<RemoteRunner> matched = new LinkedList<>();
            for (RemoteRunner runner : runners) {
                if (request.getRunner().equals(runner.getName())) {
                    matched.add(runner);
                }
            }
            if (matched.isEmpty()) {
                return null;
            }
            // List of runners that have enough resources for launch application.
            final List<RemoteRunner> available = new LinkedList<>();
            int attemptGetState = 0;
            for (; ; ) {
                for (RemoteRunner runner : matched) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null; // stop immediately
                    }
                    RunnerState runnerState;
                    try {
                        runnerState = runner.getRemoteRunnerState();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        ++attemptGetState;
                        if (attemptGetState > 10) {
                            return null;
                        }
                        continue;
                    }
                    if (runnerState.getServerState().getFreeMemory() >= request.getMemorySize()) {
                        available.add(runner);
                    }
                }
                if (available.isEmpty()) {
                    try {
                        wait(CHECK_AVAILABLE_RUNNER_DELAY); // wait and try again
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null; // expected to get here if task is canceled
                    }
                } else {
                    if (available.size() > 0) {
                        return runnerSelector.select(available);
                    }
                    return available.get(0);
                }
            }
        }
    }
}
