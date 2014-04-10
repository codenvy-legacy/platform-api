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
import com.codenvy.api.builder.dto.BuilderServerAccessCriteria;
import com.codenvy.api.builder.dto.BuilderServerLocation;
import com.codenvy.api.builder.dto.BuilderServerRegistration;
import com.codenvy.api.builder.internal.BuildDoneEvent;
import com.codenvy.api.builder.internal.Constants;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderState;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.notification.EventSubscriber;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.RemoteException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.project.server.ProjectService;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.lang.concurrent.ThreadLocalPropagateContext;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

    private final ConcurrentMap<String, RemoteBuilderServer>       builderServices;
    private final BuilderSelectionStrategy                         builderSelector;
    private final ConcurrentMap<Long, BuildQueueTask>              tasks;
    private final ConcurrentMap<ProjectWithWorkspace, BuilderList> builderListMapping;
    private final String                                           baseProjectApiUrl;
    private final int                                              timeout;
    private final EventService                                     eventService;
    /** Max time for request to be in queue in milliseconds. */
    private final long                                             maxTimeInQueueMillis;
    private final ConcurrentMap<BaseBuilderRequest, RemoteTask>    successfulBuilds;

    private ExecutorService executor;
    private boolean         started;

    /** Optional pre-configured slave builders. */
    @com.google.inject.Inject(optional = true)
    @Named("builder.slave_builders")
    private String[] slaves = new String[0];

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
        successfulBuilds = new ConcurrentHashMap<>();
        builderServices = new ConcurrentHashMap<>();
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

    public List<RemoteBuilderServer> getRegisterBuilderServers() {
        return new ArrayList<>(builderServices.values());
    }

    /**
     * Register remote SlaveBuildService which can process builds.
     *
     * @param registration
     *         BuilderServerRegistration
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean registerBuilderServer(BuilderServerRegistration registration) throws BuilderException {
        checkStarted();
        final String url = registration.getBuilderServerLocation().getUrl();
        final RemoteBuilderServer builderServer = new RemoteBuilderServer(url);
        String workspace = null;
        String project = null;
        final BuilderServerAccessCriteria accessCriteria = registration.getBuilderServerAccessCriteria();
        if (accessCriteria != null) {
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }
        if (workspace != null) {
            builderServer.setAssignedWorkspace(workspace);
            if (project != null) {
                builderServer.setAssignedProject(project);
            }
        }
        return doRegisterBuilderServer(builderServer);
    }

    private boolean doRegisterBuilderServer(RemoteBuilderServer builderServer) throws BuilderException {
        final List<RemoteBuilder> toAdd = new LinkedList<>();
        for (BuilderDescriptor builderDescriptor : builderServer.getAvailableBuilders()) {
            toAdd.add(builderServer.createRemoteBuilder(builderDescriptor));
        }
        builderServices.put(builderServer.getBaseUrl(), builderServer);
        return registerBuilders(builderServer.getAssignedWorkspace(), builderServer.getAssignedProject(), toAdd);
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
     *         BuilderServerLocation
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean unregisterBuilderServer(BuilderServerLocation location) throws BuilderException {
        checkStarted();
        final String url = location.getUrl();
        if (url == null) {
            return false;
        }
        final RemoteBuilderServer builderServer = builderServices.remove(url);
        if (builderServer == null) {
            return false;
        }
        final List<RemoteBuilder> toRemove = new LinkedList<>();
        for (BuilderDescriptor builderDescriptor : builderServer.getAvailableBuilders()) {
            toRemove.add(builderServer.createRemoteBuilder(builderDescriptor));
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
        final ProjectDescriptor projectDescription = getProjectDescription(workspace, project, serviceContext);
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
        addParametersFromProjectDescriptor(projectDescription, request);
        final RemoteTask successfulTask = successfulBuilds.get(request);
        Callable<RemoteTask> callable = null;
        boolean reuse = false;
        if (successfulTask != null) {
            try {
                reuse = projectDescription.getModificationDate() < successfulTask.getBuildTaskDescriptor().getEndTime();
            } catch (Exception ignored) {
            }
            if (reuse) {
                LOG.debug("Reuse successful build {}", successfulTask);
                callable = new Callable<RemoteTask>() {
                    @Override
                    public RemoteTask call() throws Exception {
                        return successfulTask;
                    }
                };
            } else {
                successfulBuilds.remove(request);
            }
        }
        if (callable == null) {
            request.setTimeout(getBuildTimeout(request));
            callable = createTaskFor(request);
        }
        final Long id = sequence.getAndIncrement();
        final BuildFutureTask future = new BuildFutureTask(ThreadLocalPropagateContext.wrap(callable), id, workspace, project, reuse);
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
                return getBuilder(request).perform(request);
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
        addParametersFromProjectDescriptor(descriptor, request);
        request.setTimeout(getBuildTimeout(request));
        final Callable<RemoteTask> callable = createTaskFor(request);
        final Long id = sequence.getAndIncrement();
        final BuildFutureTask future = new BuildFutureTask(ThreadLocalPropagateContext.wrap(callable), id, workspace, project, false);
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
                return getBuilder(request).perform(request);
            }
        };
    }

    private void addParametersFromProjectDescriptor(ProjectDescriptor descriptor, BaseBuilderRequest request) throws BuilderException {
        final Map<String, List<String>> projectAttributes = descriptor.getAttributes();
        String builder = request.getBuilder();
        if (builder == null) {
            builder = getAttributeValue(Constants.BUILDER_NAME, projectAttributes);
            if (builder == null) {
                throw new BuilderException(
                        String.format("Name of builder is not specified, be sure property of project %s is set", Constants.BUILDER_NAME));
            }
            request.setBuilder(builder);
        }
        if (!hasBuilder(request)) {
            throw new BuilderException(String.format("Builder '%s' is not available. ", builder));
        }
        request.setProjectUrl(descriptor.getBaseUrl());
        final Link zipballLink = getLink(com.codenvy.api.project.server.Constants.LINK_REL_EXPORT_ZIP, descriptor.getLinks());
        if (zipballLink != null) {
            final String zipballLinkHref = zipballLink.getHref();
            final String token = getAuthenticationToken();
            request.setSourcesUrl(token != null ? String.format("%s?token=%s", zipballLinkHref, token) : zipballLinkHref);
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

    boolean hasBuilder(BaseBuilderRequest request) {
        final BuilderList builderList = getBuilderList(request.getWorkspace(), request.getProject());
        return builderList != null && builderList.hasBuilder(request.getBuilder());
    }

    protected BuilderList getBuilderList(String workspace, String project) {
        BuilderList builderList = builderListMapping.get(new ProjectWithWorkspace(project, workspace));
        if (builderList == null) {
            if (project != null || workspace != null) {
                if (workspace != null) {
                    // have dedicated builders for whole workspace (omit project) ?
                    builderList = builderListMapping.get(new ProjectWithWorkspace(null, workspace));
                }
                if (builderList == null) {
                    // seems there is no dedicated builders for specified request, use shared one then
                    builderList = builderListMapping.get(new ProjectWithWorkspace(null, null));
                }
            }
        }
        return builderList;
    }

    protected RemoteBuilder getBuilder(BaseBuilderRequest request) throws BuilderException {
        final BuilderList builderList = getBuilderList(request.getWorkspace(), request.getProject());
        if (builderList == null) {
            // Cannot continue, typically should never happen. At least shared builders should be available for everyone.
            throw new BuilderException("There is no any builder to process this request. ");
        }
        final RemoteBuilder builder = builderList.getBuilder(request);
        if (builder == null) {
            throw new BuilderException("There is no any builder available. ");
        }
        LOG.debug("Use slave builder {} at {}", builder.getName(), builder.getBaseUrl());
        return builder;
    }

    private long getBuildTimeout(BaseBuilderRequest request) throws BuilderException {
        // TODO: calculate in different way for different workspace/project.
        return timeout;
    }

    private String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
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
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                          new NamedThreadFactory("BuildQueue-", true)) {
            @Override
            protected void afterExecute(Runnable runnable, Throwable error) {
                super.afterExecute(runnable, error);
                if (runnable instanceof BuildFutureTask) {
                    final BuildFutureTask buildFutureTask = (BuildFutureTask)runnable;
                    if (buildFutureTask.reused) {
                        // Emulate event from remote builder. In fact we didn't send request to remote builder just reuse result from previous build.
                        eventService.publish(new BuildDoneEvent(buildFutureTask.id, buildFutureTask.workspace, buildFutureTask.project));
                    }
                }
            }
        };
        eventService.subscribe(new EventSubscriber<BuildDoneEvent>() {
            @Override
            public void onEvent(BuildDoneEvent event) {
                final long id = event.getTaskId();
                try {
                    final BuildQueueTask task = getTask(id);
                    final BaseBuilderRequest request = task.getRequest();
                    if (task.getDescriptor().getStatus() == BuildStatus.SUCCESSFUL) {
                        // Clone request and replace its id and timeout with 0.
                        successfulBuilds.put(DtoFactory.getInstance().clone(request).withId(0L).withTimeout(0L), task.getRemoteTask());
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
        eventService.subscribe(new EventSubscriber<BuildDoneEvent>() {
            @Override
            public void onEvent(BuildDoneEvent event) {
                try {
                    final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                    final long id = event.getTaskId();
                    bm.setChannel(String.format("builder:status:%d", id));
                    try {
                        bm.setBody(DtoFactory.getInstance().toJson(getTask(id).getDescriptor()));
                    } catch (BuilderException re) {
                        bm.setType(ChannelBroadcastMessage.Type.ERROR);
                        bm.setBody(String.format("{\"message\":\"%s\"}", re.getMessage()));
                    }
                    WSConnectionContext.sendMessage(bm);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
        if (slaves.length > 0) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    final LinkedList<RemoteBuilderServer> servers = new LinkedList<>();
                    for (String slave : slaves) {
                        try {
                            servers.add(new RemoteBuilderServer(slave));
                        } catch (IllegalArgumentException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                    final LinkedList<RemoteBuilderServer> offline = new LinkedList<>();
                    for (; ; ) {
                        while (!servers.isEmpty()) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            final RemoteBuilderServer server = servers.pop();
                            if (server.isAvailable()) {
                                try {
                                    doRegisterBuilderServer(server);
                                    LOG.debug("Pre-configured slave builder server {} registered. ", server.getBaseUrl());
                                } catch (BuilderException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                            } else {
                                LOG.warn("Pre-configured slave builder server {} isn't responding. ", server.getBaseUrl());
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
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        tasks.clear();
        builderListMapping.clear();
        successfulBuilds.clear();
        started = false;
    }

    protected EventService getEventService() {
        return eventService;
    }

    private static class BuildFutureTask extends FutureTask<RemoteTask> {
        final Long    id;
        final String  workspace;
        final String  project;
        final boolean reused;

        BuildFutureTask(Callable<RemoteTask> callable, Long id, String workspace, String project, boolean reused) {
            super(callable);
            this.id = id;
            this.workspace = workspace;
            this.project = project;
            this.reused = reused;
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


    private static class BuilderList {
        final Collection<RemoteBuilder> builders;
        final BuilderSelectionStrategy  builderSelector;

        BuilderList(BuilderSelectionStrategy builderSelector) {
            this.builderSelector = builderSelector;
            builders = new LinkedHashSet<>();
        }

        synchronized boolean hasBuilder(String name) {
            for (RemoteBuilder builder : builders) {
                if (name.equals(builder.getName())) {
                    return true;
                }
            }
            return false;
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
                    BuilderState builderState;
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
