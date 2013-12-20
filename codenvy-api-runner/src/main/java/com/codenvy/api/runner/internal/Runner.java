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

import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.util.DownloadPlugin;
import com.codenvy.api.core.util.HttpDownloadPlugin;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.api.runner.NoSuchRunnerTaskException;
import com.codenvy.api.runner.RunnerException;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.inject.ConfigurationParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
public abstract class Runner {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    public static final String DEPLOY_DIRECTORY   = "runner.internal.deploy_directory";
    public static final String CLEANUP_DELAY_TIME = "runner.internal.clean_delay_time";

    private static final AtomicLong processIdSequence = new AtomicLong(1);

    private final ExecutorService               executor;
    private final Map<Long, RunnerProcessEntry> processes;
    private final Map<Long, List<Disposer>>     applicationDisposers;
    private final Object                        applicationDisposersLock;
    private final AtomicInteger                 runningAppsCounter;
    private final java.io.File                  deployDirectoryRoot;
    private final DownloadPlugin                downloadPlugin;
    private final ResourceAllocators            allocators;
    private final int                           cleanupDelay;


    private java.io.File deployDirectory;
    private boolean      started;

    public Runner(java.io.File deployDirectoryRoot, int cleanupDelay, ResourceAllocators allocators) {
        this.deployDirectoryRoot = deployDirectoryRoot;
        this.cleanupDelay = cleanupDelay;
        this.allocators = allocators;
        processes = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory(getName().toUpperCase(), true));
        applicationDisposers = new HashMap<>();
        applicationDisposersLock = new Object();
        runningAppsCounter = new AtomicInteger(0);
        downloadPlugin = new HttpDownloadPlugin();
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess newApplicationProcess(DeploymentSources toDeploy,
                                                                RunnerConfiguration runnerCfg) throws RunnerException;

    protected ExecutorService getExecutor() {
        return executor;
    }

    @PostConstruct
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        deployDirectory = new java.io.File(deployDirectoryRoot, getName());
        if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
            throw new IllegalStateException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
        }
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalArgumentException("Lifecycle instance is not started yet.");
        }
    }

    @PreDestroy
    public synchronized void stop() {
        checkStarted();
        executor.shutdownNow();
        List<Disposer> allDisposers = new ArrayList<>();
        synchronized (applicationDisposersLock) {
            for (List<Disposer> disposerList : applicationDisposers.values()) {
                if (disposerList != null) {
                    for (Disposer disposer : disposerList) {
                        allDisposers.add(disposer);
                    }
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
        // cleanup deploy directory
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
    }

    public java.io.File getDeployDirectory() {
        return deployDirectory;
    }

    public RunnerProcess getProcess(Long id) throws RunnerException {
        checkStarted();
        final RunnerProcessEntry wrapper = processes.get(id);
        if (wrapper == null) {
            throw new NoSuchRunnerTaskException(id);
        }
        return wrapper.process;
    }

    public RunnerProcess execute(final RunRequest request) throws IOException, RunnerException {
        checkStarted();
        // TODO: cleanup
        final java.io.File downloadDir = Files.createTempDirectory(deployDirectory.toPath(), ("download_" + getName() + '_')).toFile();
        final RunnerConfiguration runnerCfg = getRunnerConfigurationFactory().createRunnerConfiguration(request);
        final Long id = processIdSequence.getAndIncrement();
        final String webHookUrl = request.getWebHookUrl();
        final RunnerProcessImpl process = new RunnerProcessImpl(id,
                                                                getName(),
                                                                runnerCfg,
                                                                webHookUrl == null ? null : new WebHookCallback(webHookUrl));
        purgeExpiredProcesses();
        processes.put(id, new RunnerProcessEntry(process, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cleanupDelay)));
        final Watchdog watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", request.getLifetime(), TimeUnit.SECONDS);
        final int mem = runnerCfg.getMemory();
        final ResourceAllocator memoryAllocator = allocators.newMemoryAllocator(mem)
                                                            .allocate();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final ApplicationProcess realProcess =
                            newApplicationProcess(downloadApplication(downloadDir, request.getDeploymentSourcesUrl()), runnerCfg);
                    realProcess.start();
                    process.started(realProcess);
                    watcher.start(process);
                    runningAppsCounter.incrementAndGet();
                    LOG.debug("Started {}", process);
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
        return process;
    }

    protected DeploymentSources downloadApplication(java.io.File downloadTo, String url) throws RunnerException {
        final IOException[] errorHolder = new IOException[1];
        final DeploymentSources[] resultHolder = new DeploymentSources[1];
        downloadPlugin.download(url, downloadTo, new DownloadPlugin.Callback() {
            @Override
            public void done(java.io.File downloaded) {
                resultHolder[0] = new DeploymentSources(downloaded);
            }

            @Override
            public void error(IOException e) {
                LOG.error(e.getMessage(), e);
                errorHolder[0] = e;
            }
        });
        if (errorHolder[0] != null) {
            throw new RunnerException(errorHolder[0]);
        }
        return resultHolder[0];
    }

    protected void registerDisposer(ApplicationProcess application, Disposer disposer) {
        final Long id = application.getId();
        synchronized (applicationDisposersLock) {
            List<Disposer> disposersList = applicationDisposers.get(id);
            if (disposersList == null) {
                applicationDisposers.put(id, disposersList = new ArrayList<>(1));
            }
            disposersList.add(disposer);
        }
    }

    private void purgeExpiredProcesses() {
        for (Iterator<RunnerProcessEntry> i = processes.values().iterator(); i.hasNext(); ) {
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
                Disposer[] disposers = null;
                final ApplicationProcess realProcess = next.process.realProcess;
                if (realProcess != null) {
                    synchronized (applicationDisposersLock) {
                        final List<Disposer> disposerList = applicationDisposers.remove(realProcess.getId());
                        if (disposerList != null) {
                            disposers = disposerList.toArray(new Disposer[disposerList.size()]);
                        }
                    }
                }
                if (disposers != null) {
                    for (Disposer disposer : disposers) {
                        try {
                            disposer.dispose();
                        } catch (RuntimeException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
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
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.started(RunnerProcessImpl.this);
                    }
                });
            }
        }

        synchronized void stopped() {
            stopTime = System.currentTimeMillis();
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.stopped(RunnerProcessImpl.this);
                    }
                });
            }
        }

        @Override
        public synchronized boolean isRunning() throws RunnerException {
            return realProcess != null && realProcess.isRunning();
        }

        @Override
        public synchronized boolean isStopped() throws RunnerException {
            return realProcess != null && !realProcess.isRunning();
        }

        @Override
        public synchronized ApplicationLogger getLogger() throws RunnerException {
            if (error != null) {
                throw new RunnerException(error);
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
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.error(RunnerProcessImpl.this, error);
                    }
                });
            }
        }

        @Override
        public synchronized void cancel() throws Exception {
            if (realProcess != null) {
                realProcess.stop();
            }
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

    private static class WebHookCallback implements RunnerProcess.Callback {
        final String url;

        WebHookCallback(String url) {
            this.url = url;
        }

        @Override
        public void started(RunnerProcess process) {
            try {
                HttpJsonHelper.post(null, url, null, Pair.of("event", "started"));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void stopped(RunnerProcess process) {
            try {
                HttpJsonHelper.post(null, url, null, Pair.of("event", "stopped"));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void error(RunnerProcess process, Throwable t) {
            try {
                HttpJsonHelper.post(null, url, null, Pair.of("event", "error"));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
