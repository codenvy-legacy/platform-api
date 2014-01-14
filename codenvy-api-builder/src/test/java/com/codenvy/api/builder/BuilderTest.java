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

import com.codenvy.api.builder.internal.BuildListener;
import com.codenvy.api.builder.internal.BuildLogger;
import com.codenvy.api.builder.internal.BuildResult;
import com.codenvy.api.builder.internal.BuildTask;
import com.codenvy.api.builder.internal.Builder;
import com.codenvy.api.builder.internal.BuilderConfiguration;
import com.codenvy.api.builder.internal.BuilderException;
import com.codenvy.api.builder.internal.DelegateBuildLogger;
import com.codenvy.api.builder.internal.SourcesManager;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.core.util.CommandLine;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/** @author andrew00x */
public class BuilderTest {

    // Simple test for main Builder components. Don't run any real build processes.

    public static class MyBuilder extends Builder {
        MyDelegateBuildLogger logger;

        public MyBuilder(File root, int numberOfWorkers, int queueSize, int cleanBuildResultDelay) {
            super(root, numberOfWorkers, queueSize, cleanBuildResultDelay);
        }

        @Override
        public String getName() {
            return "my";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        protected BuildResult getTaskResult(FutureBuildTask task, boolean successful) {
            return new BuildResult(successful);
        }

        @Override
        protected CommandLine createCommandLine(BuilderConfiguration config) {
            return new CommandLine("echo", "test"); // display line of text
        }

        @Override
        protected BuildLogger createBuildLogger(BuilderConfiguration buildConfiguration, java.io.File logFile) throws BuilderException {
            return logger = new MyDelegateBuildLogger(super.createBuildLogger(buildConfiguration, logFile));
        }

        @Override
        public SourcesManager getSourcesManager() {
            return new SourcesManager() {

                @Override
                public void getSources(String workspace, String project, String sourcesUrl, File workDir) throws IOException {
                    // Don't need for current set of tests.
                }

                @Override
                public java.io.File getDirectory() {
                    return getSourcesDirectory();
                }
            };
        }
    }

    public static class MyDelegateBuildLogger extends DelegateBuildLogger {
        private StringBuilder buff = new StringBuilder();

        public MyDelegateBuildLogger(BuildLogger delegate) {
            super(delegate);
        }

        @Override
        public void writeLine(String line) throws IOException {
            if (line != null) {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append(line);
            }
            super.writeLine(line);
        }

        public String getLogsAsString() {
            return buff.toString();
        }
    }

    private java.io.File repo;
    private MyBuilder    builder;

    @BeforeTest
    public void setUp() throws Exception {
        repo = createRepository();
        builder = new MyBuilder(repo, Runtime.getRuntime().availableProcessors(), 100, 3600);
        builder.start();
    }

    public void tearDown() {
        builder.stop();
        Assert.assertTrue(IoUtil.deleteRecursive(repo), "Unable remove test directory");
    }

    static java.io.File createRepository() throws Exception {
        java.io.File root = new java.io.File(System.getProperty("workDir"), "repo");
        if (!(root.exists() || root.mkdirs())) {
            Assert.fail("Unable create test directory");
        }
        return root;
    }

    @Test
    public void testRunTask() throws Exception {
        final BuildRequest buildRequest = DtoFactory.getInstance().createDto(BuildRequest.class);
        buildRequest.setBuilder("my");
        buildRequest.setSourcesUrl("http://localhost/a" /* ok for test, nothing download*/);
        final BuildTask task = builder.perform(buildRequest);
        waitForTask(task);
        Assert.assertEquals(builder.logger.getLogsAsString(), "test");
    }

    @Test
    public void testBuildListener() throws Exception {
        final boolean[] beginFlag = new boolean[]{false};
        final boolean[] endFlag = new boolean[]{false};
        final BuildListener listener = new BuildListener() {
            @Override
            public void begin(BuildTask task) {
                beginFlag[0] = true;
            }

            @Override
            public void end(BuildTask task) {
                endFlag[0] = true;
            }
        };
        Assert.assertTrue(builder.addBuildListener(listener));
        final BuildRequest buildRequest = DtoFactory.getInstance().createDto(BuildRequest.class);
        buildRequest.setBuilder("my");
        buildRequest.setSourcesUrl("http://localhost/a" /* ok for test, nothing download*/);
        final BuildTask task = builder.perform(buildRequest);
        waitForTask(task);
        Assert.assertTrue(beginFlag[0]);
        Assert.assertTrue(endFlag[0]);
        Assert.assertTrue(builder.removeBuildListener(listener));
    }

    private void waitForTask(BuildTask task) throws Exception {
        final long end = System.currentTimeMillis() + 5000;
        synchronized (this) {
            while (!task.isDone()) {
                wait(100);
                if (System.currentTimeMillis() > end) {
                    Assert.fail("timeout");
                }
            }
        }
    }
}
