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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.util.DownloadPlugin;
import com.codenvy.api.core.util.HttpDownloadPlugin;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.api.runner.ApplicationStatus;
import com.codenvy.api.runner.RunnerException;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerEnvironment;
import com.codenvy.api.runner.dto.RunnerMetric;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.lang.concurrent.ThreadLocalPropagateContext;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super-class for all implementation of Runner.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public abstract class Runner {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    /** @deprecated use {@link com.codenvy.api.runner.internal.Constants#DEPLOY_DIRECTORY} */
    public static final String DEPLOY_DIRECTORY   = Constants.DEPLOY_DIRECTORY;
    /** @deprecated use {@link com.codenvy.api.runner.internal.Constants#APP_CLEANUP_TIME} */
    public static final String CLEANUP_DELAY_TIME = Constants.APP_CLEANUP_TIME;

    private static final AtomicLong processIdSequence = new AtomicLong(1);

    private static final DeploymentSourcesValidator ALL_VALID = new DeploymentSourcesValidator() {
        @Override
        public boolean isValid(DeploymentSources deployment) {
            return true;
        }
    };

    private static final DeploymentSources NO_SOURCES = new DeploymentSources(null);

    private final Map<Long, RunnerProcessImpl> processes;
    private final Map<Long, RunnerProcessImpl> expiredProcesses;
    private final Map<Long, List<Disposer>>    applicationDisposers;
    private final Object                       applicationDisposersLock;
    private final AtomicInteger                runningAppsCounter;
    private final java.io.File                 deployDirectoryRoot;
    private final ResourceAllocators           allocators;
    private final EventService                 eventService;
    private final long                         cleanupDelayMillis;
    private final AtomicBoolean                started;
    private final long                         maxStartTime;

    private ExecutorService          executor;
    private ScheduledExecutorService cleanScheduler;
    private java.io.File             deployDirectory;

    protected final DownloadPlugin downloadPlugin;

    public Runner(java.io.File deployDirectoryRoot, int cleanupDelay, ResourceAllocators allocators, EventService eventService) {
        this.deployDirectoryRoot = deployDirectoryRoot;
        this.cleanupDelayMillis = TimeUnit.SECONDS.toMillis(cleanupDelay);
        this.maxStartTime = TimeUnit.MINUTES.toMillis(10); // TODO: configurable
        this.allocators = allocators;
        this.eventService = eventService;

        processes = new ConcurrentHashMap<>();
        expiredProcesses = new ConcurrentHashMap<>();
        applicationDisposers = new ConcurrentHashMap<>();
        applicationDisposersLock = new Object();
        runningAppsCounter = new AtomicInteger(0);
        downloadPlugin = new HttpDownloadPlugin();
        started = new AtomicBoolean(false);
    }

    /**
     * Returns the name of the runner. All registered runners should have unique name.
     *
     * @return the name of this runner
     */
    public abstract String getName();

    /**
     * Returns the description of the runner. Description should help client to recognize correct type of runner for an application.
     *
     * @return the description of this runner
     */
    public abstract String getDescription();

    /**
     * Gets environments that are supported by the runner. Each environment presupposes an existing some embedded pre-configured
     * environment
     * for running application, e.g. type of server or its configuration. By default this method returns empty map that means usage single
     * runtime environment for running an application.
     */
    public Map<String, RunnerEnvironment> getEnvironments() {
        return Collections.emptyMap();
    }

    /**
     * Gets global stats for this runner.
     *
     * @throws RunnerException
     *         if any error occurs while getting runner metrics
     */
    public List<RunnerMetric> getStats() throws RunnerException {
        List<RunnerMetric> global = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        global.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.TOTAL_APPS)
                             .withValue(Integer.toString(getTotalAppsNum())));
        global.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.RUNNING_APPS)
                             .withValue(Integer.toString(getRunningAppsNum())));
        return global;
    }

    public int getRunningAppsNum() {
        return runningAppsCounter.get();
    }

    public int getTotalAppsNum() {
        return processes.size();
    }

    /**
     * Gets root directory for deploy all applications.
     *
     * @return root directory for deploy all applications.
     */
    public java.io.File getDeployDirectory() {
        return deployDirectory;
    }

    /**
     * Gets process by its {@code id}.
     *
     * @param id
     *         id of process
     * @return runner process with specified id
     * @throws NotFoundException
     *         if id of RunnerProcess is invalid
     */
    public final RunnerProcess getProcess(Long id) throws NotFoundException {
        RunnerProcessImpl process = processes.get(id);
        if (process == null) {
            process = expiredProcesses.get(id);
            if (process == null) {
                throw new NotFoundException(String.format("Invalid run task id: %d", id));
            }
        }
        return process;
    }

    /**
     * Gets stats related to the specified process.
     *
     * @throws NotFoundException
     *         if id of RunnerProcess is invalid
     * @throws RunnerException
     *         if any other error occurs
     * @see #getProcess(Long)
     */
    public List<RunnerMetric> getStats(Long id) throws NotFoundException, RunnerException {
        return getStats(getProcess(id));
    }

    protected List<RunnerMetric> getStats(RunnerProcess process) throws RunnerException {
        final List<RunnerMetric> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final long started = process.getStartTime();
        final long stopped = process.getStopTime();
        if (started > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.START_TIME).withValue(Long.toString(started))
                                 .withDescription("Time when application was started"));
            if (stopped <= 0) {
                final long lifetime = process.getConfiguration().getRequest().getLifetime();
                final String terminationTime = lifetime >= Integer.MAX_VALUE ? RunnerMetric.ALWAYS_ON
                                                                             : Long.toString(started + TimeUnit.SECONDS.toMillis(lifetime));
                result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.TERMINATION_TIME).withValue(terminationTime)
                                     .withDescription("Time after that this application might be terminated"));
            }
        }
        if (stopped > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.STOP_TIME).withValue(Long.toString(stopped))
                                 .withDescription("Time when application was stopped"));
        }
        final long uptime = process.getUptime();
        if (uptime > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.UP_TIME).withValue(Long.toString(uptime))
                                 .withDescription("Application's uptime"));
        }
        return result;
    }

    public RunnerProcess execute(final RunRequest request) throws RunnerException {
        checkStarted();
        final long startTime = System.currentTimeMillis();
        final RunnerConfiguration runnerCfg = getRunnerConfigurationFactory().createRunnerConfiguration(request);
        final Long internalId = processIdSequence.getAndIncrement();
        final RunnerProcess.Callback callback = new RunnerProcess.Callback() {
            @Override
            public void started(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.startedEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject()));
            }

            @Override
            public void stopped(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.stoppedEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject()));
            }

            @Override
            public void error(RunnerProcess process, Throwable t) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.errorEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject(), t.getMessage()));
            }

            private void notify(RunnerEvent re) {
                try {
                    eventService.publish(re);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        final RunnerProcessImpl process = new RunnerProcessImpl(internalId, getName(), runnerCfg, callback);
        processes.put(internalId, process);
        final Watchdog watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", request.getLifetime(), TimeUnit.SECONDS);
        final int mem = runnerCfg.getMemory();
        final ResourceAllocator memoryAllocator = allocators.newMemoryAllocator(mem).allocate();
        final Runnable r = ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                try {
                    final java.io.File downloadDir =
                            Files.createTempDirectory(deployDirectory.toPath(), ("download_" + getName() + '_')).toFile();
                    final String url = request.getDeploymentSourcesUrl();
                    final DeploymentSources deploymentSources =
                            url == null ? NO_SOURCES : new DeploymentSources(downloadFile(url, downloadDir));
                    process.addToCleanupList(downloadDir);
                    if (!getDeploymentSourcesValidator().isValid(deploymentSources)) {
                        throw new RunnerException(
                                String.format("Unsupported project. Cannot deploy project %s from workspace %s with runner %s",
                                              request.getProject(), request.getWorkspace(), getName())
                        );
                    }
                    final ApplicationProcess realProcess = newApplicationProcess(deploymentSources, runnerCfg);
                    realProcess.start();
                    process.started(realProcess);
                    watcher.start(process);
                    runningAppsCounter.incrementAndGet();
                    LOG.debug("Started {}", process);
                    final long endTime = System.currentTimeMillis();
                    LOG.debug("Application {}/{} startup in {} ms", request.getWorkspace(), request.getProject(), (endTime - startTime));
                    realProcess.waitFor();
                    process.stopped();
                    LOG.debug("Stopped {}", process);
                } catch (Throwable e) {
                    process.setError(e);
                } finally {
                    watcher.stop();
                    memoryAllocator.release();
                    runningAppsCounter.decrementAndGet();
                }
            }
        });
        final FutureTask<Void> future = new FutureTask<>(r, null);
        process.setTask(future);
        executor.execute(future);
        return process;
    }

    /** @see RunnerConfiguration */
    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess newApplicationProcess(DeploymentSources toDeploy, RunnerConfiguration runnerCfg)
            throws RunnerException;

    protected ExecutorService getExecutor() {
        return executor;
    }

    protected EventService getEventService() {
        return eventService;
    }

    /**
     * Gets builder for DeploymentSources. By default this method returns builder that does nothing. Sub-classes may override this
     * method
     * and provide proper implementation of DeploymentSourcesValidator.
     *
     * @return builder for DeploymentSources
     */
    protected DeploymentSourcesValidator getDeploymentSourcesValidator() {
        return ALL_VALID;
    }

    protected java.io.File downloadFile(String url, java.io.File downloadDir) throws IOException {
        final ValueHolder<IOException> errorHolder = new ValueHolder<>();
        final ValueHolder<java.io.File> resultHolder = new ValueHolder<>();
        downloadPlugin.download(url, downloadDir, new DownloadPlugin.Callback() {
            @Override
            public void done(java.io.File downloaded) {
                resultHolder.set(downloaded);
            }

            @Override
            public void error(IOException e) {
                LOG.error(e.getMessage(), e);
                errorHolder.set(e);
            }
        });
        final IOException ioError = errorHolder.get();
        if (ioError != null) {
            throw ioError;
        }
        return resultHolder.get();
    }

    protected void registerDisposer(ApplicationProcess application, Disposer disposer) {
        final Long id = application.getId();
        synchronized (applicationDisposersLock) {
            List<Disposer> disposers = applicationDisposers.get(id);
            if (disposers == null) {
                applicationDisposers.put(id, disposers = new LinkedList<>());
            }
            disposers.add(0, disposer);
        }
    }

    private class RunnerProcessImpl implements RunnerProcess {
        private final Long                id;
        private final String              runner;
        private final RunnerConfiguration configuration;
        private final Callback            callback;
        private final long                created;

        private Future<Void>       task;
        private ApplicationProcess realProcess;
        private long               startTime;
        private long               stopTime;
        private Throwable          error;
        private List<java.io.File> forCleanup;
        private ApplicationStatus  status;

        RunnerProcessImpl(Long id, String runner, RunnerConfiguration configuration, Callback callback) {
            this.id = id;
            this.runner = runner;
            this.configuration = configuration;
            this.callback = callback;
            created = System.currentTimeMillis();
            startTime = -1L;
            stopTime = -1L;
            status = ApplicationStatus.NEW;
        }

        synchronized void setTask(Future<Void> task) {
            this.task = task;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public synchronized ApplicationStatus getStatus() {
            return status;
        }

        @Override
        public synchronized long getStartTime() {
            return startTime;
        }

        @Override
        public synchronized long getStopTime() {
            return stopTime;
        }

        @Override
        public synchronized long getUptime() {
            return startTime > 0
                   ? stopTime > 0
                     ? (stopTime - startTime) : (System.currentTimeMillis() - startTime)
                   : 0;
        }

        @Override
        public synchronized ApplicationProcess getApplicationProcess() {
            return realProcess;
        }

        @Override
        public synchronized Throwable getError() {
            return error;
        }

        synchronized void started(ApplicationProcess realProcess) {
            this.realProcess = realProcess;
            startTime = System.currentTimeMillis();
            status = ApplicationStatus.RUNNING;
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.started(RunnerProcessImpl.this);
                    }
                }));
            }
        }

        synchronized void stopped() {
            stopTime = System.currentTimeMillis();
            if (status != ApplicationStatus.CANCELLED) {
                // save 'cancelled' status
                status = ApplicationStatus.STOPPED;
            }
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.stopped(RunnerProcessImpl.this);
                    }
                }));
            }
        }

        @Override
        public String getRunner() {
            return runner;
        }

        @Override
        public RunnerConfiguration getConfiguration() {
            return configuration;
        }

        synchronized void setError(final Throwable error) {
            this.error = error;
            status = ApplicationStatus.FAILED;
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.error(RunnerProcessImpl.this, error);
                    }
                }));
            }
        }

        @Override
        public synchronized void cancel() throws Exception {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
            if (realProcess != null && realProcess.isRunning()) {
                realProcess.stop();
            }
            status = ApplicationStatus.CANCELLED;
        }

        synchronized void addToCleanupList(java.io.File file) {
            if (forCleanup == null) {
                forCleanup = new LinkedList<>();
            }
            forCleanup.add(file);
        }

        synchronized List<java.io.File> getCleanupList() {
            return forCleanup;
        }

        synchronized boolean isExpired() {
            return (startTime < 0 && ((created + maxStartTime) < System.currentTimeMillis())) ||
                   (stopTime > 0 && ((stopTime + cleanupDelayMillis) < System.currentTimeMillis()));
        }

        @Override
        public String toString() {
            return "RunnerProcessImpl{" +
                   "\nworkspace='" + configuration.getRequest().getWorkspace() + '\'' +
                   "\nproject='" + configuration.getRequest().getProject() + '\'' +
                   "\nrunner='" + runner + '\'' +
                   "\ncreated=" + created +
                   "\nstartTime=" + startTime +
                   "\nstopTime=" + stopTime +
                   "\nid=" + id +
                   "\n}";
        }
    }

    /** Initializes Runner. Sub-classes should invoke {@code super.start} at the begin of this method. */
    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            deployDirectory = new java.io.File(deployDirectoryRoot, getName());
            if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
                throw new IllegalStateException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
            }
            executor = Executors.newCachedThreadPool(new NamedThreadFactory(getName() + "-Runner-", true));
            cleanScheduler =
                    Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName() + "-RunnerCleanSchedulerPool-", true));
            cleanScheduler.scheduleAtFixedRate(new CleanupTask(), 1, 1, TimeUnit.MINUTES);
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    private class CleanupTask implements Runnable {
        public void run() {
            for (Iterator<RunnerProcessImpl> i = expiredProcesses.values().iterator(); i.hasNext(); ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final RunnerProcessImpl process = i.next();
                i.remove();
                Disposer[] appDisposers = null;
                final ApplicationProcess realProcess = process.realProcess;
                if (realProcess != null) {
                    synchronized (applicationDisposersLock) {
                        final List<Disposer> disposers = applicationDisposers.remove(realProcess.getId());
                        if (disposers != null) {
                            appDisposers = disposers.toArray(new Disposer[disposers.size()]);
                        }
                    }
                }
                if (appDisposers != null) {
                    for (Disposer disposer : appDisposers) {
                        try {
                            disposer.dispose();
                        } catch (RuntimeException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                final List<java.io.File> cleanupList = process.getCleanupList();
                if (cleanupList != null) {
                    for (java.io.File file : cleanupList) {
                        if (!IoUtil.deleteRecursive(file)) {
                            LOG.warn("Failed delete {}", file);
                        }
                    }
                }
            }
            for (Iterator<RunnerProcessImpl> i = processes.values().iterator(); i.hasNext(); ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final RunnerProcessImpl process = i.next();
                if (process.isExpired()) {
                    try {
                        process.cancel();
                        if (process.getApplicationProcess() == null) {
                            process.setError(new RunnerException(
                                    "Running process is terminated due to exceeded max allowed time for start."));
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        continue; // try next time
                    }
                    i.remove();
                    expiredProcesses.put(process.getId(), process);
                }
            }
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    /**
     * Stops Runner and releases any resources associated with the Runner.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
    @PreDestroy
    public void stop() {
        if (started.compareAndSet(true, false)) {
            boolean interrupted = false;
            cleanScheduler.shutdownNow();
            try {
                if (!cleanScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate cleanup scheduler");
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
            final List<Disposer> allDisposers = new LinkedList<>();
            synchronized (applicationDisposersLock) {
                for (List<Disposer> disposers : applicationDisposers.values()) {
                    if (disposers != null) {
                        allDisposers.addAll(disposers);
                    }
                }
                applicationDisposers.clear();
            }
            for (Disposer disposer : allDisposers) {
                try {
                    disposer.dispose();
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            final java.io.File[] files = getDeployDirectory().listFiles();
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
            processes.clear();
            expiredProcesses.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }
}
