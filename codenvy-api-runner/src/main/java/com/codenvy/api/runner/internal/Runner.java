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
import com.codenvy.api.core.config.Configurable;
import com.codenvy.api.core.config.Configuration;
import com.codenvy.api.core.rest.DownloadPlugin;
import com.codenvy.api.core.rest.RemoteContent;
import com.codenvy.api.core.util.CustomPortService;
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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class Runner implements Configurable, Lifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    public static final  String DEPLOY_DIRECTORY = "runner.deploy_directory";
    private static final String MIN_PORT         = "runner.min_port";
    private static final String MAX_PORT         = "runner.max_port";

    public static interface Disposer {
        void dispose();
    }

    private volatile boolean       maySetConfiguration;
    private          Configuration configuration;

    private java.io.File deployDirectory;

    protected final CustomPortService         portService;
    private final   ExecutorService           executor;
    private final   Map<Long, ProcessWrapper> applications;
    private final   Map<Long, List<Disposer>> disposers;
    private final   Object                    disposersLock;

    public Runner() {
        applications = new ConcurrentHashMap<>();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory(getName(), true));
        portService = new CustomPortService();
        maySetConfiguration = true;
        disposers = new HashMap<>();
        disposersLock = new Object();
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess doExecute(DeploymentSources toDeploy, RunnerConfiguration configuration) throws RunnerException;

    @Override
    public Configuration getDefaultConfiguration() {
        final Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setFile(DEPLOY_DIRECTORY, new java.io.File(System.getProperty("java.io.tmpdir")));
        defaultConfiguration.setInt(MIN_PORT, 49152);
        defaultConfiguration.setInt(MAX_PORT, 65535);
        return defaultConfiguration;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        if (maySetConfiguration) {
            this.configuration = new Configuration(configuration);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Configuration getConfiguration() {
        return new Configuration(configuration);
    }

    @Override
    public void start() {
        maySetConfiguration = false;
        final Configuration myConfiguration = getConfiguration();
        LOG.debug("{}", myConfiguration);
        final java.io.File path = myConfiguration.getFile(DEPLOY_DIRECTORY, new java.io.File(System.getProperty("java.io.tmpdir")));
        deployDirectory = new java.io.File(path, getName());
        if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
            throw new LifecycleException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
        }
        portService.setRange(myConfiguration.getInt(MIN_PORT, 49152), myConfiguration.getInt(MAX_PORT, 65535));
    }

    @Override
    public void stop() {
        executor.shutdownNow();
        synchronized (disposersLock) {
            for (List<Disposer> disposerList : disposers.values()) {
                if (disposerList != null) {
                    for (Disposer disposer : disposerList) {
                        try {
                            disposer.dispose();
                        } catch (RuntimeException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
            disposers.clear();
        }
        applications.clear();
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
    }

    public java.io.File getDeployDirectory() {
        return deployDirectory;
    }

    public ApplicationProcess getApplicationProcess(Long id) throws RunnerException {
        final ApplicationProcess process = applications.get(id);
        if (process == null) {
            throw new NoSuchRunnerTaskException(id);
        }
        return process;
    }

    public ApplicationProcess execute(final RunRequest request) throws IOException, RunnerException {
        final java.io.File downloadDir = Files.createTempDirectory(deployDirectory.toPath(), ("download_" + getName() + '_')).toFile();
        final RunnerConfiguration runnerConfiguration = getRunnerConfigurationFactory().createRunnerConfiguration(request);
        final ProcessWrapper wrapper = new ProcessWrapper(getName(), runnerConfiguration, request.getLifetime());
        purgeExpiredProcesses();
        applications.put(wrapper.getId(), wrapper);
        final RemoteContent remoteContent = RemoteContent.of(downloadDir, request.getDeploymentSourcesUrl());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                remoteContent.download(new DownloadPlugin.Callback() {
                    @Override
                    public void done(java.io.File downloaded) {
                        try {
                            wrapper.process = doExecute(new DeploymentSources(downloaded), runnerConfiguration);
                        } catch (Exception e) {
                            wrapper.setError(e);
                        }
                    }

                    @Override
                    public void error(IOException e) {
                        wrapper.setError(e);
                    }
                });
            }
        });
        return wrapper;
    }

    protected void registerDisposer(ApplicationProcess process, Disposer disposer) {
        final Long id = process.getId();
        synchronized (disposersLock) {
            List<Disposer> disposersList = disposers.get(id);
            if (disposersList == null) {
                disposers.put(id, disposersList = new ArrayList<>(1));
            }
            disposersList.add(disposer);
        }
    }

    private void purgeExpiredProcesses() {
        for (Iterator<ProcessWrapper> i = applications.values().iterator(); i.hasNext(); ) {
            final ProcessWrapper wrapper = i.next();
            if (wrapper.isExpired()) {
                try {
                    if (wrapper.isRunning()) {
                        wrapper.stop();
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    continue; // try next time
                }
                i.remove();
                final ApplicationProcess process = wrapper.process;
                if (process != null) {
                    // If null then real process wasn't started.
                    synchronized (disposersLock) {
                        final List<Disposer> disposerList = disposers.remove(process.getId());
                        if (disposerList != null) {
                            for (Disposer disposer : disposerList) {
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
        }
    }

    private static class ProcessWrapper extends ApplicationProcess {
        volatile ApplicationProcess process;
        volatile Throwable          error;

        final long expirationTime;

        ProcessWrapper(String runner, RunnerConfiguration configuration, long lifetime) {
            super(runner, configuration);
            expirationTime = System.currentTimeMillis() + lifetime;
        }

        boolean isExpired() {
            return expirationTime < System.currentTimeMillis();
        }

        @Override
        public long getStartTime() throws RunnerException {
            final ApplicationProcess process = this.process;
            return process == null ? -1 : process.getStartTime();
        }

        @Override
        public boolean isRunning() throws RunnerException {
            final ApplicationProcess process = this.process;
            return process != null && process.isRunning();
        }

        @Override
        public boolean isDone() throws RunnerException {
            final ApplicationProcess process = this.process;
            return process != null && process.isDone();
        }

        @Override
        public void stop() throws RunnerException {
            final ApplicationProcess process = this.process;
            if (process == null) {
                return;
            }
            process.stop();
        }

        @Override
        public ApplicationLogger getLogger() {
            final ApplicationProcess process = this.process;
            return process == null ? ApplicationLogger.DUMMY : process.getLogger();
        }

        @Override
        public RunnerConfiguration getConfiguration() {
            final ApplicationProcess process = this.process;
            return process == null ? null : process.getConfiguration();
        }

        @Override
        public Throwable getError() {
            final Throwable error = this.error;
            if (error != null) {
                return error;
            }
            final ApplicationProcess process = this.process;
            if (process == null) {
                return null;
            }
            return process.getError();
        }

        @Override
        public void setError(Throwable error) {
            this.error = error;
        }
    }
}
