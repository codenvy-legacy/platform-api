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
import com.codenvy.api.builder.internal.NoSuchBuildTaskException;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.LifecycleException;
import com.codenvy.api.core.config.Configurable;
import com.codenvy.api.core.config.Configuration;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteAccessException;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.util.ComponentLoader;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.api.workspace.server.WorkspaceService;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see Configurable
 * @see Configuration
 */
public class BuildQueue implements Configurable, Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(BuildQueue.class);

    /**
     * Name of configuration parameter that sets max time (in minutes) which request may be in this queue. After this time the results of
     * build may be removed. Default value is 10 minutes.
     */
    public static final String MAX_TIME_IN_QUEUE = "builder.queue.max_time_in_queue";

    private final BuilderListSorter                          builderListSorter;
    private final ExecutorService                            executor;
    private final ConcurrentMap<Long, BuildQueueTask>        tasks;
    private final Queue<BuildQueueTask>                      tasksFIFO;
    private final ConcurrentMap<BuilderListKey, BuilderList> builderListMapping;

    private volatile boolean       maySetConfiguration;
    private          Configuration configuration;
    private          long          maxTimeInQueueMillis;

    public BuildQueue() {
        builderListSorter = ComponentLoader.one(BuilderListSorter.class);
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BuildQueue-", true));
        tasks = new ConcurrentHashMap<>();
        tasksFIFO = new ConcurrentLinkedQueue<>();
        builderListMapping = new ConcurrentHashMap<>();
        maySetConfiguration = true;
    }

    /**
     * Get total size of queue of tasks.
     *
     * @return total size of queue of tasks
     */
    public int getTotalNum() {
        return tasks.size();
    }

    /**
     * Get number of request which are waiting for processing.
     *
     * @return number of request which are waiting for processing
     */
    public int getWaitingNum() {
        int count = 0;
        for (BuildQueueTask request : tasks.values()) {
            if (request.isWaiting()) {
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
     * @throws com.codenvy.api.core.rest.RemoteAccessException
     *         if an error occurs when try to access remote BuildService
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote BuildService
     * @throws RemoteException
     *         if we access remote BuildService successfully but get error response which can understand and transform it to
     *         RemoteApiException
     */
    public boolean registerBuilderService(BuilderServiceRegistration registration) throws IOException, RemoteException {
        final BuilderServiceAccessCriteria accessCriteria = registration.getBuilderServiceAccessCriteria();
        final BuilderListKey key = accessCriteria != null
                                   ? new BuilderListKey(accessCriteria.getProject(), accessCriteria.getWorkspace())
                                   : new BuilderListKey(null, null);
        BuilderList builderList = builderListMapping.get(key);
        if (builderList == null) {
            final BuilderList newBuilderList = new BuilderList(builderListSorter);
            builderList = builderListMapping.putIfAbsent(key, newBuilderList);
            if (builderList == null) {
                builderList = newBuilderList;
            }
        }

        final RemoteBuilderFactory factory = new RemoteBuilderFactory(registration.getBuilderServiceLocation().getUrl());
        final List<RemoteBuilder> toAdd = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toAdd.add(factory.getRemoteBuilder(builderDescriptor.getName()));
        }
        return builderList.addBuilders(toAdd);
    }

    /**
     * Unregister remote BuildService which can process builds.
     *
     * @param location
     *         BuilderServiceLocation
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws com.codenvy.api.core.rest.RemoteAccessException
     *         if an error occurs when try to access remote BuildService
     * @throws java.io.IOException
     *         if any i/o error occurs when try to access remote BuildService
     * @throws RemoteException
     *         if we access remote BuildService successfully but get error response which can understand and transform it to
     *         RemoteApiException
     */
    public boolean unregisterBuilderService(BuilderServiceLocation location) throws RemoteException, IOException {
        final RemoteBuilderFactory factory = new RemoteBuilderFactory(location.getUrl());
        final List<RemoteBuilder> toRemove = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toRemove.add(factory.getRemoteBuilder(builderDescriptor.getName()));
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
    public BuildQueueTask scheduleBuild(String workspace, String project, ServiceContext serviceContext)
            throws RemoteException, IOException {
        final Project myProject = getProject(workspace, project, serviceContext);
        final BuildRequest request = DtoFactory.getInstance().createDto(BuildRequest.class);
        request.setWorkspace(workspace);
        request.setProject(project);
        addRequestParameters(myProject, request);
        final BuilderList builderList = getBuilderList(request);
        final Callable<RemoteBuildTask> callable = new Callable<RemoteBuildTask>() {
            @Override
            public RemoteBuildTask call() throws IOException, RemoteException {
                return builderList.getBuilder(request).perform(request);
            }
        };
        final FutureTask<RemoteBuildTask> future = new FutureTask<>(callable);
        final BuildQueueTask waiting = new BuildQueueTask(request, future);
        tasks.put(waiting.getId(), waiting);
        tasksFIFO.offer(waiting);
        purgeExpiredTasks();
        executor.execute(future);
        return waiting;
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
            throws RemoteException, IOException {
        final Project myProject = getProject(workspace, project, serviceContext);
        final DependencyRequest request = DtoFactory.getInstance().createDto(DependencyRequest.class);
        request.setWorkspace(workspace);
        request.setProject(project);
        request.setType(type);
        addRequestParameters(myProject, request);
        final BuilderList builderList = getBuilderList(request);
        final Callable<RemoteBuildTask> callable = new Callable<RemoteBuildTask>() {
            @Override
            public RemoteBuildTask call() throws IOException, RemoteException {
                return builderList.getBuilder(request).perform(request);
            }
        };
        final FutureTask<RemoteBuildTask> future = new FutureTask<>(callable);
        final BuildQueueTask waiting = new BuildQueueTask(request, future);
        tasks.put(waiting.getId(), waiting);
        tasksFIFO.offer(waiting);
        purgeExpiredTasks();
        executor.execute(future);
        return waiting;
    }

    private void addRequestParameters(Project project, BaseBuilderRequest request) {
        request.setSourcesUrl(project.getLinks().get(com.codenvy.api.vfs.shared.dto.Link.REL_EXPORT).getHref());
        for (Property property : project.getProperties()) {
            if ("builder.name".equals(property.getName())) {
                if (!property.getValue().isEmpty()) {
                    request.setBuilder(property.getValue().get(0));
                }
            } else if ("builder.targets".equals(property.getName())) {
                if (!property.getValue().isEmpty()) {
                    request.setTargets(property.getValue());
                }
            } else if ("builder.options".equals(property.getName())) {
                if (!property.getValue().isEmpty()) {
                    final Map<String, String> options = new LinkedHashMap<>();
                    for (String str : property.getValue()) {
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

    private Project getProject(String workspace, String project, ServiceContext serviceContext) throws IOException, RemoteException {
        final UriBuilder serviceUriBuilder = serviceContext.getBaseUriBuilder();
        final String projectUrl = serviceUriBuilder.path(WorkspaceService.class)
                                                   .path(WorkspaceService.class, "getProject")
                                                   .build(workspace, project).toString();
        return HttpJsonHelper.get(Project.class, projectUrl);
    }

    private BuilderList getBuilderList(BaseBuilderRequest request) {
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
                    // seems there is no dedicated builders for specified request, use shared then
                    builderList = builderListMapping.get(new BuilderListKey(null, null));
                }
            }
        }
        if (builderList == null) {
            // Cannot continue, typically should never happen. At least shared builders should be available for everyone.
            throw new IllegalStateException("There is no any builder to process this request. ");
        }
        return builderList;
    }

    private void purgeExpiredTasks() {
        final long timestamp = System.currentTimeMillis();
        int num = 0;
        int waitingNum = 0;
        BuildQueueTask current;
        while ((current = tasksFIFO.peek()) != null && (current.getCreationDate() + maxTimeInQueueMillis) < timestamp) {
            if (current.isWaiting()) {
                try {
                    current.cancel();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                waitingNum++;
            }
            tasksFIFO.poll();
            tasks.remove(current.getId());
            num++;
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks, {} of them were waiting for processing", num, waitingNum);
        }
    }

    public BuildQueueTask get(Long id) throws NoSuchBuildTaskException {
        final BuildQueueTask request = tasks.get(id);
        if (request == null) {
            throw new NoSuchBuildTaskException(String.format("Not found task %d. It may be cancelled by timeout.", id));
        }
        return request;
    }

    @Override
    public final Configuration getDefaultConfiguration() {
        final Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setInt(MAX_TIME_IN_QUEUE, 10);
        return defaultConfiguration;
    }

    @Override
    public final void setConfiguration(Configuration configuration) {
        if (maySetConfiguration) {
            this.configuration = configuration;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public final Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void start() {
        maySetConfiguration = false;
        final Configuration configuration = getConfiguration();
        final int maxTimeInQueueMinutes = configuration.getInt(MAX_TIME_IN_QUEUE, 10);
        if (maxTimeInQueueMinutes < 1) {
            throw new LifecycleException(String.format("Invalid %s parameter", MAX_TIME_IN_QUEUE));
        }
        maxTimeInQueueMillis = TimeUnit.MINUTES.toMillis(maxTimeInQueueMinutes);
    }

    @Override
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
        final BuilderListSorter         builderListSorter;

        BuilderList(BuilderListSorter builderListSorter) {
            this.builderListSorter = builderListSorter;
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
            final List<RemoteBuilder> candidates = new ArrayList<>();
            for (; ; ) {
                final int size = builders.size();
                if (size == 0) {
                    throw new RemoteAccessException("There is no any builder in the list. ");
                }

                for (RemoteBuilder builder : builders) {
                    if (request.getBuilder().equals(builder.getName())) {
                        SlaveBuilderState builderState;
                        try {
                            builderState = builder.getRemoteBuilderState();
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                            continue;
                        }
                        if (builderState.getNumberOfActiveWorkers() < builderState.getNumberOfWorkers()) {
                            candidates.add(builder);
                        }
                    }
                }
                if (candidates.isEmpty()) {
                    try {
                        wait(2000); // wait and try again
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    if (candidates.size() > 0) {
                        builderListSorter.sort(candidates);
                    }
                    return candidates.get(0);
                }
            }
        }
    }
}
