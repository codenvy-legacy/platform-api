/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.util.Cancellable;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.FileLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

/**
* @author Alexander Garagatyi
*/
class SyncTask implements Cancellable, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);
    private final String       watchPath;
    private final LineConsumer stdout;
    private final LineConsumer stderr;
    private       Process      process;

    public SyncTask(String watchPath, String workspace, String project) throws IOException {
        this.watchPath = watchPath;
        final String token = EnvironmentContext.getCurrent().getUser().getToken();
        final File stdoutFile = Files.createTempFile(watchPath.replace('/', '_'), "stdout").toFile();
        this.stdout = new CompositeLineConsumer(new FileLineConsumer(stdoutFile),
                                                new SyncEventProcessor(workspace, project, token, 3000));// TODO inject with guice
        this.stderr = new FileLineConsumer(Files.createTempFile(watchPath.replace('/', '_'), "stderr").toFile());
    }

    @Override
    public void run() {
        LOG.info("Sync task is starting");
        CommandLine cl = new CommandLine("inotifywait",
                                         "-mr",
                                         "--event",
                                         "modify,move,create,delete",//attrib,delete_self,move_self?
                                         "--format",
                                         "'%e %w%f'",
                                         watchPath);
        ProcessBuilder processBuilder = new ProcessBuilder().command(cl.toShellCommand());
        try {
            process = processBuilder.start();
            try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                LOG.info("Sync task read stdout");
                while ((line = inputReader.readLine()) != null) {
                    stdout.writeLine(line);
                }
                LOG.info("Sync task read stderr");
                while ((line = errorReader.readLine()) != null) {
                    stderr.writeLine(line);
                }
            } finally {
                try {
                    stdout.close();
                } finally {
                    stderr.close();
                }
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        LOG.info("Sync task is finishing");
    }

    @Override
    public void cancel() throws Exception {
        ProcessUtil.kill(process);
    }
}
