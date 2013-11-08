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
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.BuildStatus;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.LifecycleException;
import com.codenvy.api.core.config.Configurable;
import com.codenvy.api.core.config.Configuration;
import com.codenvy.api.core.rest.DownloadPlugin;
import com.codenvy.api.core.rest.FileAdapter;
import com.codenvy.api.core.rest.RemoteContent;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.CancellableProcessWrapper;
import com.codenvy.api.core.util.CommandLine;
import com.codenvy.api.core.util.ComponentLoader;
import com.codenvy.api.core.util.ProcessUtil;
import com.codenvy.api.core.util.StreamPump;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.lang.ZipUtils;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super-class for all implementation of Builder.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public abstract class Builder implements Configurable, Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    /**
     * Name of configuration parameter that points to the directory where all builds stored. Is such parameter is not specified then
     * 'java.io.tmpdir' used.
     */
    public static final String REPOSITORY              = "builder.build_repository";
    /**
     * Name of configuration parameter that sets the number of build workers. In other words it set the number of build
     * process that can be run at the same time. If this parameter is not set then the number of available processors
     * used, e.g. {@code Runtime.getRuntime().availableProcessors();}
     */
    public static final String NUMBER_OF_WORKERS       = "builder.workers_number";
    /**
     * Name of configuration parameter that sets time (in minutes) of keeping the results (artifact and logs) of build (by default 60
     * minutes). After this time the results of build may be removed.
     */
    public static final String CLEAN_RESULT_DELAY_TIME = "builder.clean_result_delay_time";
    /**
     * Name of parameter that set the max size of build queue (by default 100). The number of build task in queue may not be greater than
     * provided by this parameter.
     */
    public static final String INTERNAL_QUEUE_SIZE     = "builder.internal_queue_size";
    /** Name of configuration parameter that provides build timeout is seconds (by default 300). After this time build may be terminated. */
    public static final String TIMEOUT                 = "builder.build_timeout";

    private final AtomicLong                             buildIdSequence;
    private final ConcurrentMap<Long, CachedBuildTask>   tasks;
    private final ConcurrentLinkedQueue<CachedBuildTask> tasksFIFO;
    private final ConcurrentLinkedQueue<java.io.File>    cleanerQueue;
    private final Set<BuildListener>                     buildListeners;

    private int queueSize;
    private int timeout;
    private int cleanBuildResultDelay;

    private volatile boolean                  maySetConfiguration;
    private          Configuration            configuration;
    private          ScheduledExecutorService cleaner;
    private          ThreadPoolExecutor       executor;
    private          java.io.File             repository;

    public Builder() {
        buildIdSequence = new AtomicLong(1);
        buildListeners = new LinkedHashSet<>();
        tasks = new ConcurrentHashMap<>();
        tasksFIFO = new ConcurrentLinkedQueue<>();
        cleanerQueue = new ConcurrentLinkedQueue<>();
        maySetConfiguration = true;
    }

    /**
     * Returns the name of the builder. All registered builders should have unique name.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Returns the description of builder. Description should help client to recognize correct type of builder for an application.
     *
     * @return the description of builder
     */
    public abstract String getDescription();

    /**
     * Get result of FutureBuildTask. Getting result is implementation specific and mostly depends to build system, e.g. maven usually
     * stores build result in directory 'target' but it is not rule for ant. Regular users are not expected to use this method directly.
     * They should always use method {@link BuildTask#getResult()} instead.
     *
     * @param task
     *         task
     * @param successful
     *         reports whether build process terminated normally or not.
     *         Note: {@code true} is not indicated successful build but only normal process termination. Build itself may be unsuccessful
     *         because to compilation error, failed tests, etc.
     * @return BuildResult
     * @throws BuilderException
     *         if an error occurs when try to get result
     * @see BuildTask#getResult()
     */
    protected abstract BuildResult getTaskResult(FutureBuildTask task, boolean successful) throws BuilderException;

    protected abstract CommandLine createCommandLine(BuildTaskConfiguration config) throws BuilderException;

    protected BuildLogger createBuildLogger(BuildTaskConfiguration buildConfiguration, java.io.File logFile) throws BuilderException {
        try {
            return new DefaultBuildLogger(logFile, "text/plain");
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Note: Sub-classes should copy default configuration from super class.
     * <pre>
     * &#064Override
     * public Configuration getDefaultConfiguration() {
     *     Configuration superConf = super.getDefaultConfiguration();
     *     Configuration myConf = new Configuration(superConf);
     *     // add new parameters or update parameters provided by method from super class
     *     return myConf;
     * }
     * </pre>
     */
    @Override
    public Configuration getDefaultConfiguration() {
        final Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setFile(REPOSITORY, new java.io.File(System.getProperty("java.io.tmpdir")));
        defaultConfiguration.setInt(NUMBER_OF_WORKERS, Runtime.getRuntime().availableProcessors());
        defaultConfiguration.setInt(INTERNAL_QUEUE_SIZE, 100);
        defaultConfiguration.setInt(CLEAN_RESULT_DELAY_TIME, 60);
        defaultConfiguration.setInt(TIMEOUT, 300);
        return defaultConfiguration;
    }

    @Override
    public final void setConfiguration(Configuration configuration) {
        if (maySetConfiguration) {
            this.configuration = new Configuration(configuration);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public final Configuration getConfiguration() {
        Configuration myConfiguration = this.configuration;
        if (myConfiguration != null) {
            return new Configuration(myConfiguration);
        }
        return getDefaultConfiguration();
    }

    /** Initialize Builder. Sub-classes should invoke {@code super.start} at the begin of this method. */
    public void start() {
        maySetConfiguration = false;
        final Configuration myConfiguration = getConfiguration();
        LOG.debug("{}", myConfiguration);
        final java.io.File path = myConfiguration.getFile(REPOSITORY, new java.io.File(System.getProperty("java.io.tmpdir")));
        repository = new java.io.File(path, getName());
        if (!(repository.exists() || repository.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", repository.getAbsolutePath()));
        }
        int workerNumber = myConfiguration.getInt(NUMBER_OF_WORKERS, Runtime.getRuntime().availableProcessors());
        queueSize = myConfiguration.getInt(INTERNAL_QUEUE_SIZE, 100);
        cleanBuildResultDelay = myConfiguration.getInt(CLEAN_RESULT_DELAY_TIME, 60);
        timeout = myConfiguration.getInt(TIMEOUT, -1); // Do not restore from default is caller don't want it.
        executor = new MyThreadPoolExecutor(workerNumber, queueSize);
        cleaner = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName() + "-BuilderCleaner-", true));
        cleaner.scheduleAtFixedRate(new CleanTask(), cleanBuildResultDelay, cleanBuildResultDelay, TimeUnit.MINUTES);
        synchronized (buildListeners) {
            for (BuildListener listener : ComponentLoader.all(BuildListener.class)) {
                buildListeners.add(listener);
            }
        }
    }

    /**
     * Stops builder and releases any resources associated with the Builder.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
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
        } finally {
            cleaner.shutdownNow();

            // Remove all build results.
            final java.io.File[] files = getRepository().listFiles();
            if (files != null && files.length > 0) {
                for (java.io.File f : files) {
                    boolean deleted;
                    if (f.isDirectory()) {
                        deleted = IoUtil.deleteRecursive(f);
                    } else {
                        deleted = f.delete();
                    }
                    if (!deleted) {
                        LOG.warn("Failed delete {}", f);
                    }
                }
            }
        }
    }

    public java.io.File getRepository() {
        return repository;
    }

    /**
     * Add new BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was added
     */
    public boolean addBuildListener(BuildListener listener) {
        synchronized (buildListeners) {
            return buildListeners.add(listener);
        }
    }

    /**
     * Remove BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was removed
     */
    public boolean removeBuildListener(BuildListener listener) {
        synchronized (buildListeners) {
            return buildListeners.remove(listener);
        }
    }

    /**
     * Get all registered build listeners. Modifications to the returned {@code Set} will not affect the internal {@code Set}.
     *
     * @return all available download plugins
     */
    public Set<BuildListener> getBuildListeners() {
        synchronized (buildListeners) {
            return new LinkedHashSet<>(buildListeners);
        }
    }

    /**
     * Starts new build process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(BuildRequest request) throws BuilderException {
        final BuildTaskConfiguration buildConfiguration;
        try {
            buildConfiguration = BuildTaskConfiguration.newBuildConfiguration(this, request);
        } catch (IOException e) {
            throw new BuilderException(e);
        }
        final java.io.File srcDir = buildConfiguration.getSources().getDirectory().getIoFile();
        final java.io.File logFile = new java.io.File(srcDir.getParentFile(), srcDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(buildConfiguration, logFile);
        return execute(buildConfiguration, null, logger);
    }

    /**
     * Starts new process of analysis dependencies.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(DependencyRequest request) throws BuilderException {
        final BuildTaskConfiguration buildConfiguration;
        try {
            buildConfiguration = BuildTaskConfiguration.newDependencyAnalysisConfiguration(this, request);
        } catch (IOException e) {
            throw new BuilderException(e);
        }
        final java.io.File srcDir = buildConfiguration.getSources().getDirectory().getIoFile();
        final java.io.File logFile = new java.io.File(srcDir.getParentFile(), srcDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(buildConfiguration, logFile);
        return execute(buildConfiguration, null, logger);
    }

    protected BuildTask execute(BuildTaskConfiguration config, BuildTask.Callback callback, BuildLogger logger) throws BuilderException {
        final CommandLine commandLine = createCommandLine(config);
        final Callable<Boolean> callable = createTaskFor(commandLine, config.getSources(), logger);
        final FutureBuildTask task =
                new FutureBuildTask(callable, buildIdSequence.getAndIncrement(), commandLine, getName(), config, logger, callback);
        final long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cleanBuildResultDelay);
        final CachedBuildTask cachedTask = new CachedBuildTask(task, expirationTime);
        purgeExpiredTasks();
        tasks.put(task.getId(), cachedTask);
        tasksFIFO.offer(cachedTask);
        executor.execute(task);
        return task;
    }

    protected Callable<Boolean> createTaskFor(final CommandLine commandLine, final RemoteContent sources, final BuildLogger logger) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                downloadSources(sources);
                StreamPump output = null;
                Watchdog watcher = null;
                int result = -1;
                try {
                    final java.io.File srcDir = sources.getDirectory().getIoFile();
                    final Process process = Runtime.getRuntime().exec(commandLine.toShellCommand(), null, srcDir);
                    if (timeout > 0) {
                        watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", timeout, TimeUnit.SECONDS);
                        watcher.start(new CancellableProcessWrapper(process));
                    }
                    output = new StreamPump();
                    output.start(process, logger);
                    try {
                        result = process.waitFor();
                    } catch (InterruptedException e) {
                        Thread.interrupted(); // we interrupt thread when cancel task
                        ProcessUtil.kill(process);
                    }
                } finally {
                    if (watcher != null) {
                        watcher.stop();
                    }
                    if (output != null) {
                        output.stop();
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.info("Done: {}, exit code: {}", commandLine, result);
                }
                return result == 0;
            }
        };
    }

    /**
     * Downloads remote sources.
     *
     * @param sources
     *         remote sources
     * @throws BuilderException
     *         if an error occurs when try to download source
     */
    protected void downloadSources(RemoteContent sources) throws BuilderException {
        final IOException[] errorHolder = new IOException[1];
        sources.download(new DownloadPlugin.Callback() {
            @Override
            public void done(java.io.File downloaded) {
                try {
                    if (ZipUtils.isZipFile(downloaded)) {
                        ZipUtils.unzip(downloaded, downloaded.getParentFile());
                        if (!downloaded.delete()) {
                            LOG.warn("Failed delete {}", downloaded);
                        }
                    }
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    errorHolder[0] = e;
                }
            }

            @Override
            public void error(IOException e) {
                LOG.error(e.getMessage(), e);
                errorHolder[0] = e;
            }
        });
        if (errorHolder[0] != null) {
            throw new BuilderException(errorHolder[0]);
        }
    }

    public int getNumberOfWorkers() {
        return executor.getCorePoolSize();
    }

    public int getNumberOfActiveWorkers() {
        return executor.getActiveCount();
    }

    public int getInternalQueueSize() {
        return executor.getQueue().size();
    }

    public int getMaxInternalQueueSize() {
        return queueSize;
    }

    /** Removes expired tasks. */
    private void purgeExpiredTasks() {
        int num = 0;
        for (Iterator<CachedBuildTask> i = tasksFIFO.iterator(); i.hasNext(); ) {
            final CachedBuildTask next = i.next();
            if (!next.isExpired()) {
                // Don't need to check other tasks if find first one that is not expired yet.
                break;
            }
            if (!next.task.isDone()) {
                try {
                    next.task.cancel();
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                    continue; // try next time
                }
            }
            i.remove();
            tasks.remove(next.task.getId());
            try {
                cleanup(next.task);
            } catch (RuntimeException e) {
                LOG.error(e.getMessage(), e);
            }
            num++;
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks", num);
        }
    }

    /**
     * Cleanup task. Cleanup means removing all local files which were created by build process, e.g logs, sources, build reports, etc.
     * <p/>
     * Sub-classes should invoke {@code super.cleanup} at the start of this method.
     *
     * @param task
     *         build task
     */
    protected void cleanup(BuildTask task) {
        final java.io.File sources = task.getSources().getDirectory().getIoFile();
        if (sources != null && sources.exists()) {
            cleanerQueue.offer(sources);
        }
        final java.io.File log = task.getBuildLogger().getFile();
        if (log != null && log.exists()) {
            cleanerQueue.offer(log);
        }
        BuildResult result = null;
        try {
            result = task.getResult();
        } catch (BuilderException e) {
            LOG.error("Skip cleanup of the task {}. Unable get task result.", task);
        }
        if (result != null) {
            List<FileAdapter> artifacts = result.getResultUnits();
            if (!artifacts.isEmpty()) {
                for (FileAdapter artifact : artifacts) {
                    cleanerQueue.offer(artifact.getIoFile());
                }
            }
            if (result.hasBuildReport()) {
                java.io.File report = result.getBuildReport().getIoFile();
                if (report != null && report.exists()) {
                    cleanerQueue.offer(report);
                }
            }
        }
    }

    /**
     * Get build task by its {@code id}. Typically build process takes some time, so client start process of build or analyze dependencies
     * and periodically check is process already done. Client also may use {@link BuildListener} to be notified when build process starts
     * or
     * ends.
     *
     * @param id
     *         id of BuildTask
     * @return BuildTask
     * @throws NoSuchBuildTaskException
     *         if id of BuildTask is invalid
     * @see #addBuildListener(BuildListener)
     * @see #removeBuildListener(BuildListener)
     */
    public final BuildTask getBuildTask(Long id) throws NoSuchBuildTaskException {
        final CachedBuildTask e = tasks.get(id);
        if (e == null) {
            throw new NoSuchBuildTaskException(id);
        }
        return e.task;
    }

    protected class FutureBuildTask extends FutureTask<Boolean> implements BuildTask {
        private final Long                   id;
        private final CommandLine            commandLine;
        private final String                 builder;
        private final BuildTaskConfiguration configuration;
        private final BuildLogger            buildLogger;
        private final Callback               callback;

        private BuildResult result;
        private long        startTime;

        protected FutureBuildTask(Callable<Boolean> callable,
                                  Long id,
                                  CommandLine commandLine,
                                  String builder,
                                  BuildTaskConfiguration configuration,
                                  BuildLogger buildLogger,
                                  Callback callback) {
            super(callable);
            this.id = id;
            this.commandLine = commandLine;
            this.builder = builder;
            this.configuration = configuration;
            this.buildLogger = buildLogger;
            this.callback = callback;
            startTime = -1L;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getBuilder() {
            return builder;
        }

        public CommandLine getCommandLine() {
            return commandLine;
        }

        @Override
        public RemoteContent getSources() {
            return configuration.getSources();
        }

        @Override
        public BuildLogger getBuildLogger() {
            return buildLogger;
        }

        @Override
        public void cancel() {
            super.cancel(true);
        }

        @Override
        protected void done() {
            if (callback != null) {
                callback.done(this);
            }
        }

        @Override
        public final BuildResult getResult() throws BuilderException {
            if (!isDone()) {
                return null;
            }
            if (result == null) {
                boolean successful;
                try {
                    successful = super.get();
                } catch (InterruptedException e) {
                    // Should not happen since we checked is task done or not.
                    Thread.currentThread().interrupt();
                    successful = false;
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof Error) {
                        throw (Error)cause; // lets caller to get Error as is
                    } else if (cause instanceof BuilderException) {
                        throw (BuilderException)cause;
                    } else {
                        throw new BuilderException(cause.getMessage(), cause);
                    }
                } catch (CancellationException ce) {
                    successful = false;
                }

                result = Builder.this.getTaskResult(this, successful);
            }
            return result;
        }

        @Override
        public BuildTaskDescriptor getDescriptor(ServiceContext restfulRequestContext) throws BuilderException {
            final String builder = getBuilder();
            final Long taskId = getId();
            final BuildResult result = getResult();
            final BuildStatus status = isDone()
                                       ? (isCancelled() ? BuildStatus.CANCELLED
                                                        : (result.isSuccessful() ? BuildStatus.SUCCESSFUL : BuildStatus.FAILED))
                                       : (isStarted() ? BuildStatus.IN_PROGRESS : BuildStatus.IN_QUEUE);
            final List<Link> links = new ArrayList<>();
            final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_GET_STATUS)
                                .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "getStatus")
                                                            .build(builder, taskId).toString())
                                .withMethod("GET")
                                .withProduces(MediaType.APPLICATION_JSON));

            if (status == BuildStatus.IN_QUEUE || status == BuildStatus.IN_PROGRESS) {
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_CANCEL)
                                    .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "cancel")
                                                                .build(builder, taskId).toString())
                                    .withMethod("POST")
                                    .withProduces(MediaType.APPLICATION_JSON));
            }

            if (status != BuildStatus.IN_QUEUE) {
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_VIEW_LOG)
                                    .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "getLogs")
                                                                .build(builder, taskId).toString())
                                    .withMethod("GET")
                                    .withProduces(getBuildLogger().getContentType()));
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_BROWSE)
                                    .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "browse").queryParam("path", "/")
                                                                .build(builder, taskId).toString())
                                    .withMethod("GET")
                                    .withProduces(MediaType.TEXT_HTML));
            }

            if (status == BuildStatus.SUCCESSFUL) {
                for (FileAdapter ru : result.getResultUnits()) {
                    links.add(DtoFactory.getInstance().createDto(Link.class)
                                        .withRel(Constants.LINK_REL_DOWNLOAD_RESULT)
                                        .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "download")
                                                                    .queryParam("path", ru.getHref()).build(builder, taskId).toString())
                                        .withMethod("GET").withProduces(ru.getContentType()));
                }
            }

            if ((status == BuildStatus.SUCCESSFUL || status == BuildStatus.FAILED) && result.hasBuildReport()) {
                final FileAdapter br = result.getBuildReport();
                if (br.isDirectory()) {
                    links.add(DtoFactory.getInstance().createDto(Link.class)
                                        .withRel(Constants.LINK_REL_VIEW_REPORT)
                                        .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "browse")
                                                                    .queryParam("path", br.getHref()).build(builder, taskId).toString())
                                        .withMethod("GET")
                                        .withProduces(MediaType.TEXT_HTML));
                } else {
                    links.add(DtoFactory.getInstance().createDto(Link.class)
                                        .withRel(Constants.LINK_REL_VIEW_REPORT)
                                        .withHref(servicePathBuilder.clone().path(SlaveBuilderService.class, "view")
                                                                    .queryParam("path", br.getHref()).build(builder, taskId).toString())
                                        .withMethod("GET")
                                        .withProduces(br.getContentType()));
                }
            }

            return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                             .withTaskId(taskId)
                             .withStatus(status)
                             .withLinks(links)
                             .withStartTime(getStartTime());
        }

        @Override
        public BuildTaskConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public final synchronized boolean isStarted() {
            return startTime > 0;
        }

        @Override
        public final synchronized long getStartTime() {
            return startTime;
        }

        final synchronized void started() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "FutureBuildTask{" +
                   "id=" + id +
                   ", builder='" + builder + '\'' +
                   ", sources=" + configuration.getSources().getDirectory() +
                   '}';
        }
    }

    private class MyThreadPoolExecutor extends ThreadPoolExecutor {
        private MyThreadPoolExecutor(int workerNumber, int queueSize) {
            super(workerNumber, workerNumber, 0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>(queueSize),
                  new NamedThreadFactory(Builder.this.getName() + "-Builder-", true),
                  new ManyBuildTasksRejectedExecutionPolicy(new AbortPolicy()));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            final FutureBuildTask futureBuildTask = (FutureBuildTask)r; // We know it is FutureBuildTask
            for (BuildListener buildListener : getBuildListeners()) {
                try {
                    buildListener.begin(futureBuildTask);
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            futureBuildTask.started();
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            final FutureBuildTask futureBuildTask = (FutureBuildTask)r; // We know it is FutureBuildTask
            for (BuildListener buildListener : getBuildListeners()) {
                try {
                    buildListener.end(futureBuildTask);
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private class CleanTask implements Runnable {
        public void run() {
            LOG.debug("clean {}: remove {} files", new java.util.Date(), cleanerQueue.size());
            Set<java.io.File> failToDelete = new LinkedHashSet<>();
            java.io.File f;
            while ((f = cleanerQueue.poll()) != null) {
                if (f.isDirectory()) {
                    if (!IoUtil.deleteRecursive(f)) {
                        if (f.exists()) {
                            failToDelete.add(f);
                        }
                    }
                } else {
                    if (!f.delete()) {
                        if (f.exists()) {
                            failToDelete.add(f);
                        }
                    }
                }
            }
            if (!failToDelete.isEmpty()) {
                LOG.debug("clean: could remove {} files, try next time", failToDelete.size());
                cleanerQueue.addAll(failToDelete);
            }
        }
    }

    private static final class CachedBuildTask {
        private final long            expirationTime;
        private final int             hash;
        private final FutureBuildTask task;

        private CachedBuildTask(FutureBuildTask task, long expirationTime) {
            this.task = task;
            this.expirationTime = expirationTime;
            this.hash = 7 * 31 + task.getId().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof CachedBuildTask) {
                return task.getId().equals(((CachedBuildTask)o).task.getId());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "CachedBuildTask{" +
                   "task=" + task +
                   '}';
        }
    }
}
