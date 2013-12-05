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

import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.core.util.DownloadPlugin;
import com.codenvy.api.core.util.Pair;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.lang.ZipUtils;
import com.google.common.hash.Hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Implementation of SourcesManager that stores sources locally and gets only updated files over virtual file system RESt API.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class SourcesManagerImpl implements DownloadPlugin, SourcesManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourcesManagerImpl.class);

    private final java.io.File                        directory;
    private final ConcurrentMap<String, Future<Void>> tasks;

    public SourcesManagerImpl(java.io.File directory) {
        this.directory = directory;
        tasks = new ConcurrentHashMap<>();
    }

    @Override
    public void getSources(BuilderConfiguration configuration) throws IOException {
        final BaseBuilderRequest request = configuration.getRequest();
        // Directory for sources. Keep sources to avoid download whole project before build.
        // This directory is not permanent and may be removed at any time.
        final String workspace = request.getWorkspace();
        final String project = request.getProject();
        final java.io.File srcDir = new java.io.File(directory, workspace + java.io.File.separatorChar + project);
        // Temporary directory where we copy sources before build.
        final java.io.File workDir = configuration.getWorkDir();
        final String key = workspace + project;
        // Avoid multiple threads download source of the same project.
        Future<Void> future = tasks.get(key);
        final IOException[] errorHolder = new IOException[1];
        if (future == null) {
            final FutureTask<Void> newFuture = new FutureTask<>(new Runnable() {
                @Override
                public void run() {
                    download(request.getSourcesUrl(), srcDir, new Callback() {
                        @Override
                        public void done(java.io.File downloaded) {
                            // Don't need this event. We are waiting until download is done.
                        }

                        @Override
                        public void error(IOException e) {
                            LOG.error(e.getMessage(), e);
                            errorHolder[0] = e;
                        }
                    });
                }
            }, null);
            future = tasks.putIfAbsent(key, newFuture);
            if (future == null) {
                future = newFuture;
                newFuture.run();
            }
        }
        try {
            future.get(); // Block thread until download is completed.
            if (errorHolder[0] != null) {
                throw errorHolder[0];
            }
            IoUtil.copy(srcDir, workDir, IoUtil.ANY_FILTER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Runnable does not throw checked exceptions.
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error)cause;
            } else {
                throw (RuntimeException)cause;
            }
        } finally {
            tasks.remove(key);
        }
    }

    @Override
    public void download(String downloadUrl, java.io.File downloadTo, Callback callback) {
        HttpURLConnection conn = null;
        try {
            final LinkedList<java.io.File> q = new LinkedList<>();
            q.add(downloadTo);
            final long start = System.currentTimeMillis();
            final List<Pair<String, String>> md5sums = new LinkedList<>();
            while (!q.isEmpty()) {
                java.io.File current = q.pop();
                java.io.File[] list = current.listFiles();
                if (list != null) {
                    for (java.io.File f : list) {
                        if (f.isDirectory()) {
                            q.push(f);
                        } else {
                            md5sums.add(Pair.of(com.google.common.io.Files.hash(f, Hashing.md5()).toString(),
                                                downloadTo.toPath().relativize(f.toPath()).toString()));
                        }
                    }
                }
            }
            final long end = System.currentTimeMillis();
            if (md5sums.size() > 0) {
                LOG.debug("count md5sums of {} files, time: {}ms", md5sums.size(), (end - start));
            }
            conn = (HttpURLConnection)new URL(downloadUrl).openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setConnectTimeout(30 * 1000);
            if (!md5sums.isEmpty()) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "text/plain");
                conn.setDoOutput(true);
                try (OutputStream output = conn.getOutputStream();
                     Writer writer = new OutputStreamWriter(output)) {
                    for (Pair<String, String> pair : md5sums) {
                        writer.write(pair.first);
                        writer.write(' ');
                        writer.write(pair.second);
                        writer.write('\n');
                    }
                }
            }
            final int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (InputStream in = conn.getInputStream()) {
                    ZipUtils.unzip(in, downloadTo);
                }
                final String removeHeader = conn.getHeaderField("x-removed-paths");
                if (removeHeader != null) {
                    for (String item : removeHeader.split(",")) {
                        java.io.File f = new java.io.File(downloadTo, item);
                        if (!f.delete()) {
                            if (f.exists()) {
                                throw new IOException(String.format("Can't delete %s", item));
                            }
                        }
                    }
                }
            } else if (responseCode != 204) {
                throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
            }
            callback.done(downloadTo);
        } catch (IOException e) {
            callback.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public java.io.File getDirectory() {
        return directory;
    }
}
