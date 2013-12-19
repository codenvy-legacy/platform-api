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

import com.codenvy.api.builder.dto.BuilderServiceAccessCriteria;
import com.codenvy.api.builder.dto.BuilderServiceLocation;
import com.codenvy.api.builder.dto.BuilderServiceRegistration;
import com.codenvy.api.builder.internal.BuilderException;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.NoSuchBuildTaskException;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildOptions;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.workspace.server.WorkspaceService;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.inject.ConfigurationParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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

/**
 * Accepts all build request and redirects them to the slave-builders. If there is no any available slave-builder at the moment it stores
 * build request and tries send request again. Requests don't stay in this queue forever. Max time (in minutes) for request to be in the
 * queue set up by configuration parameter {@link #MAX_TIME_IN_QUEUE}.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Singleton
public class BuildQueue implements Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(BuildQueue.class);

    /**
     * Name of configuration parameter that sets max time (in seconds) which request may be in this queue. After this time the results of
     * build may be removed.
     */
    public static final String MAX_TIME_IN_QUEUE = "builder.queue.max_time_in_queue";
    /** Name of configuration parameter that provides build timeout is seconds. After this time build may be terminated. */
    public static final String BUILD_TIMEOUT     = "builder.queue.build_timeout";

    private static final long CHECK_AVAILABLE_BUILDER_DELAY = 2000;

    private final BuilderSelectionStrategy                   builderSelector;
    private final ExecutorService                            executor;
    private final ConcurrentMap<Long, BuildQueueTask>        tasks;
    private final ConcurrentMap<BuilderListKey, BuilderList> builderListMapping;
    private final int                                        timeout;
    /** Max time for request to be in queue in milliseconds. */
    private final long                                       maxTimeInQueueMillis;

    private boolean started;

    @Inject
    public BuildQueue(@Named(MAX_TIME_IN_QUEUE) ConfigurationParameter maxTimeInQueue,
                      @Named(BUILD_TIMEOUT) ConfigurationParameter timeout,
                      BuilderSelectionStrategy builderSelector) {
        this(maxTimeInQueue.asInt(), timeout.asInt(), builderSelector);
    }

    public BuildQueue(int maxTimeInQueue, int timeout, BuilderSelectionStrategy builderSelector) {
        this.timeout = timeout;
        this.maxTimeInQueueMillis = TimeUnit.SECONDS.toMillis(maxTimeInQueue);
        this.builderSelector = builderSelector;
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BuildQueue-", true));
        tasks = new ConcurrentHashMap<>();
        builderListMapping = new ConcurrentHashMap<>();
    }

    /**
     * Get total size of queue of tasks.
     *
     * @return total size of queue of tasks
     */
    public int getTotalNum() {
        checkStarted();
        return tasks.size();
    }

    /**
     * Get number of tasks which are waiting for processing.
     *
     * @return number of tasks which are waiting for processing
     */
    public int getWaitingNum() {
        checkStarted();
        int count = 0;
        for (BuildQueueTask task : tasks.values()) {
            if (task.isWaiting()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Register remote BuildService which can process builds.
     *
     * @param registration
     *         BuilderServiceRegistration
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote BuildService
     * @throws RemoteException
     *         if we access remote BuildService successfully but get error response
     * @throws BuilderException
     *         if other type of error occurs
     */
    public boolean registerBuilderService(BuilderServiceRegistration registration) throws IOException, RemoteException, BuilderException {
        checkStarted();
        final BuilderServiceAccessCriteria accessCriteria = registration.getBuilderServiceAccessCriteria();
        final BuilderListKey key = accessCriteria != null
                                   ? new BuilderListKey(accessCriteria.getProject(), accessCriteria.getWorkspace())
                                   : new BuilderListKey(null, null);
        BuilderList builderList = builderListMapping.get(key);
        if (builderList == null) {
            final BuilderList newBuilderList = new BuilderList(builderSelector);
            builderList = builderListMapping.putIfAbsent(key, newBuilderList);
            if (builderList == null) {
                builderList = newBuilderList;
            }
        }

        final RemoteBuilderFactory factory = new RemoteBuilderFactory(registration.getBuilderServiceLocation().getUrl());
        final List<RemoteBuilder> toAdd = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toAdd.add(factory.createRemoteBuilder(builderDescriptor));
        }
        return builderList.addBuilders(toAdd);
    }

    /**
     * Unregister remote BuildService which can process builds.
     *
     * @param location
     *         BuilderServiceLocation
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote BuildService
     * @throws RemoteException
     *         if we access remote BuildService successfully but get error response
     * @throws BuilderException
     *         if other type of error occurs
     */
    public boolean unregisterBuilderService(BuilderServiceLocation location) throws RemoteException, IOException, BuilderException {
        checkStarted();
        final RemoteBuilderFactory factory = new RemoteBuilderFactory(location.getUrl());
        final List<RemoteBuilder> toRemove = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toRemove.add(factory.createRemoteBuilder(builderDescriptor));
        }

        boolean modified = false;
        for (Iterator<BuilderList> iterator = builderListMapping.values().iterator(); iterator.hasNext(); ) {
            final BuilderList builderList = iterator.next();
            if (builderList.removeBuilders(toRemove)) {
                modified |= true;
                if (builderList.size() == 0) {
                    iterator.remove();
                }
            }
        }
        return modified;
    }

    /**
     * Schedule new build.
     *
     * @param workspace
     *         name of workspace to which project belongs
     * @param project
     *         name of project
     * @param serviceContext
     *         ServiceContext
     * @return BuildQueueTask
     * @throws RemoteException
     *         if error occurs when try to get info about project or access slave-builder
     * @throws IOException
     *         if an i/o error occurs
     */
    public BuildQueueTask scheduleBuild(String workspace, String project, ServiceContext serviceContext, BuildOptions buildOptions)
            throws RemoteException, IOException, BuilderException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, serviceContext);
        final BuildRequest request = (BuildRequest)DtoFactory.getInstance().createDto(BuildRequest.class)
                                                             .withWorkspace(workspace)
                                                             .withProject(project);
        if (buildOptions != null) {
            request.setOptions(buildOptions.getOptions());
            request.setTargets(buildOptions.getTargets());
        }
        addRequestParameters(descriptor, request);
        request.setTimeout(getBuildTimeout(request));
        final BuilderList builderList = getBuilderList(request);
        final Callable<RemoteBuildTask> callable = new Callable<RemoteBuildTask>() {
            @Override
            public RemoteBuildTask call() throws IOException, RemoteException, BuilderException {
                final RemoteBuilder builder = builderList.getBuilder(request);
                if (builder == null) {
                    throw new BuilderException("There is no any builder available. ");
                }
                return builder.perform(request);
            }
        };
        final FutureTask<RemoteBuildTask> future = new FutureTask<>(callable);
        final BuildQueueTask task = new BuildQueueTask(request, future);
        request.setWebHookUrl(serviceContext.getServiceUriBuilder().path(BuilderService.class, "webhook").build(workspace,
                                                                                                                task.getId()).toString());
        tasks.put(task.getId(), task);
        purgeExpiredTasks();
        executor.execute(future);
        return task;
    }

    /**
     * Schedule new dependencies analyze.
     *
     * @param workspace
     *         name of workspace to which project belongs
     * @param project
     *         name of project
     * @param type
     *         type of analyze dependencies. Depends to implementation of slave-builder.
     * @param serviceContext
     *         ServiceContext
     * @return BuildQueueTask
     * @throws RemoteException
     *         if error occurs when try to get info about project or access slave-builder
     * @throws IOException
     *         if an i/o error occurs
     */
    public BuildQueueTask scheduleDependenciesAnalyze(String workspace, String project, String type, ServiceContext serviceContext)
            throws RemoteException, IOException, BuilderException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, serviceContext);
        final DependencyRequest request = (DependencyRequest)DtoFactory.getInstance().createDto(DependencyRequest.class)
                                                                       .withType(type)
                                                                       .withWorkspace(workspace)
                                                                       .withProject(project);
        addRequestParameters(descriptor, request);
        request.setTimeout(getBuildTimeout(request));
        final BuilderList builderList = getBuilderList(request);
        final Callable<RemoteBuildTask> callable = new Callable<RemoteBuildTask>() {
            @Override
            public RemoteBuildTask call() throws IOException, RemoteException, BuilderException {
                final RemoteBuilder builder = builderList.getBuilder(request);
                if (builder == null) {
                    throw new BuilderException("There is no any builder available. ");
                }
                return builder.perform(request);
            }
        };
        final FutureTask<RemoteBuildTask> future = new FutureTask<>(callable);
        final BuildQueueTask task = new BuildQueueTask(request, future);
        request.setWebHookUrl(serviceContext.getServiceUriBuilder().path(BuilderService.class, "webhook").build(workspace,
                                                                                                                task.getId()).toString());
        tasks.put(task.getId(), task);
        purgeExpiredTasks();
        executor.execute(future);
        return task;
    }

    private void addRequestParameters(ProjectDescriptor descriptor, BaseBuilderRequest request) {
        final String builder;
        List<String> builderAttribute = descriptor.getAttributes().get(Constants.BUILDER_NAME);
        if (builderAttribute == null || builderAttribute.isEmpty() || (builder = builderAttribute.get(0)) == null) {
            throw new IllegalStateException(
                    String.format("Name of builder is not specified, be sure property of project %s is set", Constants.BUILDER_NAME));
        }
        request.setBuilder(builder);
        final String buildTargets = Constants.BUILDER_TARGETS.replace("${builder}", builder);
        final String buildOptions = Constants.BUILDER_OPTIONS.replace("${builder}", builder);

        for (Map.Entry<String, List<String>> entry : descriptor.getAttributes().entrySet()) {
            if ("zipball_sources_url".equals(entry.getKey())) {
                if (!entry.getValue().isEmpty()) {
                    request.setSourcesUrl(entry.getValue().get(0));
                }
            } else if (buildTargets.equals(entry.getKey()) && request.getTargets().isEmpty()) {
                if (!entry.getValue().isEmpty()) {
                    request.setTargets(entry.getValue());
                }
            } else if (buildOptions.equals(entry.getKey()) && request.getOptions().isEmpty()) {
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
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, ServiceContext serviceContext)
            throws IOException, RemoteException {
        final UriBuilder baseUriBuilder = serviceContext.getBaseUriBuilder();
        final String projectUrl = baseUriBuilder.path(WorkspaceService.class)
                                                .path(WorkspaceService.class, "getProject")
                                                .build(workspace).toString();
        return HttpJsonHelper.get(ProjectDescriptor.class, projectUrl, Pair.of("name", project));
    }

    private BuilderList getBuilderList(BaseBuilderRequest request) throws BuilderException {
        final String project = request.getProject();
        final String workspace = request.getWorkspace();
        BuilderList builderList = builderListMapping.get(new BuilderListKey(project, workspace));
        if (builderList == null) {
            if (project != null || workspace != null) {
                if (project != null && workspace != null) {
                    // have dedicated builders for project in some workspace?
                    builderList = builderListMapping.get(new BuilderListKey(project, workspace));
                }
                if (builderList == null && workspace != null) {
                    // have dedicated builders for whole workspace (omit project) ?
                    builderList = builderListMapping.get(new BuilderListKey(null, workspace));
                }
                if (builderList == null) {
                    // seems there is no dedicated builders for specified request, use shared one then
                    builderList = builderListMapping.get(new BuilderListKey(null, null));
                }
            }
        }
        if (builderList == null) {
            // Cannot continue, typically should never happen. At least shared builders should be available for everyone.
            throw new BuilderException("There is no any builder to process this request. ");
        }
        return builderList;
    }

    private long getBuildTimeout(BaseBuilderRequest request) {
        // TODO: calculate in different way for different workspace/project.
        return timeout;
    }

    private void purgeExpiredTasks() {
        final long timestamp = System.currentTimeMillis();
        int num = 0;
        int waitingNum = 0;
        for (Iterator<BuildQueueTask> i = tasks.values().iterator(); i.hasNext(); ) {
            final BuildQueueTask task = i.next();
            boolean waiting;
            long sendTime;
            if ((waiting = task.isWaiting()) && ((task.getCreationTime() + maxTimeInQueueMillis) < timestamp)
                || ((sendTime = task.getSendToRemoteBuilderTime()) > 0 && (sendTime + task.getRequest().getTimeout()) < timestamp)) {
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
                tasks.remove(task.getId());
                num++;
            }
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks, {} of them were waiting for processing", num, waitingNum);
        }
    }

    public BuildQueueTask getTask(Long id) throws NoSuchBuildTaskException {
        checkStarted();
        final BuildQueueTask task = tasks.get(id);
        if (task == null) {
            throw new NoSuchBuildTaskException(String.format("Not found task %d. It may be cancelled by timeout.", id));
        }
        return task;
    }

    @PostConstruct
    @Override
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        final InputStream regConf =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/builder_service_registrations.json");
        if (regConf != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000); // TODO: fix this, add this to give couple of time for starting servlet container
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        for (BuilderServiceRegistration registration : DtoFactory.getInstance().createListDtoFromJson(regConf,
                                                                                                                      BuilderServiceRegistration.class)) {
                            registerBuilderService(registration);
                            LOG.debug("Register slave builder: {}", registration);
                        }
                    } catch (IOException | RemoteException | BuilderException e) {
                        LOG.error(e.getMessage(), e);
                    } finally {
                        try {
                            regConf.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            });
        }
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalArgumentException("Lifecycle instance is not started yet.");
        }
    }

    @PreDestroy
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
        builderListMapping.clear();
        started = false;
    }

    private static class BuilderListKey {
        final String project;
        final String workspace;

        BuilderListKey(String project, String workspace) {
            this.project = project;
            this.workspace = workspace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BuilderListKey)) {
                return false;
            }
            BuilderListKey other = (BuilderListKey)o;
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
            return "BuilderListKey{" +
                   "workspace='" + workspace + '\'' +
                   ", project='" + project + '\'' +
                   '}';
        }
    }


    private static class BuilderList {
        final Collection<RemoteBuilder> builders;
        final BuilderSelectionStrategy  builderSelector;

        BuilderList(BuilderSelectionStrategy builderSelector) {
            this.builderSelector = builderSelector;
            builders = new LinkedHashSet<>();
        }

        synchronized boolean addBuilders(Collection<? extends RemoteBuilder> list) {
            if (builders.addAll(list)) {
                notifyAll();
                return true;
            }
            return false;
        }

        synchronized boolean removeBuilders(Collection<? extends RemoteBuilder> list) {
            if (builders.removeAll(list)) {
                notifyAll();
                return true;
            }
            return false;
        }

        synchronized int size() {
            return builders.size();
        }

        synchronized RemoteBuilder getBuilder(BaseBuilderRequest request) {
            final List<RemoteBuilder> matched = new ArrayList<>();
            for (RemoteBuilder builder : builders) {
                if (request.getBuilder().equals(builder.getName())) {
                    matched.add(builder);
                }
            }
            final int size = matched.size();
            if (size == 0) {
                return null;
            }
            final List<RemoteBuilder> available = new ArrayList<>(matched.size());
            int attemptGetState = 0;
            for (; ; ) {
                for (RemoteBuilder builder : matched) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null; // stop immediately
                    }
                    SlaveBuilderState builderState;
                    try {
                        builderState = builder.getRemoteBuilderState();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        ++attemptGetState;
                        if (attemptGetState > 10) {
                            return null;
                        }
                        continue;
                    }
                    if (builderState.getNumberOfActiveWorkers() < builderState.getNumberOfWorkers()) {
                        available.add(builder);
                    }
                }

                if (available.isEmpty()) {
                    try {
                        wait(CHECK_AVAILABLE_BUILDER_DELAY); // wait and try again
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null; // expected to get here if task is canceled
                    }
                } else {
                    if (available.size() > 0) {
                        return builderSelector.select(available);
                    }
                    return available.get(0);
                }
            }
        }
    }
}
