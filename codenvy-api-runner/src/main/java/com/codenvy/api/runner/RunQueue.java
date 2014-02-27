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

import com.codenvy.api.builder.BuildStatus;
import com.codenvy.api.builder.BuilderService;
import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.api.runner.dto.RunnerServiceAccessCriteria;
import com.codenvy.api.runner.dto.RunnerServiceLocation;
import com.codenvy.api.runner.dto.RunnerServiceRegistration;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.runner.internal.dto.DebugMode;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.dto.RunnerState;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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

    private final RunnerSelectionStrategy                  runnerSelector;
    private final ExecutorService                          executor;
    private final ConcurrentMap<RunnerListKey, RunnerList> runnerListMapping;
    private final ConcurrentMap<Long, RunQueueTask>        tasks;
    private final int                                      defMemSize;
    private final String                                   baseProjectApiUrl;
    private final String                                   baseBuilderApiUrl;
    private final int                                      appLifetime;
    private final long                                     maxTimeInQueueMillis;

    private boolean started;

    /**
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy.com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param baseBuilderApiUrl
     *         builder api url. Configuration parameter that points to the base Builder API location. If such parameter isn't specified
     *         than use the same base URL as runner API has, e.g. suppose we have runner API at URL:
     *         <i>http://codenvy.com/api/runner/my_workspace</i>, in this case base URL is <i>http://codenvy.com/api</i> so we will try to
     *         find builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>.
     * @param defMemSize
     *         default size of memory for application in megabytes. This value used is there is nothing specified in properties of project.
     * @param maxTimeInQueue
     *         max time for request to be in queue in seconds
     * @param appLifetime
     *         application life time in seconds. After this time the application may be terminated.
     */
    @Inject
    public RunQueue(@Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                    @Nullable @Named("builder.base_api_url") String baseBuilderApiUrl,
                    @Named("runner.default_app_mem_size") int defMemSize,
                    @Named("runner.queue.max_time_in_queue") int maxTimeInQueue,
                    @Named("runner.queue.app_lifetime") int appLifetime,
                    RunnerSelectionStrategy runnerSelector) {
        this.baseProjectApiUrl = baseProjectApiUrl;
        this.baseBuilderApiUrl = baseBuilderApiUrl;
        this.defMemSize = defMemSize;
        this.maxTimeInQueueMillis = TimeUnit.SECONDS.toMillis(maxTimeInQueue);
        this.appLifetime = appLifetime;
        this.runnerSelector = runnerSelector;
        tasks = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("RunQueue-", true));
        runnerListMapping = new ConcurrentHashMap<>();
    }

    public RunQueueTask run(String workspace, String project, ServiceContext serviceContext, RunOptions options)
            throws IOException, RemoteException, RunnerException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, serviceContext);
        final RunRequest request = DtoFactory.getInstance().createDto(RunRequest.class)
                                             .withWorkspace(workspace)
                                             .withProject(project)
                                             .withProjectDescriptor(descriptor);
        BuildOptions buildOptions = null;
        if (options != null) {
            request.setRunner(options.getRunnerName());
            request.setOptions(options.getOptions());
            request.setDebugMode(options.getDebugMode());
            buildOptions = options.getBuildOptions();
        }
        addRequestParameters(descriptor, request);
        final Callable<RemoteRunnerProcess> callable;
        if ((buildOptions != null && buildOptions.getBuilderName() != null)
            || descriptor.getAttributes().get(com.codenvy.api.builder.internal.Constants.BUILDER_NAME) != null) {
            LOG.debug("Need build project first");
            final RemoteServiceDescriptor builderService = getBuilderServiceDescriptor(workspace, serviceContext);
            final Link buildLink = builderService.getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_BUILD);
            if (buildLink == null) {
                throw new RunnerException("Unable get URL for starting build of the application");
            }
            // schedule build
            final BuildTaskDescriptor buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class,
                                                                               buildLink,
                                                                               buildOptions,
                                                                               Pair.of("project", project));
            callable = createTaskFor(buildDescriptor, request);
        } else {
            final Link zipballLink = getLink(com.codenvy.api.project.server.Constants.LINK_REL_EXPORT_ZIP, descriptor);
            if (zipballLink != null) {
                request.setDeploymentSourcesUrl(zipballLink.getHref());
            }
            callable = createTaskFor(null, request);
        }
        final FutureTask<RemoteRunnerProcess> future = new FutureTask<>(callable);
        final RunQueueTask task = new RunQueueTask(request, future);
        request.setWebHookUrl(serviceContext.getServiceUriBuilder().path(RunnerService.class, "webhook").build(workspace,
                                                                                                               task.getId()).toString());
        purgeExpiredTasks();
        tasks.put(task.getId(), task);
        executor.execute(future);
        return task;
    }

    private Link getLink(String rel, ProjectDescriptor descriptor) {
        for (Link link : descriptor.getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, ServiceContext serviceContext)
            throws IOException, RemoteException {
        final UriBuilder baseProjectUriBuilder = baseProjectApiUrl == null || baseProjectApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseProjectApiUrl);
        final String projectUrl = baseProjectUriBuilder.path(ProjectService.class)
                                                       .path(ProjectService.class, "getProject")
                                                       .build(workspace, project.startsWith("/") ? project.substring(1) : project)
                                                       .toString();
        return HttpJsonHelper.get(ProjectDescriptor.class, projectUrl);
    }

    private RemoteServiceDescriptor getBuilderServiceDescriptor(String workspace, ServiceContext serviceContext) {
        final UriBuilder baseBuilderUriBuilder = baseBuilderApiUrl == null || baseBuilderApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseBuilderApiUrl);
        final String builderUrl = baseBuilderUriBuilder.path(BuilderService.class).build(workspace).toString();
        return new RemoteServiceDescriptor(builderUrl);
    }

    private void addRequestParameters(ProjectDescriptor descriptor, RunRequest request) {
        String runner = request.getRunner();
        if (runner == null || runner.isEmpty()) {
            List<String> runnerAttribute = descriptor.getAttributes().get(Constants.RUNNER_NAME);
            if (runnerAttribute == null || runnerAttribute.isEmpty() || (runner = runnerAttribute.get(0)) == null) {
                throw new IllegalStateException(
                        String.format("Name of runner is not specified, be sure property of project %s is set", Constants.RUNNER_NAME));
            }
            request.setRunner(runner);
        }
        final String runDebugMode = Constants.RUNNER_DEBUG_MODE.replace("${runner}", runner);
        final String runMemSize = Constants.RUNNER_MEMORY_SIZE.replace("${runner}", runner);
        final String runOptions = Constants.RUNNER_OPTIONS.replace("${runner}", runner);

        for (Map.Entry<String, List<String>> entry : descriptor.getAttributes().entrySet()) {
            if (runDebugMode.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty() && request.getDebugMode() == null) {
                    request.setDebugMode(DtoFactory.getInstance().createDto(DebugMode.class).withMode(entry.getValue().get(0)));
                }
            } else if (runMemSize.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    request.setMemorySize(Integer.parseInt(entry.getValue().get(0)));
                }
            } else if (runOptions.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    final Map<String, String> options = request.getOptions();
                    for (String str : entry.getValue()) {
                        if (str != null) {
                            final String[] pair = str.split("=");
                            if (!options.containsKey(pair[0])) {
                                if (pair.length > 1) {
                                    options.put(pair[0], pair[1]);
                                } else {
                                    options.put(pair[0], null);
                                }
                            }
                        }
                    }
                }
            }
        }
        // use default value if memory is not set in project properties
        if (request.getMemorySize() <= 0) {
            request.setMemorySize(defMemSize);
        }
    }

    private Callable<RemoteRunnerProcess> createTaskFor(final BuildTaskDescriptor buildDescriptor, final RunRequest request) {
        return new Callable<RemoteRunnerProcess>() {
            @Override
            public RemoteRunnerProcess call() throws Exception {
                if (buildDescriptor != null) {
                    final Link buildStatusLink = getLink(buildDescriptor, com.codenvy.api.builder.internal.Constants.LINK_REL_GET_STATUS);
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
                        switch (buildDescriptor.getStatus()) {
                            case SUCCESSFUL:
                                final Link downloadLink =
                                        getLink(buildDescriptor, com.codenvy.api.builder.internal.Constants.LINK_REL_DOWNLOAD_RESULT);
                                if (downloadLink == null) {
                                    throw new RunnerException("Unable start application. Application build is successful but there " +
                                                              "is no URL for download result of build.");
                                }
                                final long lifetime = getApplicationLifetime(request);
                                return getRunner(request)
                                        .run(request.withDeploymentSourcesUrl(downloadLink.getHref()).withLifetime(lifetime));
                            case CANCELLED:
                            case FAILED:
                                String msg = "Unable start application. Build of application is failed or cancelled.";
                                final Link logLink =
                                        getLink(buildDescriptor, com.codenvy.api.builder.internal.Constants.LINK_REL_VIEW_LOG);
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
                    final long lifetime = getApplicationLifetime(request);
                    return getRunner(request).run(request.withLifetime(lifetime));
                }
            }
        };
    }

    private boolean tryCancelBuild(BuildTaskDescriptor buildDescriptor) {
        final Link cancelLink = getLink(buildDescriptor, com.codenvy.api.builder.internal.Constants.LINK_REL_CANCEL);
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

    private long getApplicationLifetime(RunRequest request) {
        // TODO: calculate in different way for different workspace/project.
        return appLifetime;
    }

    private void purgeExpiredTasks() {
        final long timestamp = System.currentTimeMillis();
        int num = 0;
        int waitingNum = 0;
        for (Iterator<RunQueueTask> i = tasks.values().iterator(); i.hasNext(); ) {
            final RunQueueTask task = i.next();
            boolean waiting;
            long sendTime;
            // 1. not started, may be because too long build process
            // 2. running longer then expect
            if ((waiting = task.isWaiting()) && ((task.getCreationTime() + maxTimeInQueueMillis) < timestamp)
                || ((sendTime = task.getSendToRemoteRunnerTime()) > 0
                    && (sendTime + TimeUnit.SECONDS.toMillis(task.getRequest().getLifetime())) < timestamp)) {
                try {
                    task.cancel();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    continue; // try next time
                }
                if (waiting) {
                    waitingNum++;
                }
                i.remove();
                num++;
            }
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks, {} of them were waiting for processing", num, waitingNum);
        }
    }

    public RunQueueTask getTask(Long id) throws NoSuchRunnerTaskException {
        checkStarted();
        final RunQueueTask task = tasks.get(id);
        if (task == null) {
            throw new NoSuchRunnerTaskException(String.format("Not found task %d. It may be cancelled by timeout.", id));
        }
        return task;
    }

    @PostConstruct
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    @PreDestroy
    public synchronized void stop() {
        checkStarted();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        tasks.clear();
        runnerListMapping.clear();
        started = false;
    }

    public boolean registerRunnerService(RunnerServiceRegistration registration) throws RemoteException, IOException, RunnerException {
        checkStarted();
        String workspace = null;
        String project = null;
        final RunnerServiceAccessCriteria accessCriteria = registration.getRunnerServiceAccessCriteria();
        if (accessCriteria != null) {
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }

        final RemoteRunnerFactory factory = new RemoteRunnerFactory(registration.getRunnerServiceLocation().getUrl());
        final List<RemoteRunner> toAdd = new ArrayList<>();
        for (RunnerDescriptor runnerDescriptor : factory.getAvailableRunners()) {
            toAdd.add(factory.createRemoteRunner(runnerDescriptor));
        }
        return registerRunners(workspace, project, toAdd);
    }

    boolean registerRunners(String workspace, String project, List<RemoteRunner> toAdd) {
        final RunnerListKey key = new RunnerListKey(project, workspace);
        RunnerList runnerList = runnerListMapping.get(key);
        if (runnerList == null) {
            final RunnerList newRunnerList = new RunnerList(runnerSelector);
            runnerList = runnerListMapping.putIfAbsent(key, newRunnerList);
            if (runnerList == null) {
                runnerList = newRunnerList;
            }
        }
        return runnerList.addRunners(toAdd);
    }

    public boolean unregisterRunnerService(RunnerServiceLocation location) throws RemoteException, IOException, RunnerException {
        checkStarted();
        final RemoteRunnerFactory factory = new RemoteRunnerFactory(location.getUrl());
        final List<RemoteRunner> toRemove = new ArrayList<>();
        for (RunnerDescriptor runnerDescriptor : factory.getAvailableRunners()) {
            toRemove.add(factory.createRemoteRunner(runnerDescriptor));
        }
        return unregisterRunners(toRemove);
    }

    boolean unregisterRunners(List<RemoteRunner> toRemove) {
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

    private Link getLink(BuildTaskDescriptor buildDescriptor, String rel) {
        for (Link link : buildDescriptor.getLinks()) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private RemoteRunner getRunner(RunRequest request) throws RunnerException {
        final String project = request.getProject();
        final String workspace = request.getWorkspace();
        RunnerList runnerList = runnerListMapping.get(new RunnerListKey(project, workspace));
        if (runnerList == null) {
            if (project != null || workspace != null) {
                if (project != null && workspace != null) {
                    // have dedicated runners for project in some workspace?
                    runnerList = runnerListMapping.get(new RunnerListKey(project, workspace));
                }
                if (runnerList == null && workspace != null) {
                    // have dedicated runners for whole workspace (omit project) ?
                    runnerList = runnerListMapping.get(new RunnerListKey(null, workspace));
                }
                if (runnerList == null) {
                    // seems there is no dedicated runners for specified request, use shared one then
                    runnerList = runnerListMapping.get(new RunnerListKey(null, null));
                }
            }
        }
        if (runnerList == null) {
            // Can't continue, typically should never happen. At least shared runners should be available for everyone.
            throw new RunnerException("There is no any runner to process this request. ");
        }
        final RemoteRunner runner = runnerList.getRunner(request);
        if (runner == null) {
            throw new RunnerException("There is no any runner to process this request. ");
        }
        return runner;
    }

    private static class RunnerListKey {
        final String project;
        final String workspace;

        RunnerListKey(String project, String workspace) {
            this.project = project;
            this.workspace = workspace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RunnerListKey)) {
                return false;
            }
            RunnerListKey other = (RunnerListKey)o;
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
            return "RunnerListKey{" +
                   "workspace='" + workspace + '\'' +
                   ", project='" + project + '\'' +
                   '}';
        }
    }


    private static class RunnerList {
        final Collection<RemoteRunner> runners;
        final RunnerSelectionStrategy  runnerSelector;

        RunnerList(RunnerSelectionStrategy runnerSelector) {
            this.runnerSelector = runnerSelector;
            runners = new LinkedHashSet<>();
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
            final List<RemoteRunner> matched = new ArrayList<>();
            for (RemoteRunner runner : runners) {
                if (request.getRunner().equals(runner.getName())) {
                    matched.add(runner);
                }
            }
            final int size = matched.size();
            if (size == 0) {
                return null;
            }
            final List<RemoteRunner> available = new ArrayList<>(matched.size());
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
                    if (runnerState.getFreeMemory() >= request.getMemorySize()) {
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
