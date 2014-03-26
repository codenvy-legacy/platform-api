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

import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.dto.BuilderServiceAccessCriteria;
import com.codenvy.api.builder.dto.BuilderServiceLocation;
import com.codenvy.api.builder.dto.BuilderServiceRegistration;
import com.codenvy.api.builder.internal.BuildDoneEvent;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accepts all build request and redirects them to the slave-builders. If there is no any available slave-builder at the moment it stores
 * build request and tries send request again. Requests don't stay in this queue forever. Max time (in minutes) for request to be in the
 * queue set up by configuration parameter {@code builder.queue.max_time_in_queue}.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Singleton
public class BuildQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BuildQueue.class);

    private static final long CHECK_AVAILABLE_BUILDER_DELAY = 2000;

    private static final AtomicLong sequence = new AtomicLong(1);

    private final BuilderSelectionStrategy                                 builderSelector;
    private final ConcurrentMap<Long, BuildQueueTask>                      tasks;
    private final ConcurrentMap<ProjectWithWorkspace, BuilderList>         builderListMapping;
    private final String                                                   baseProjectApiUrl;
    private final int                                                      timeout;
    private final EventService                                             eventService;
    /** Max time for request to be in queue in milliseconds. */
    private final long                                                     maxTimeInQueueMillis;
    private final ConcurrentMap<ProjectWithWorkspace, BuildTaskDescriptor> successfulBuild;

    private ExecutorService executor;
    private boolean         started;

    /**
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as builder API has, e.g. suppose we have builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param maxTimeInQueue
     *         max time for request to be in queue in seconds. Configuration parameter that sets max time (in seconds) which request may be
     *         in this queue. After this time the results of build may be removed.
     * @param timeout
     *         build timeout. Configuration parameter that provides build timeout is seconds. After this time build may be terminated.
     */
    @Inject
    public BuildQueue(@Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                      @Named("builder.queue.max_time_in_queue") int maxTimeInQueue,
                      @Named("builder.queue.build_timeout") int timeout,
                      BuilderSelectionStrategy builderSelector,
                      EventService eventService) {
        this.baseProjectApiUrl = baseProjectApiUrl;
        this.timeout = timeout;
        this.eventService = eventService;
        this.maxTimeInQueueMillis = TimeUnit.SECONDS.toMillis(maxTimeInQueue);
        this.builderSelector = builderSelector;
        tasks = new ConcurrentHashMap<>();
        builderListMapping = new ConcurrentHashMap<>();
        successfulBuild = new ConcurrentHashMap<>();
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
     * Register remote SlaveBuildService which can process builds.
     *
     * @param registration
     *         BuilderServiceRegistration
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean registerBuilderService(BuilderServiceRegistration registration) throws BuilderException {
        checkStarted();
        String workspace = null;
        String project = null;
        final BuilderServiceAccessCriteria accessCriteria = registration.getBuilderServiceAccessCriteria();
        if (accessCriteria != null) {
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }
        final RemoteBuilderFactory factory = new RemoteBuilderFactory(registration.getBuilderServiceLocation().getUrl());
        final List<RemoteBuilder> toAdd = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toAdd.add(factory.createRemoteBuilder(builderDescriptor));
        }
        return registerBuilders(workspace, project, toAdd);
    }

    protected boolean registerBuilders(String workspace, String project, List<RemoteBuilder> toAdd) {
        final ProjectWithWorkspace key = new ProjectWithWorkspace(project, workspace);
        BuilderList builderList = builderListMapping.get(key);
        if (builderList == null) {
            final BuilderList newBuilderList = new BuilderList(builderSelector);
            builderList = builderListMapping.putIfAbsent(key, newBuilderList);
            if (builderList == null) {
                builderList = newBuilderList;
            }
        }
        return builderList.addBuilders(toAdd);
    }

    /**
     * Unregister remote SlaveBuildService.
     *
     * @param location
     *         BuilderServiceLocation
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean unregisterBuilderService(BuilderServiceLocation location) throws BuilderException {
        checkStarted();
        final RemoteBuilderFactory factory = new RemoteBuilderFactory(location.getUrl());
        final List<RemoteBuilder> toRemove = new ArrayList<>();
        for (BuilderDescriptor builderDescriptor : factory.getAvailableBuilders()) {
            toRemove.add(factory.createRemoteBuilder(builderDescriptor));
        }
        return unregisterBuilders(toRemove);
    }

    protected boolean unregisterBuilders(List<RemoteBuilder> toRemove) {
        boolean modified = false;
        for (Iterator<BuilderList> i = builderListMapping.values().iterator(); i.hasNext(); ) {
            final BuilderList builderList = i.next();
            if (builderList.removeBuilders(toRemove)) {
                modified |= true;
                if (builderList.size() == 0) {
                    i.remove();
                }
            }
        }
        return modified;
    }

    /**
     * Schedule new build.
     *
     * @param workspace
     *         id of workspace to which project belongs
     * @param project
     *         name of project
     * @param serviceContext
     *         ServiceContext
     * @return BuildQueueTask
     */
    public BuildQueueTask scheduleBuild(String workspace, String project, ServiceContext serviceContext, BuildOptions buildOptions)
            throws BuilderException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, serviceContext);
        final BuildRequest request = (BuildRequest)DtoFactory.getInstance().createDto(BuildRequest.class)
                                                             .withWorkspace(workspace)
                                                             .withProject(project);
        if (buildOptions != null) {
            request.setBuilder(buildOptions.getBuilderName());
            request.setOptions(buildOptions.getOptions());
            request.setTargets(buildOptions.getTargets());
            request.setIncludeDependencies(buildOptions.isIncludeDependencies());
            request.setSkipTest(buildOptions.isSkipTest());
        }
        addRequestParameters(descriptor, request);
        request.setTimeout(getBuildTimeout(request));
        final Callable<RemoteTask> callable = createTaskFor(request);
        final FutureTask<RemoteTask> future = new FutureTask<>(callable);
        final Long id = sequence.getAndIncrement();
        request.setId(id);
        final BuildQueueTask task = new BuildQueueTask(id, request, future, serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        purgeExpiredTasks();
        executor.execute(future);
        return task;
    }

    protected Callable<RemoteTask> createTaskFor(final BuildRequest request) {
        return new Callable<RemoteTask>() {
            @Override
            public RemoteTask call() throws BuilderException {
                final BuilderList builderList = getBuilderList(request);
                final RemoteBuilder builder = builderList.getBuilder(request);
                if (builder == null) {
                    throw new BuilderException("There is no any builder available. ");
                }
                LOG.debug("Use slave builder {} at {}", builder.getName(), builder.getBaseUrl());
                return builder.perform(request);
            }
        };
    }

    /**
     * Schedule new dependencies analyze.
     *
     * @param workspace
     *         id of workspace to which project belongs
     * @param project
     *         name of project
     * @param type
     *         type of analyze dependencies. Depends to implementation of slave-builder.
     * @param serviceContext
     *         ServiceContext
     * @return BuildQueueTask
     */
    public BuildQueueTask scheduleDependenciesAnalyze(String workspace, String project, String type, ServiceContext serviceContext)
            throws BuilderException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(workspace, project, serviceContext);
        final DependencyRequest request = (DependencyRequest)DtoFactory.getInstance().createDto(DependencyRequest.class)
                                                                       .withType(type)
                                                                       .withWorkspace(workspace)
                                                                       .withProject(project);
        addRequestParameters(descriptor, request);
        request.setTimeout(getBuildTimeout(request));
        final Callable<RemoteTask> callable = createTaskFor(request);
        final FutureTask<RemoteTask> future = new FutureTask<>(callable);
        final Long id = sequence.getAndIncrement();
        request.setId(id);
        final BuildQueueTask task = new BuildQueueTask(id, request, future, serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        purgeExpiredTasks();
        executor.execute(future);
        return task;
    }

    protected Callable<RemoteTask> createTaskFor(final DependencyRequest request) {
        return new Callable<RemoteTask>() {
            @Override
            public RemoteTask call() throws BuilderException {
                final BuilderList builderList = getBuilderList(request);
                final RemoteBuilder builder = builderList.getBuilder(request);
                if (builder == null) {
                    throw new BuilderException("There is no any builder available. ");
                }
                LOG.debug("Use slave builder {} at {}", builder.getName(), builder.getBaseUrl());
                return builder.perform(request);
            }
        };
    }

    private void addRequestParameters(ProjectDescriptor descriptor, BaseBuilderRequest request) {
        String builder = request.getBuilder();
        final Map<String, List<String>> projectAttributes = descriptor.getAttributes();
        if (builder == null) {
            builder = getAttributeValue(Constants.BUILDER_NAME, projectAttributes);
            if (builder == null) {
                throw new IllegalStateException(
                        String.format("Name of builder is not specified, be sure property of project %s is set", Constants.BUILDER_NAME));
            }
            request.setBuilder(builder);
        }
        request.setProjectUrl(descriptor.getBaseUrl());
        final Link zipballLink = getLink(com.codenvy.api.project.server.Constants.LINK_REL_EXPORT_ZIP, descriptor.getLinks());
        if (zipballLink != null) {
            request.setSourcesUrl(zipballLink.getHref());
        }
        if (request.getTargets().isEmpty()) {
            final List<String> targetsAttr = projectAttributes.get(Constants.BUILDER_TARGETS.replace("${builder}", builder));
            if (targetsAttr != null && !targetsAttr.isEmpty()) {
                request.getTargets().addAll(targetsAttr);
            }
        }
        final List<String> optionsAttr = projectAttributes.get(Constants.BUILDER_OPTIONS.replace("${builder}", builder));
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
    }

    private static String getAttributeValue(String name, Map<String, List<String>> attributes) {
        final List<String> list = attributes.get(name);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private static Link getLink(String rel, List<Link> links) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, ServiceContext serviceContext)
            throws BuilderException {
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
            throw new BuilderException(e);
        } catch (RemoteException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    private BuilderList getBuilderList(BaseBuilderRequest request) throws BuilderException {
        final String project = request.getProject();
        final String workspace = request.getWorkspace();
        BuilderList builderList = builderListMapping.get(new ProjectWithWorkspace(project, workspace));
        if (builderList == null) {
            if (project != null || workspace != null) {
                if (project != null && workspace != null) {
                    // have dedicated builders for project in some workspace?
                    builderList = builderListMapping.get(new ProjectWithWorkspace(project, workspace));
                }
                if (builderList == null && workspace != null) {
                    // have dedicated builders for whole workspace (omit project) ?
                    builderList = builderListMapping.get(new ProjectWithWorkspace(null, workspace));
                }
                if (builderList == null) {
                    // seems there is no dedicated builders for specified request, use shared one then
                    builderList = builderListMapping.get(new ProjectWithWorkspace(null, null));
                }
            }
        }
        if (builderList == null) {
            // Cannot continue, typically should never happen. At least shared builders should be available for everyone.
            throw new BuilderException("There is no any builder to process this request. ");
        }
        return builderList;
    }

    private long getBuildTimeout(BaseBuilderRequest request) throws BuilderException {
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
                || ((sendTime = task.getSendToRemoteBuilderTime()) > 0
                    && (sendTime + TimeUnit.SECONDS.toMillis(task.getRequest().getTimeout())) < timestamp)) {
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
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BuildQueue-", true));
        eventService.subscribe(new EventSubscriber<BuildDoneEvent>() {
            @Override
            public void onEvent(BuildDoneEvent event) {
                final long id = event.getTaskId();
                try {
                    final BuildQueueTask task = getTask(id);
                    final BaseBuilderRequest request = task.getRequest();
                    if (request instanceof BuildRequest) {
                        final BuildTaskDescriptor buildDescriptor = task.getDescriptor();
                        if (buildDescriptor.getStatus() == BuildStatus.SUCCESSFUL) {
                            final String project = request.getProject();
                            final String workspace = request.getWorkspace();
                            successfulBuild.put(new ProjectWithWorkspace(project, workspace), buildDescriptor);
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
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
        builderListMapping.clear();
        successfulBuild.clear();
        started = false;
    }

    protected EventService getEventService() {
        return eventService;
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
                        builderState = builder.getBuilderState();
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
