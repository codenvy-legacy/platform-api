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
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
* @author Alexander Garagatyi
*/
class SyncTask implements Cancellable, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);
    private final String       watchPath;
    private final LineConsumer output;
    private       Process      process;

    public SyncTask(String watchPath, String workspace, String project, @Named("api.endpoint") String apiEndpoint) throws IOException {
        this.watchPath = watchPath;
        final String token = EnvironmentContext.getCurrent().getUser().getToken();
        // TODO inject delay, apiEndpoint with guice. @Assisted can be used to inject
        this.output = new SyncEventProcessor(watchPath, workspace, project, token, 1000, apiEndpoint);
    }

    @Override
    public void run() {
        LOG.info("Sync task is starting");
        CommandLine cl = new CommandLine("inotifywait",
                                         "-mr",
                                         "--quiet",
                                         "--event",
                                         "modify,move,move_self,create,delete",//attrib,delete_self?
                                         "--format",
                                         "'%e %w%f'",
                                         watchPath);
        ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true).command(cl.toShellCommand());
        try {
            process = processBuilder.start();
            try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = inputReader.readLine()) != null) {
                    output.writeLine(line);
                }
            } finally {
                output.close();
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
