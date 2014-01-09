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
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.config.Configuration;
import com.codenvy.api.core.config.SingletonConfiguration;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.RemoteServiceDescriptor;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.rest.shared.dto.ServiceDescriptor;
import com.codenvy.api.core.util.ComponentLoader;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.runner.dto.RunnerServiceAccessCriteria;
import com.codenvy.api.runner.dto.RunnerServiceLocation;
import com.codenvy.api.runner.dto.RunnerServiceRegistration;
import com.codenvy.api.runner.internal.Constants;
import com.codenvy.api.runner.internal.dto.DebugMode;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.api.runner.internal.dto.RunnerDescriptor;
import com.codenvy.api.runner.internal.dto.RunnerState;
import com.codenvy.api.workspace.server.WorkspaceService;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class RunQueue implements Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(RunQueue.class);

    /**
     * Name of configuration parameter that points to the base API location. If such parameter isn't specified than use the same base
     * URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy.com/api/my_workspace/runner</i>, in this case base
     * URL is <i>http://codenvy.com/api/my_workspace</i> so we will try to find builder API at URL:
     * <i>http://codenvy.com/api/my_workspace/builder</i> and workspace API at URL: <i>http://codenvy.com/api/my_workspace/workspace</i>
     */
    public static final String BASE_API_URL         = "runner.queue.base_api_url";
    /**
     * Name of configuration parameter that set default memory size for application in megabytes. 128 is default value if this property is
     * not set.
     */
    public static final String DEFAULT_MEMORY_SIZE  = "runner.default_app_mem_size";
    /**
     * Name of configuration parameter that sets max time (in seconds) which request may be in this queue. After this time the request may
     * be removed from the queue. Default value is 600 seconds (10 minutes).
     */
    public static final String MAX_TIME_IN_QUEUE    = "runner.queue.max_time_in_queue";
    /**
     * Name of configuration parameter that sets lifetime (in seconds) of application. After this time the application may be terminated.
     * Default value is 900 seconds (15 minutes).
     */
    public static final String APPLICATION_LIFETIME = "runner.queue.app_lifetime";

    /** Pause in milliseconds for checking the result of build process. */
    private static final long CHECK_BUILD_RESULT_DELAY     = 2000;
    private static final long CHECK_AVAILABLE_RUNNER_DELAY = 2000;

    private final RunnerSelectionStrategy                  runnerSelector;
    private final ExecutorService                          executor;
    private final ConcurrentMap<RunnerListKey, RunnerList> runnerListMapping;
    private final ConcurrentMap<Long, RunQueueTask>        tasks;

    private String  baseApiUrl;
    /**
     * Default size of memory for application. This value used is there is nothing specified in properties of project.
     *
     * @see #DEFAULT_MEMORY_SIZE
     */
    private int     defMemSize;
    /**
     * Max time for request to be in queue in milliseconds.
     *
     * @see #MAX_TIME_IN_QUEUE
     */
    private long    maxTimeInQueueMillis;
    private long    appLifetime;
    private boolean started;

    public RunQueue() {
        runnerSelector = ComponentLoader.one(RunnerSelectionStrategy.class);
        tasks = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("RunQueue-", true));
        runnerListMapping = new ConcurrentHashMap<>();
    }

    public RunQueueTask run(String workspace, String project, ServiceContext serviceContext)
            throws IOException, RemoteException, RunnerException {
        checkStarted();
        // TODO: check do we need build the project
        final UriBuilder baseUriBuilder = baseApiUrl == null ? serviceContext.getBaseUriBuilder() : UriBuilder.fromUri(baseApiUrl);
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, baseUriBuilder.clone());
        final RunRequest request = DtoFactory.getInstance().createDto(RunRequest.class).withWorkspace(workspace).withProject(project);
        addRequestParameters(descriptor, request);
        final String builderUrl = baseUriBuilder.clone().path(BuilderService.class).build(workspace).toString();
        final RemoteServiceDescriptor builderService = new RemoteServiceDescriptor(builderUrl);
        final Link buildLink = builderService.getLink(com.codenvy.api.builder.internal.Constants.LINK_REL_BUILD);
        if (buildLink == null) {
            throw new RunnerException("Unable get URL for starting build of the application");
        }
        // schedule build
        final BuildTaskDescriptor buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class,
                                                                           buildLink,
                                                                           Pair.of("project", project));
        final FutureTask<RemoteRunnerProcess> future = new FutureTask<>(createTaskFor(buildDescriptor, request));
        final RunQueueTask task = new RunQueueTask(request, future);
        request.setWebHookUrl(serviceContext.getServiceUriBuilder().path(RunnerService.class, "webhook").build(workspace,
                                                                                                               task.getId()).toString());
        purgeExpiredTasks();
        tasks.put(task.getId(), task);
        executor.execute(future);
        return task;
    }

    private void addRequestParameters(ProjectDescriptor descriptor, RunRequest request) {
        final String runner;
        List<String> runnerAttribute = descriptor.getAttributes().get(Constants.RUNNER_NAME);
        if (runnerAttribute == null || runnerAttribute.isEmpty() || (runner = runnerAttribute.get(0)) == null) {
            throw new IllegalStateException(
                    String.format("Name of runner is not specified, be sure property of project %s is set", Constants.RUNNER_NAME));
        }
        request.setRunner(runner);
        final String runDebugMode = Constants.RUNNER_DEBUG_MODE.replace("${runner}", runner);
        final String runMemSize = Constants.RUNNER_MEMORY_SIZE.replace("${runner}", runner);
        final String runOptions = Constants.RUNNER_OPTIONS.replace("${runner}", runner);

        for (Map.Entry<String, List<String>> entry : descriptor.getAttributes().entrySet()) {
            if (runDebugMode.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    request.setDebugMode(DtoFactory.getInstance().createDto(DebugMode.class).withMode(entry.getValue().get(0)));
                }
            }
            if (runMemSize.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    request.setMemorySize(Integer.parseInt(entry.getValue().get(0)));
                }
            } else if (runOptions.equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    final Map<String, String> options = new LinkedHashMap<>();
                    for (String str : entry.getValue()) {
                        if (str != null) {
                            final String[] pair = str.split("=");
                            if (pair.length > 1) {
                                options.put(pair[0], pair[1]);
                            } else {
                                options.put(pair[0], null);
                            }
                        }
                    }
                    request.setOptions(options);
                }
            }
        }
        // use default value if memory is not set in project properties
        if (request.getMemorySize() <= 0) {
            request.setMemorySize(defMemSize);
        }
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, UriBuilder baseUriBuilder)
            throws IOException, RemoteException {
        final String projectUrl = baseUriBuilder.path(WorkspaceService.class)
                                                .path(WorkspaceService.class, "getProject")
                                                .build(workspace).toString();
        return HttpJsonHelper.get(ProjectDescriptor.class, projectUrl, Pair.of("name", project));
    }

    private Callable<RemoteRunnerProcess> createTaskFor(final BuildTaskDescriptor buildDescriptor, final RunRequest request) {
        return new Callable<RemoteRunnerProcess>() {
            @Override
            public RemoteRunnerProcess call() throws Exception {
                if (buildDescriptor != null) {
                    final Link buildStatusLink = getLink(buildDescriptor, com.codenvy.api.builder.internal.Constants.LINK_REL_GET_STATUS);
                    if (buildStatusLink == null) {
                        // TODO: more info!!!
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
                // create copy of link when pass it outside!!
                final BuildTaskDescriptor result =
                        HttpJsonHelper.request(BuildTaskDescriptor.class, DtoFactory.getInstance().clone(cancelLink));
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
                || ((sendTime = task.getSendToRemoteRunnerTime()) > 0 && (sendTime + task.getRequest().getLifetime()) < timestamp)) {
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

    protected Configuration getConfiguration() {
        return SingletonConfiguration.get();
    }

    @Override
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        final Configuration myConfiguration = getConfiguration();
        LOG.debug("{}", myConfiguration);
        baseApiUrl = myConfiguration.get(BASE_API_URL);
        defMemSize = myConfiguration.getInt(DEFAULT_MEMORY_SIZE, 128);
        maxTimeInQueueMillis = TimeUnit.SECONDS.toMillis(myConfiguration.getInt(MAX_TIME_IN_QUEUE, 600));
        appLifetime = myConfiguration.getInt(APPLICATION_LIFETIME, 900);
        final InputStream regConf = Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/runners.json");
        if (regConf != null) {
            try {
                final RunnerRegistration[] registrations = JsonHelper.fromJson(regConf, RunnerRegistration[].class, null);
                final List<RemoteRunner> runners = new ArrayList<>(registrations.length);
                for (RunnerRegistration registration : registrations) {
                    final ServiceDescriptor serviceDescriptor = registration.getRunnerServiceDescriptor();
                    for (RunnerDescriptor runnerDescriptor : registration.getRunnerDescriptors()) {
                        runners.add(new RemoteRunner(serviceDescriptor.getHref(), runnerDescriptor, serviceDescriptor.getLinks()));
                    }
                }
                registerRunners(null, null, runners);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                try {
                    regConf.close();
                } catch (IOException ignored) {
                }
            }

        }
        started = true;
    }

    // For local registration of Runners on startup. Need it to avoid sending any HTTP requests during starting servlet container.
    // This class MUST provide all required information about each Runner.
    public final static class RunnerRegistration {
        // info about remote runner service (rest service that is frontend for runners)
        private ServiceDescriptor      runnerServiceDescriptor;
        // set of runners that must be available
        private List<RunnerDescriptor> runnerDescriptors;

        public ServiceDescriptor getRunnerServiceDescriptor() {
            return runnerServiceDescriptor;
        }

        public void setRunnerServiceDescriptor(ServiceDescriptor runnerServiceDescriptor) {
            this.runnerServiceDescriptor = runnerServiceDescriptor;
        }

        public List<RunnerDescriptor> getRunnerDescriptors() {
            if (runnerDescriptors == null) {
                return Collections.emptyList();
            }
            return runnerDescriptors;
        }

        public void setRunnerDescriptors(List<RunnerDescriptor> runnerDescriptors) {
            this.runnerDescriptors = runnerDescriptors;
        }
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalArgumentException("Lifecycle instance is not started yet.");
        }
    }

    @Override
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

    private boolean registerRunners(String workspace, String project, List<RemoteRunner> toAdd) {
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

    private boolean unregisterRunners(List<RemoteRunner> toRemove) {
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
