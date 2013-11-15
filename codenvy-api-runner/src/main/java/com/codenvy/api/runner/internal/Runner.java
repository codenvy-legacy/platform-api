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

import com.codenvy.api.core.Lifecycle;
import com.codenvy.api.core.LifecycleException;
import com.codenvy.api.core.config.Configuration;
import com.codenvy.api.core.config.SingletonConfiguration;
import com.codenvy.api.core.rest.DownloadPlugin;
import com.codenvy.api.core.rest.RemoteContent;
import com.codenvy.api.core.util.CustomPortService;
import com.codenvy.api.core.util.Watchdog;
import com.codenvy.api.runner.NoSuchRunnerTaskException;
import com.codenvy.api.runner.RunnerException;
import com.codenvy.api.runner.internal.dto.RunRequest;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicLong;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class Runner implements Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    public static final  String DEPLOY_DIRECTORY   = "runner.deploy_directory";
    private static final String MIN_PORT           = "runner.min_port";
    private static final String MAX_PORT           = "runner.max_port";
    private static final String CLEANUP_DELAY_TIME = "runner.clean_delay_time";

    private static final AtomicLong processIdSequence = new AtomicLong(1);

    /** Default memory size for application in megabytes. */
    public static final int DEFAULT_MEMORY_SIZE = 128; // TODO

    private java.io.File deployDirectory;

    protected final CustomPortService portService;

    private final ExecutorService                executor;
    private final Map<Long, CachedRunnerProcess> processes;
    private final Map<Long, List<Disposer>>      applicationDisposers;
    private final Object                         applicationDisposersLock;

    private int     cleanupDelay;
    private boolean started;

    public Runner() {
        processes = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory(getName().toUpperCase(), true));
        portService = new CustomPortService();
        applicationDisposers = new HashMap<>();
        applicationDisposersLock = new Object();
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess newApplicationProcess(DeploymentSources toDeploy,
                                                                RunnerConfiguration runnerCfg,
                                                                ApplicationProcess.Callback callback) throws RunnerException;

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
        final java.io.File path = myConfiguration.getFile(DEPLOY_DIRECTORY, new java.io.File(System.getProperty("java.io.tmpdir")));
        deployDirectory = new java.io.File(path, getName());
        if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
        }
        portService.setRange(myConfiguration.getInt(MIN_PORT, 49152), myConfiguration.getInt(MAX_PORT, 65535));
        cleanupDelay = myConfiguration.getInt(CLEANUP_DELAY_TIME, 15);
        started = true;
    }

    protected synchronized void checkStarted() {
        if (!started) {
            throw new IllegalArgumentException("Lifecycle instance is not started yet.");
        }
    }

    @Override
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
        final CachedRunnerProcess wrapper = processes.get(id);
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
        final RemoteContent remoteContent = RemoteContent.of(downloadDir, request.getDeploymentSourcesUrl());
        final Long id = processIdSequence.getAndIncrement();
        final RunnerProcessImpl process = new RunnerProcessImpl(id, getName(), runnerCfg);
        purgeExpiredProcesses();
        processes.put(id, new CachedRunnerProcess(process, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cleanupDelay)));
        final Watchdog watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", request.getLifetime(), TimeUnit.SECONDS);
        final int mem = runnerCfg.getMemory();
        final ResourceAllocators.ResourceAllocator memoryAllocator = ResourceAllocators.getInstance()
                                                                                       .newMemoryAllocator(mem)
                                                                                       .allocate();
        final ApplicationProcess.Callback callback = new ApplicationProcess.Callback() {
            @Override
            public void started(ApplicationProcess realProcess) {
                process.started(realProcess);
                watcher.start(process);
                // TODO: debug
                LOG.info("Started {}", process);
            }

            @Override
            public void stopped(ApplicationProcess realProcess) {
                watcher.stop();
                memoryAllocator.release();
                process.stopped();
                // TODO: debug
                LOG.info("Stopped {}", process);
            }

            @Override
            public void startError(Throwable error) {
                LOG.error(String.format("Failed start %s", process), error);
                watcher.stop();
                memoryAllocator.release();
                process.setError(error);
            }

            @Override
            public void stopError(Throwable error) {
                LOG.error(String.format("Stop error %s", process), error);
                process.setError(error);
            }
        };
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    newApplicationProcess(downloadApplication(remoteContent), runnerCfg, callback).start();
                } catch (Throwable e) {
                    callback.startError(e);
                }
            }
        });
        return process;
    }

    protected DeploymentSources downloadApplication(RemoteContent sources) throws RunnerException {
        final IOException[] errorHolder = new IOException[1];
        final DeploymentSources[] resultHolder = new DeploymentSources[1];
        sources.download(new DownloadPlugin.Callback() {
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
        for (Iterator<CachedRunnerProcess> i = processes.values().iterator(); i.hasNext(); ) {
            final CachedRunnerProcess next = i.next();
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

    private static class RunnerProcessImpl implements RunnerProcess {
        private final Long                id;
        private final String              runner;
        private final RunnerConfiguration configuration;

        private ApplicationProcess realProcess;
        private long               startTime;
        private long               stopTime;
        private Throwable          error;

        RunnerProcessImpl(Long id, String runner, RunnerConfiguration configuration) {
            this.id = id;
            this.runner = runner;
            this.configuration = configuration;
            this.startTime = -1;
            this.stopTime = -1;
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
        }

        synchronized void stopped() {
            stopTime = System.currentTimeMillis();
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

        synchronized void setError(Throwable error) {
            this.error = error;
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

    private static class CachedRunnerProcess {
        final RunnerProcessImpl process;
        final long              expirationTime;

        CachedRunnerProcess(RunnerProcessImpl process, long expirationTime) {
            this.process = process;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }
    }
}
