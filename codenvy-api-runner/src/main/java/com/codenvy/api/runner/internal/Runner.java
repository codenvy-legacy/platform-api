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
package com.codenvy.api.runner.internal;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.notification.EventService;
import com.codenvy.api.core.util.DownloadPlugin;
import com.codenvy.api.core.util.HttpDownloadPlugin;
import com.codenvy.api.core.util.ValueHolder;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.api.runner.dto.RunRequest;
import com.codenvy.api.runner.dto.RunnerEnvironment;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.commons.lang.concurrent.ThreadLocalPropagateContext;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.codenvy.api.runner.internal.RunnerEvent.EventType;

/**
 * Super-class for all implementation of Runner.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public abstract class Runner {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    public static final String DEPLOY_DIRECTORY   = "runner.internal.deploy_directory";
    public static final String CLEANUP_DELAY_TIME = "runner.internal.clean_delay_time";

    private static final AtomicLong processIdSequence = new AtomicLong(1);

    private final Map<Long, RunnerProcessEntry> processes;
    private final Map<Long, List<Disposer>>     applicationDisposers;
    private final Object                        applicationDisposersLock;
    private final AtomicInteger                 runningAppsCounter;
    private final java.io.File                  deployDirectoryRoot;
    private final DownloadPlugin                downloadPlugin;
    private final ResourceAllocators            allocators;
    private final EventService                  eventService;
    private final long                          cleanupDelayMillis;

    private ExecutorService          executor;
    private ScheduledExecutorService scheduler;
    private java.io.File             deployDirectory;
    private boolean                  started;

    public Runner(java.io.File deployDirectoryRoot, int cleanupDelay, ResourceAllocators allocators, EventService eventService) {
        this.deployDirectoryRoot = deployDirectoryRoot;
        this.cleanupDelayMillis = TimeUnit.SECONDS.toMillis(cleanupDelay);
        this.allocators = allocators;
        this.eventService = eventService;
        processes = new ConcurrentHashMap<>();
        applicationDisposers = new ConcurrentHashMap<>();
        applicationDisposersLock = new Object();
        runningAppsCounter = new AtomicInteger(0);
        downloadPlugin = new HttpDownloadPlugin();
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
     * Get environments that are supported by the runner. Each environment presupposes an existing some embedded pre-configured environment
     * for running application, e.g. type of server or its configuration. By default this method returns empty list that means usage of one
     * runtime environment for running an application.
     */
    public Map<String, RunnerEnvironment> getEnvironments() {
        return Collections.emptyMap();
    }

    /** @see RunnerConfiguration */
    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess newApplicationProcess(DeploymentSources toDeploy, RunnerConfiguration runnerCfg)
            throws ApiException;

    protected ExecutorService getExecutor() {
        return executor;
    }

    protected EventService getEventService() {
        return eventService;
    }

    private static final DeploymentSourcesValidator ALL_VALID = new DeploymentSourcesValidator() {
        @Override
        public boolean isValid(DeploymentSources deployment) {
            return true;
        }
    };

    /**
     * Get validator for DeploymentSources. By default this method returns validator that does nothing. Sub-classes may override this
     * method
     * and provide proper implementation of DeploymentSourcesValidator.
     *
     * @return validator for DeploymentSources
     */
    protected DeploymentSourcesValidator getDeploymentSourcesValidator() {
        return ALL_VALID;
    }

    /** Initialize Runner. Sub-classes should invoke {@code super.start} at the begin of this method. */
    @PostConstruct
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        deployDirectory = new java.io.File(deployDirectoryRoot, getName());
        if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
            throw new IllegalStateException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
        }
        executor = Executors.newCachedThreadPool(new NamedThreadFactory(getName() + "-Runner-", true));
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName() + "-RunnerSchedulerPool-", true));
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                for (Iterator<RunnerProcessEntry> i = processes.values().iterator(); i.hasNext(); ) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    final RunnerProcessEntry next = i.next();
                    if (next.isExpired()) {
                        try {
                            if (next.process.isRunning()) {
                                next.process.cancel();
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                            continue; // try next time
                        }
                        i.remove();
                        Disposer[] appDisposers = null;
                        final ApplicationProcess realProcess = next.process.realProcess;
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
                        final List<java.io.File> cleanupList = next.process.getCleanupList();
                        if (cleanupList != null) {
                            for (java.io.File file : cleanupList) {
                                if (!IoUtil.deleteRecursive(file)) {
                                    LOG.warn("Failed delete {}", file);
                                }
                            }
                        }
                    }
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    /**
     * Stops Runner and releases any resources associated with the Runner.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
    @PreDestroy
    public synchronized void stop() {
        checkStarted();
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
        started = false;
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get root directory for deploy all applications.
     *
     * @return root directory for deploy all applications.
     */
    public java.io.File getDeployDirectory() {
        return deployDirectory;
    }

    /**
     * Get process by its {@code id}.
     *
     * @param id
     *         id of process
     * @return runner process with specified id
     * @throws NotFoundException
     *         if id of RunnerProcess is invalid
     */
    public RunnerProcess getProcess(Long id) throws NotFoundException {
        checkStarted();
        final RunnerProcessEntry wrapper = processes.get(id);
        if (wrapper == null) {
            throw new NotFoundException(String.format("Invalid run task id: %d", id));
        }
        return wrapper.process;
    }

    public RunnerProcess execute(final RunRequest request) throws ApiException {
        checkStarted();
        final long startTime = System.currentTimeMillis();
        final RunnerConfiguration runnerCfg = getRunnerConfigurationFactory().createRunnerConfiguration(request);
        final Long internalId = processIdSequence.getAndIncrement();
        final RunnerProcessImpl process = new RunnerProcessImpl(internalId, getName(), runnerCfg, new RunnerProcess.Callback() {
            @Override
            public void started(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(new RunnerEvent(EventType.STARTED, getName(), runRequest.getId(), runRequest.getWorkspace(),
                                       runRequest.getProject()));
            }

            @Override
            public void stopped(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(new RunnerEvent(EventType.STOPPED, getName(), runRequest.getId(), runRequest.getWorkspace(),
                                       runRequest.getProject()));
            }

            @Override
            public void error(RunnerProcess process, Throwable t) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(new RunnerEvent(EventType.ERROR, getName(), runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject(),
                                       t.getMessage()));
            }

            private void notify(RunnerEvent re) {
                try {
                    eventService.publish(re);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
        processes.put(internalId, new RunnerProcessEntry(process, System.currentTimeMillis() + cleanupDelayMillis));
        final Watchdog watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", request.getLifetime(), TimeUnit.SECONDS);
        final int mem = runnerCfg.getMemory();
        final ResourceAllocator memoryAllocator = allocators.newMemoryAllocator(mem).allocate();
        executor.execute(ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                try {
                    final java.io.File downloadDir =
                            Files.createTempDirectory(deployDirectory.toPath(), ("download_" + getName() + '_')).toFile();
                    final DeploymentSources deploymentSources = downloadApplication(request.getDeploymentSourcesUrl(), downloadDir);
                    process.addToCleanupList(downloadDir);
                    if (!getDeploymentSourcesValidator().isValid(deploymentSources)) {
                        throw new ApiException(
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
                    LOG.debug("Application startup in {} ms", (endTime - startTime));
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
        }));
        return process;
    }

    private static final DeploymentSources NO_SOURCES = new DeploymentSources(null);

    private DeploymentSources downloadApplication(String url, java.io.File downloadDir) throws IOException {
        if (url == null) {
            return NO_SOURCES;
        }
        final ValueHolder<IOException> errorHolder = new ValueHolder<>();
        final ValueHolder<DeploymentSources> resultHolder = new ValueHolder<>();
        downloadPlugin.download(url, downloadDir, new DownloadPlugin.Callback() {
            @Override
            public void done(java.io.File downloaded) {
                resultHolder.set(new DeploymentSources(downloaded));
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

    public int getRunningAppsNum() {
        return runningAppsCounter.get();
    }

    public int getTotalAppsNum() {
        return processes.size();
    }

    private class RunnerProcessImpl implements RunnerProcess {
        private final Long                id;
        private final String              runner;
        private final RunnerConfiguration configuration;
        private final Callback            callback;

        private ApplicationProcess realProcess;
        private long               startTime;
        private long               stopTime;
        private Throwable          error;
        private List<java.io.File> forCleanup;

        RunnerProcessImpl(Long id, String runner, RunnerConfiguration configuration, Callback callback) {
            this.id = id;
            this.runner = runner;
            this.configuration = configuration;
            this.callback = callback;
            startTime = -1;
            stopTime = -1;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public synchronized long getStartTime() {
            return startTime;
        }

        @Override
        public synchronized long getStopTime() {
            return stopTime;
        }

        synchronized void started(ApplicationProcess realProcess) {
            this.realProcess = realProcess;
            startTime = System.currentTimeMillis();
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
        public synchronized boolean isRunning() throws ApiException {
            return realProcess != null && realProcess.isRunning();
        }

        @Override
        public synchronized boolean isStopped() throws ApiException {
            return realProcess != null && !realProcess.isRunning();
        }

        @Override
        public synchronized ApplicationLogger getLogger() throws ApiException {
            if (error != null) {
                throw new ApiException(error);
            }
            if (realProcess != null) {
                return realProcess.getLogger();
            }
            return ApplicationLogger.DUMMY;
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
            if (realProcess != null) {
                realProcess.stop();
            }
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

        @Override
        public String toString() {
            return "RunnerProcessImpl{" +
                   "\nworkspace='" + configuration.getRequest().getWorkspace() + '\'' +
                   "\nproject='" + configuration.getRequest().getProject() + '\'' +
                   "\nrunner='" + runner + '\'' +
                   "\nstartTime=" + startTime +
                   "\nstopTime=" + stopTime +
                   "\nid=" + id +
                   "\n}";
        }
    }

    private static class RunnerProcessEntry {
        final RunnerProcessImpl process;
        final long              expirationTime;

        RunnerProcessEntry(RunnerProcessImpl process, long expirationTime) {
            this.process = process;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }
    }
}
